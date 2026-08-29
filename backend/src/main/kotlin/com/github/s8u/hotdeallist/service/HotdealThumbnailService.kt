package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.config.CrawlerProperties
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.store.FileStore
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI

@Service
class HotdealThumbnailService(
    private val fileStore: FileStore,
    private val crawlerProperties: CrawlerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val ruliwebProxy: Proxy? = crawlerProperties.ruliweb.socks.toProxy()

    fun downloadAndStore(
        platformType: PlatformType,
        platformPostId: String,
        thumbnailUrl: String?,
        fallbackUrl: String?
    ): String? {
        val imageUrl = thumbnailUrl ?: fallbackUrl ?: return null

        return try {
            val proxy = findProxy(imageUrl)
            val imageBytes = downloadImage(imageUrl, proxy) ?: return null
            val webpBytes = convertToWebp(imageBytes) ?: return null

            val path = "${platformType.name}/$platformPostId.webp"
            fileStore.store(path, ByteArrayInputStream(webpBytes), "image/webp")

            logger.debug("Thumbnail stored: {}", path)
            path
        } catch (e: Exception) {
            logger.warn("Failed to download/store thumbnail: {} - {}", imageUrl, e.message)
            null
        }
    }

    fun getThumbnailUrl(thumbnailPath: String?): String? {
        return thumbnailPath?.let { fileStore.getUrl(it) }
    }

    private fun findProxy(url: String): Proxy? {
        if (ruliwebProxy == null) return null

        val host = runCatching { URI.create(url).host }.getOrNull() ?: return null

        return if (host == "ruliweb.com" || host.endsWith(".ruliweb.com")) ruliwebProxy else null
    }

    private fun downloadImage(url: String, proxy: Proxy?): ByteArray? {
        return try {
            val target = URI.create(url).toURL()
            val connection = (proxy?.let(target::openConnection) ?: target.openConnection()) as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; HotdealBot/1.0)")

            try {
                if (connection.responseCode == 200) {
                    connection.inputStream.use { it.readBytes() }
                } else {
                    logger.debug("Failed to download image: {} - status {}", url, connection.responseCode)
                    null
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            logger.debug("Failed to download image: {} - {}", url, e.message)
            null
        }
    }

    private fun convertToWebp(imageBytes: ByteArray): ByteArray? {
        return try {
            val image = ImmutableImage.loader().fromBytes(imageBytes)

            val minSide = minOf(image.width, image.height)
            val resized = if (minSide > MIN_SIDE) {
                val scale = MIN_SIDE.toDouble() / minSide
                image.scale(scale)
            } else {
                image
            }

            resized.bytes(WebpWriter.DEFAULT.withQ(WEBP_QUALITY))
        } catch (e: Exception) {
            logger.debug("Failed to convert image to WebP: {}", e.message)
            null
        }
    }

    companion object {
        private const val MIN_SIDE = 300
        private const val WEBP_QUALITY = 80
    }
}
