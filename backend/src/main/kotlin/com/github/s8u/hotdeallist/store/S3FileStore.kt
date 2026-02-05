package com.github.s8u.hotdeallist.store

import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.net.URI

class S3FileStore(
    private val bucket: String,
    private val baseUrl: String,
    endpoint: String,
    accessKey: String,
    secretKey: String,
    region: String = "auto"
) : FileStore {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val s3Client: S3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            )
        )
        .region(Region.of(region))
        .forcePathStyle(true)
        .build()

    override fun store(path: String, inputStream: InputStream, contentType: String): String {
        val bytes = inputStream.use { it.readBytes() }

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(path)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(bytes))
        logger.debug("Stored file to S3: {}/{}", bucket, path)
        return path
    }

    override fun get(path: String): InputStream? {
        return try {
            val request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build()
            s3Client.getObject(request)
        } catch (e: NoSuchKeyException) {
            null
        } catch (e: Exception) {
            logger.warn("Failed to get file from S3: {}/{}", bucket, path, e)
            null
        }
    }

    override fun delete(path: String): Boolean {
        return try {
            val request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build()
            s3Client.deleteObject(request)
            true
        } catch (e: Exception) {
            logger.warn("Failed to delete file from S3: {}/{}", bucket, path, e)
            false
        }
    }

    override fun exists(path: String): Boolean {
        return try {
            val request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build()
            s3Client.headObject(request)
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: Exception) {
            logger.warn("Failed to check file existence in S3: {}/{}", bucket, path, e)
            false
        }
    }

    override fun getUrl(path: String): String {
        return "$baseUrl/$path"
    }
}
