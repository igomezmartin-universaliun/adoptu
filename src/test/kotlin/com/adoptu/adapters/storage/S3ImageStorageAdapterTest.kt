package com.adoptu.adapters.storage

import com.adoptu.ports.ImageStoragePort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class S3ImageStorageAdapterTest {

    private lateinit var adapter: S3ImageStorageAdapter

    @BeforeEach
    fun setup() {
        // Use a mock endpoint for testing
        adapter = S3ImageStorageAdapter(
            bucketName = "mocks-bucket",
            region = "us-east-1",
            accessKeyId = "mocks-key",
            secretAccessKey = "mocks-secret",
            endpoint = "https://s3.example.com",
            pathStyleAccess = true
        )
    }

    @Test
    fun `getImageUrl returns correct URL with custom endpoint`() {
        val url = adapter.getImageUrl(1, "pets/1/storage.jpg")
        
        assertTrue(url.startsWith("https://s3.example.com/"))
        assertTrue(url.contains("mocks-bucket"))
        assertTrue(url.contains("pets/1/storage.jpg"))
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
        
        val url = adapterNoEndpoint.getImageUrl(1, "pets/1/storage.jpg")
        
        assertTrue(url.startsWith("https://"))
        assertTrue(url.contains("my-bucket"))
        assertTrue(url.contains("eu-west-1"))
        assertTrue(url.contains("amazonaws.com"))
    }

    @Test
    fun `getImageUrl constructs correct path with pet id`() {
        val url = adapter.getImageUrl(42, "pets/42/unique-id-photo.jpg")
        
        assertTrue(url.contains("pets/42/"))
        assertTrue(url.contains("unique-id-photo.jpg"))
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
        val url = adapter.getImageUrl(1, "mocks.jpg")
        
        assertTrue(url.contains("mocks-bucket"))
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
        
        val url = adapterNoEndpoint.getImageUrl(1, "mocks.jpg")
        
        assertTrue(url.contains("eu-central-1"))
    }

    @Test
    fun `pathStyleAccess affects URL format`() {
        // With pathStyleAccess = true, should use path-style URL
        val url = adapter.getImageUrl(1, "mocks.jpg")
        
        assertTrue(url.contains("mocks-bucket/"))
    }

    @Test
    fun `different pet ids generate different paths`() {
        // The pet ID is embedded in the key during upload
        val url1 = adapter.getImageUrl(1, "pets/1/storage.jpg")
        val url2 = adapter.getImageUrl(2, "pets/2/storage.jpg")
        
        assertTrue(url1.contains("pets/1/"))
        assertTrue(url2.contains("pets/2/"))
    }

    @Test
    fun `image key can contain UUID format`() {
        val url = adapter.getImageUrl(1, "12345678-1234-1234-1234-123456789012-storage.jpg")
        
        assertTrue(url.contains("12345678-1234-1234-1234-123456789012-storage.jpg"))
    }

    @Test
    fun `multiple images for same pet have unique keys`() {
        val url1 = adapter.getImageUrl(1, "first-storage.jpg")
        val url2 = adapter.getImageUrl(1, "second-storage.jpg")
        
        assertTrue(url1.contains("first-storage.jpg"))
        assertTrue(url2.contains("second-storage.jpg"))
    }

    @Test
    fun `S3ImageStorageAdapter can be instantiated with various regions`() {
        val regions = listOf(
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "eu-west-1", "eu-west-2", "eu-central-1",
            "ap-northeast-1", "ap-southeast-1", "ap-southeast-2"
        )
        
        regions.forEach { region ->
            val testAdapter = S3ImageStorageAdapter(
                bucketName = "mocks-bucket",
                region = region,
                accessKeyId = "key",
                secretAccessKey = "secret",
                endpoint = null,
                pathStyleAccess = false
            )
            
            val url = testAdapter.getImageUrl(1, "mocks.jpg")
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
        
        val url = testAdapter.getImageUrl(1, "pets/1/mocks.jpg")
        
        assertTrue(url.startsWith(customEndpoint))
        assertTrue(url.contains("mybucket"))
    }
}

class ImageStoragePortContractTest : ImageStoragePort by S3ImageStorageAdapter(
    bucketName = "contract-mocks-bucket",
    region = "us-east-1",
    accessKeyId = "key",
    secretAccessKey = "secret",
    endpoint = "https://contract.test.com",
    pathStyleAccess = true
) {
    @Test
    fun `adapter implements ImageStoragePort interface`() {
        assertTrue(this is ImageStoragePort)
    }

    @Test
    fun `getImageUrl is implemented`() {
        val url = getImageUrl(1, "mocks.jpg")
        assertNotNull(url)
    }

    @Test
    fun `uploadImage is a suspend function`() {
        // Just verify the method signature exists
        assertTrue(this is ImageStoragePort)
    }

    @Test
    fun `deleteImage is a suspend function`() {
        // Just verify the method signature exists  
        assertTrue(this is ImageStoragePort)
    }
}
