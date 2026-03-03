package com.adoptu.domains.image

import java.io.InputStream

interface ImageStoragePort {
    suspend fun uploadImage(petId: Int, imageName: String, contentType: String, inputStream: InputStream): String
    suspend fun deleteImage(imageUrl: String): Boolean
    fun getImageUrl(petId: Int, imageKey: String): String
}
