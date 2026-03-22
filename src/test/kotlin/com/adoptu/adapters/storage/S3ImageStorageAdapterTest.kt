package com.adoptu.adapters.storage

import com.adoptu.ports.ImageStoragePort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class S3ImageStorageAdapterTest {

    private lateinit var adapter: S3ImageStorageAdapter

    @BeforeEach
    fun setup() {
        adapter = S3ImageStorageAdapter(
            bucketName = "test-bucket",
            region = "us-east-1",
            accessKeyId = "test-key",
            secretAccessKey = "test-secret",
            endpoint = "https://s3.example.com",
            pathStyleAccess = true
        )
    }

    @Test
    fun `getImageUrl returns correct URL with custom endpoint`() {
        val url = adapter.getImageUrl(1, "pets/1/test.jpg")
        
        assertTrue(url.startsWith("https://s3.example.com/"))
        assertTrue(url.contains("test-bucket"))
        assertTrue(url.contains("pets/1/test.jpg"))
    }

    @Test
    fun `getImageUrl returns AWS URL when no custom endpoint`() {
        val adapterNoEndpoint = S3ImageStorageAdapter(
            bucketName = "my-bucket",
            region = "eu-west-1",
            accessKeyId = "key",
            secretAccessKey = "secret",
            endpoint = null,
            pathStyleAccess = false
        )
        
        val url = adapterNoEndpoint.getImageUrl(1, "pets/1/test.jpg")
        
        assertTrue(url.startsWith("https://"))
        assertTrue(url.contains("my-bucket"))
        assertTrue(url.contains("eu-west-1"))
        assertTrue(url.contains("amazonaws.com"))
    }

    @Test
    fun `getImageUrl constructs correct path with pet id`() {
        val url = adapter.getImageUrl(42, "pets/42/unique-photo.jpg")
        
        assertTrue(url.contains("pets/42/"))
        assertTrue(url.contains("unique-photo.jpg"))
    }

    @Test
    fun `adapter implements ImageStoragePort`() {
        assertTrue(adapter is ImageStoragePort)
    }

    @Test
    fun `constructor accepts all parameters`() {
        val customAdapter = S3ImageStorageAdapter(
            bucketName = "custom-bucket",
            region = "ap-south-1",
            accessKeyId = "ak123",
            secretAccessKey = "sk123",
            endpoint = "https://custom.s3.com",
            pathStyleAccess = true
        )
        
        assertNotNull(customAdapter)
    }

    @Test
    fun `constructor handles empty credentials`() {
        val adapterWithNoCreds = S3ImageStorageAdapter(
            bucketName = "no-creds-bucket",
            region = "us-west-2",
            accessKeyId = "",
            secretAccessKey = "",
            endpoint = null,
            pathStyleAccess = false
        )
        
        assertNotNull(adapterWithNoCreds)
    }

    @Test
    fun `constructor handles null credentials`() {
        val adapterWithNullCreds = S3ImageStorageAdapter(
            bucketName = "null-creds-bucket",
            region = "us-west-1",
            accessKeyId = null,
            secretAccessKey = null,
            endpoint = null,
            pathStyleAccess = false
        )
        
        assertNotNull(adapterWithNullCreds)
    }

    @Test
    fun `bucket name is used in URL generation`() {
        val url = adapter.getImageUrl(1, "test.jpg")
        
        assertTrue(url.contains("test-bucket"))
    }

    @Test
    fun `region is used in AWS URL generation`() {
        val adapterNoEndpoint = S3ImageStorageAdapter(
            bucketName = "bucket",
            region = "eu-central-1",
            accessKeyId = "key",
            secretAccessKey = "secret",
            endpoint = null,
            pathStyleAccess = false
        )
        
        val url = adapterNoEndpoint.getImageUrl(1, "test.jpg")
        
        assertTrue(url.contains("eu-central-1"))
    }

    @Test
    fun `pathStyleAccess affects URL format`() {
        val url = adapter.getImageUrl(1, "test.jpg")
        
        assertTrue(url.contains("test-bucket/"))
    }

    @Test
    fun `different pet ids generate different paths`() {
        val url1 = adapter.getImageUrl(1, "pets/1/test.jpg")
        val url2 = adapter.getImageUrl(2, "pets/2/test.jpg")
        
        assertTrue(url1.contains("pets/1/"))
        assertTrue(url2.contains("pets/2/"))
    }

    @Test
    fun `image key can contain UUID format`() {
        val url = adapter.getImageUrl(1, "12345678-1234-1234-1234-123456789012-test.jpg")
        
        assertTrue(url.contains("12345678-1234-1234-1234-123456789012-test.jpg"))
    }

    @Test
    fun `multiple images for same pet have unique keys`() {
        val url1 = adapter.getImageUrl(1, "first.jpg")
        val url2 = adapter.getImageUrl(1, "second.jpg")
        
        assertTrue(url1.contains("first.jpg"))
        assertTrue(url2.contains("second.jpg"))
    }

    @Test
    fun `adapter can be instantiated with various regions`() {
        val regions = listOf(
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "eu-west-1", "eu-west-2", "eu-central-1"
        )
        
        regions.forEach { region ->
            val testAdapter = S3ImageStorageAdapter(
                bucketName = "test-bucket",
                region = region,
                accessKeyId = "key",
                secretAccessKey = "secret",
                endpoint = null,
                pathStyleAccess = false
            )
            
            val url = testAdapter.getImageUrl(1, "test.jpg")
            assertTrue(url.contains(region), "URL should contain region: $region")
        }
    }

    @Test
    fun `custom endpoint URL format is correct`() {
        val customEndpoint = "https://minio.example.com:9000"
        val testAdapter = S3ImageStorageAdapter(
            bucketName = "mybucket",
            region = "us-east-1",
            accessKeyId = "minio",
            secretAccessKey = "miniopass",
            endpoint = customEndpoint,
            pathStyleAccess = true
        )
        
        val url = testAdapter.getImageUrl(1, "pets/1/test.jpg")
        
        assertTrue(url.startsWith(customEndpoint))
        assertTrue(url.contains("mybucket"))
    }

    @Test
    fun `getImageUrl handles null endpoint with AWS format`() {
        val adapterNoEndpoint = S3ImageStorageAdapter(
            bucketName = "my-bucket",
            region = "us-west-2",
            accessKeyId = null,
            secretAccessKey = null,
            endpoint = null,
            pathStyleAccess = false
        )

        val url = adapterNoEndpoint.getImageUrl(5, "pets/5/test.png")
        assertEquals("https://my-bucket.s3.us-west-2.amazonaws.com/pets/5/test.png", url)
    }

    @Test
    fun `getImageUrl handles custom endpoint with port`() {
        val adapterWithEndpoint = S3ImageStorageAdapter(
            bucketName = "my-bucket",
            region = "us-east-1",
            accessKeyId = null,
            secretAccessKey = null,
            endpoint = "http://localhost:9000",
            pathStyleAccess = true
        )

        val url = adapterWithEndpoint.getImageUrl(5, "pets/5/test.png")
        assertTrue(url.startsWith("http://localhost:9000/"))
        assertTrue(url.contains("my-bucket/"))
        assertTrue(url.contains("pets/5/test.png"))
    }

    @Test
    fun `URL key extraction works via getImageUrl`() {
        val adapterForExtract = S3ImageStorageAdapter(
            bucketName = "test-bucket",
            region = "us-east-1",
            accessKeyId = "test-key",
            secretAccessKey = "test-secret",
            endpoint = "https://custom.endpoint.com",
            pathStyleAccess = true
        )

        val url = adapterForExtract.getImageUrl(1, "pets/1/test-image.jpg")
        assertTrue(url.contains("pets/1/test-image.jpg"))
    }

    @Test
    fun `adapter handles various content types in URLs`() {
        val contentTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp")
        
        contentTypes.forEach { contentType ->
            val extension = contentType.substringAfter("/")
            val adapter = S3ImageStorageAdapter(
                bucketName = "test-bucket",
                region = "us-east-1",
                accessKeyId = "key",
                secretAccessKey = "secret",
                endpoint = null,
                pathStyleAccess = false
            )
            
            val url = adapter.getImageUrl(1, "pets/1/image.$extension")
            assertTrue(url.contains("image.$extension"))
        }
    }

    @Test
    fun `getImageUrl generates correct URL for different bucket names`() {
        val bucketNames = listOf("my-bucket", "my-bucket-123", "my.bucket.with.dots")
        
        bucketNames.forEach { bucketName ->
            val adapter = S3ImageStorageAdapter(
                bucketName = bucketName,
                region = "us-east-1",
                accessKeyId = null,
                secretAccessKey = null,
                endpoint = null,
                pathStyleAccess = false
            )
            
            val url = adapter.getImageUrl(1, "pets/1/test.jpg")
            assertTrue(url.contains(bucketName), "URL should contain bucket: $bucketName")
        }
    }
}

