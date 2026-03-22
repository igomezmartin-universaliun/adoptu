package com.adoptu.adapters.storage

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageStorageIT {

    private var localstackContainer: LocalStackContainer? = null
    private var testBucketName: String = "test-bucket"

    @BeforeAll
    fun startLocalStack() {
        localstackContainer = LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.0.1")
        ).withServices(LocalStackContainer.Service.S3)

        localstackContainer!!.start()

        val endpointOverride = localstackContainer!!.getEndpointOverride(LocalStackContainer.Service.S3)
        val region = localstackContainer!!.region

        val s3Client = S3Client.builder()
            .endpointOverride(endpointOverride)
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
            )
            .build()

        s3Client.createBucket(CreateBucketRequest.builder().bucket(testBucketName).build())
        s3Client.close()
    }

    @AfterAll
    fun stopLocalStack() {
        localstackContainer?.stop()
    }

    private fun createAdapter(): S3ImageStorageAdapter {
        val ls = localstackContainer!!
        return S3ImageStorageAdapter(
            bucketName = testBucketName,
            region = ls.region,
            accessKeyId = "test",
            secretAccessKey = "test",
            endpoint = ls.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            pathStyleAccess = true
        )
    }

    @Test
    fun `uploadImage returns URL when successfully uploaded`() = runBlocking {
        val adapter = createAdapter()
        val imageBytes = "fake image content".toByteArray()
        val inputStream = ByteArrayInputStream(imageBytes)

        val result = adapter.uploadImage(1, "test.jpg", "image/jpeg", inputStream)

        assertNotNull(result)
        assertTrue(result.contains(testBucketName))
        assertTrue(result.contains("pets/1/"))
    }

    @Test
    fun `uploadImage generates unique keys for each upload`() = runBlocking {
        val adapter = createAdapter()

        val inputStream1 = ByteArrayInputStream("image1".toByteArray())
        val inputStream2 = ByteArrayInputStream("image2".toByteArray())

        val result1 = adapter.uploadImage(1, "test.jpg", "image/jpeg", inputStream1)
        val result2 = adapter.uploadImage(1, "test.jpg", "image/jpeg", inputStream2)

        assertTrue(result1 != result2, "Each upload should generate unique key")
    }

    @Test
    fun `uploadImage preserves pet id in path`() = runBlocking {
        val adapter = createAdapter()
        val inputStream = ByteArrayInputStream("test".toByteArray())

        val result = adapter.uploadImage(42, "photo.png", "image/png", inputStream)

        assertTrue(result.contains("pets/42/"))
        assertTrue(result.contains("photo.png"))
    }

    @Test
    fun `uploadImage handles different content types`() = runBlocking {
        val adapter = createAdapter()
        val contentTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp")

        contentTypes.forEach { contentType ->
            val extension = contentType.substringAfter("/")
            val inputStream = ByteArrayInputStream("test".toByteArray())

            val result = adapter.uploadImage(1, "image.$extension", contentType, inputStream)

            assertTrue(result.contains("image.$extension"))
        }
    }

    @Test
    fun `uploadImage includes original filename in key`() = runBlocking {
        val adapter = createAdapter()
        val originalFilename = "my-pet-photo-2024.jpg"
        val inputStream = ByteArrayInputStream("test".toByteArray())

        val result = adapter.uploadImage(1, originalFilename, "image/jpeg", inputStream)

        assertTrue(result.contains(originalFilename))
    }

    @Test
    fun `uploadImage handles large content`() = runBlocking {
        val adapter = createAdapter()
        val largeContent = "A".repeat(10000).toByteArray()
        val inputStream = ByteArrayInputStream(largeContent)

        val result = adapter.uploadImage(1, "large.jpg", "image/jpeg", inputStream)

        assertNotNull(result)
        assertTrue(result.contains(testBucketName))
    }

    @Test
    fun `getImageUrl returns custom endpoint URL`() {
        val adapter = createAdapter()

        val url = adapter.getImageUrl(1, "pets/1/test.jpg")

        assertTrue(url.startsWith(localstackContainer!!.getEndpointOverride(LocalStackContainer.Service.S3).toString()))
        assertTrue(url.contains(testBucketName))
        assertTrue(url.contains("pets/1/test.jpg"))
    }

    @Test
    fun `getImageUrl generates correct path for different images`() {
        val adapter = createAdapter()

        val url1 = adapter.getImageUrl(1, "pets/1/image1.jpg")
        val url2 = adapter.getImageUrl(1, "pets/1/image2.jpg")
        val url3 = adapter.getImageUrl(2, "pets/2/image1.jpg")

        assertTrue(url1.contains("image1.jpg"))
        assertTrue(url2.contains("image2.jpg"))
        assertTrue(url3.contains("pets/2/"))
    }

    @Test
    fun `uploadImage and deleteImage work together`() = runBlocking {
        val adapter = createAdapter()
        val inputStream = ByteArrayInputStream("test-image".toByteArray())

        val uploadResult = adapter.uploadImage(99, "to-delete.jpg", "image/jpeg", inputStream)
        assertTrue(uploadResult.contains(testBucketName))

        val deleteResult = adapter.deleteImage(uploadResult)
        assertTrue(deleteResult)
    }

    @Test
    fun `deleteImage returns true when successful`() = runBlocking {
        val adapter = createAdapter()
        val inputStream = ByteArrayInputStream("image-to-delete".toByteArray())

        val uploadUrl = adapter.uploadImage(5, "delete-me.jpg", "image/jpeg", inputStream)
        val deleteResult = adapter.deleteImage(uploadUrl)

        assertTrue(deleteResult)
    }

    @Test
    fun `multiple uploads to same pet generate unique keys`() = runBlocking {
        val adapter = createAdapter()

        val urls = (1..5).map { i ->
            val inputStream = ByteArrayInputStream("image-$i".toByteArray())
            adapter.uploadImage(10, "photo.jpg", "image/jpeg", inputStream)
        }

        val uniqueUrls = urls.toSet()
        assertEquals(5, uniqueUrls.size, "All URLs should be unique")
    }

    @Test
    fun `adapter can be created with custom endpoint for LocalStack`() {
        val ls = localstackContainer!!
        val customAdapter = S3ImageStorageAdapter(
            bucketName = testBucketName,
            region = ls.region,
            accessKeyId = "test",
            secretAccessKey = "test",
            endpoint = ls.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            pathStyleAccess = true
        )

        assertNotNull(customAdapter)
        val url = customAdapter.getImageUrl(1, "test.jpg")
        assertTrue(url.contains(testBucketName))
    }

    @Test
    fun `uploadImage creates bucket automatically when bucket does not exist`() = runBlocking {
        val ls = localstackContainer!!
        val newBucketName = "autocreated-bucket"

        val adapterForNewBucket = S3ImageStorageAdapter(
            bucketName = newBucketName,
            region = ls.region,
            accessKeyId = "test",
            secretAccessKey = "test",
            endpoint = ls.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            pathStyleAccess = true
        )

        val inputStream = ByteArrayInputStream("test image content".toByteArray())

        val result = adapterForNewBucket.uploadImage(1, "test.jpg", "image/jpeg", inputStream)

        assertNotNull(result)
        assertTrue(result.contains(newBucketName))
        assertTrue(result.contains("pets/1/"))
    }

    @Test
    fun `uploadImage succeeds after bucket is auto-created on first upload`() = runBlocking {
        val ls = localstackContainer!!
        val retryBucketName = "retry-bucket"

        val adapterForRetry = S3ImageStorageAdapter(
            bucketName = retryBucketName,
            region = ls.region,
            accessKeyId = "test",
            secretAccessKey = "test",
            endpoint = ls.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            pathStyleAccess = true
        )

        val inputStream1 = ByteArrayInputStream("first upload".toByteArray())
        val inputStream2 = ByteArrayInputStream("second upload".toByteArray())

        val result1 = adapterForRetry.uploadImage(5, "first.jpg", "image/jpeg", inputStream1)
        assertTrue(result1.contains(retryBucketName))
        assertTrue(result1.contains("pets/5/"))

        val result2 = adapterForRetry.uploadImage(5, "second.jpg", "image/jpeg", inputStream2)
        assertTrue(result2.contains(retryBucketName))
        assertTrue(result2.contains("pets/5/"))

        assertTrue(result1 != result2, "Each upload should have unique key")
    }
}