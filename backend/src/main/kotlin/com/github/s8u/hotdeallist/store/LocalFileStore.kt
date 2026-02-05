package com.github.s8u.hotdeallist.store

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalFileStore(
    private val basePath: Path,
    private val baseUrl: String
) : FileStore {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath)
            logger.info("Created local storage directory: {}", basePath)
        }
    }

    override fun store(path: String, inputStream: InputStream, contentType: String): String {
        val filePath = basePath.resolve(path)
        Files.createDirectories(filePath.parent)
        inputStream.use { input ->
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING)
        }
        logger.debug("Stored file: {}", filePath)
        return path
    }

    override fun get(path: String): InputStream? {
        val filePath = basePath.resolve(path)
        return if (Files.exists(filePath)) {
            Files.newInputStream(filePath)
        } else {
            null
        }
    }

    override fun delete(path: String): Boolean {
        val filePath = basePath.resolve(path)
        return try {
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            logger.warn("Failed to delete file: {}", filePath, e)
            false
        }
    }

    override fun exists(path: String): Boolean {
        return Files.exists(basePath.resolve(path))
    }

    override fun getUrl(path: String): String {
        return "$baseUrl/$path"
    }
}
