package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.store.FileStore
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class HotdealThumbnailService(
    private val fileStore: FileStore
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun downloadAndStore(
        platformType: PlatformType,
        platformPostId: String,
        thumbnailUrl: String?,
        fallbackUrl: String?
    ): String? {
        val imageUrl = thumbnailUrl ?: fallbackUrl ?: return null

        return try {
            val imageBytes = downloadImage(imageUrl) ?: return null
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

    private fun downloadImage(url: String): ByteArray? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0 (compatible; HotdealBot/1.0)")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

            if (response.statusCode() == 200) {
                response.body()
            } else {
                logger.debug("Failed to download image: {} - status {}", url, response.statusCode())
                null
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
