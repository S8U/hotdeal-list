package com.github.s8u.hotdeallist.config

import com.github.s8u.hotdeallist.store.FileStore
import com.github.s8u.hotdeallist.store.LocalFileStore
import com.github.s8u.hotdeallist.store.S3FileStore
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths

@ConfigurationProperties(prefix = "file-store")
data class FileStoreProperties(
    val type: String = "local",
    val local: LocalProperties = LocalProperties(),
    val s3: S3Properties = S3Properties()
) {
    data class LocalProperties(
        val basePath: String = "./storage",
        val baseUrl: String = "http://localhost:8080/storage"
    )

    data class S3Properties(
        val bucket: String = "",
        val baseUrl: String = "",
        val endpoint: String = "",
        val accessKey: String = "",
        val secretKey: String = "",
        val region: String = "auto"
    )
}

@Configuration
class FileStoreConfig(
    private val properties: FileStoreProperties
) {

    @Bean
    fun fileStore(): FileStore {
        return when (properties.type.lowercase()) {
            "s3" -> S3FileStore(
                bucket = properties.s3.bucket,
                baseUrl = properties.s3.baseUrl,
                endpoint = properties.s3.endpoint,
                accessKey = properties.s3.accessKey,
                secretKey = properties.s3.secretKey,
                region = properties.s3.region
            )
            else -> LocalFileStore(
                basePath = Paths.get(properties.local.basePath),
                baseUrl = properties.local.baseUrl
            )
        }
    }
}
