package com.adoptu.adapters.image

import com.adoptu.domains.image.ImageStoragePort
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.util.*

class S3ImageStorageAdapter(
    private val bucketName: String,
    private val region: String,
    private val accessKeyId: String?,
    private val secretAccessKey: String?,
    private val endpoint: String?,
    private val pathStyleAccess: Boolean = false
) : ImageStoragePort {

    private val s3Client: S3Client by lazy {
        val builder = S3Client.builder()
            .region(Region.of(region))

        if (!accessKeyId.isNullOrEmpty() && !secretAccessKey.isNullOrEmpty()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
            )
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create())
        }

        if (!endpoint.isNullOrEmpty()) {
            builder.endpointOverride(java.net.URI.create(endpoint))
        }

        builder.forcePathStyle(pathStyleAccess).build()
    }

    override suspend fun uploadImage(petId: Int, imageName: String, contentType: String, inputStream: InputStream): String {
        val key = "pets/$petId/${UUID.randomUUID()}-$imageName"

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, -1))

        return getImageUrl(petId, key)
    }

    override suspend fun deleteImage(imageUrl: String): Boolean {
        return try {
            val key = extractKeyFromUrl(imageUrl)
            val request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()
            s3Client.deleteObject(request)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getImageUrl(petId: Int, imageKey: String): String {
        return if (!endpoint.isNullOrEmpty()) {
            "$endpoint/$bucketName/$imageKey"
        } else {
            "https://$bucketName.s3.$region.amazonaws.com/$imageKey"
        }
    }

    private fun extractKeyFromUrl(url: String): String {
        return url.substringAfter("$bucketName/")
    }
}
