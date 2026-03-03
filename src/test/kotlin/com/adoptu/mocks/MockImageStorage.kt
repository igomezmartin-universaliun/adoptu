package com.adoptu.mocks

import com.adoptu.domains.image.ImageStoragePort
import java.io.InputStream

class MockImageStorage : ImageStoragePort {
    private val storedImages = mutableMapOf<String, ByteArray>()
    private var shouldFail = false

    fun setFailMode(fail: Boolean) {
        shouldFail = fail
    }

    override suspend fun uploadImage(petId: Int, imageName: String, contentType: String, inputStream: InputStream): String {
        if (shouldFail) {
            throw RuntimeException("Failed to upload image")
        }
        storedImages[imageName] = inputStream.readBytes()
        return "https://mock-storage.example.com/$imageName"
    }

    override suspend fun deleteImage(imageUrl: String): Boolean {
        if (shouldFail) {
            throw RuntimeException("Failed to delete image")
        }
        val key = imageUrl.substringAfterLast("/")
        storedImages.remove(key)
        return true
    }

    override fun getImageUrl(petId: Int, imageKey: String): String {
        return "https://mock-storage.example.com/$imageKey"
    }

    fun getStoredImages(): Map<String, ByteArray> = storedImages.toMap()

    fun clear() {
        storedImages.clear()
        shouldFail = false
    }
}
