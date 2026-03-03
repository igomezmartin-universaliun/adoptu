package com.adoptu.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdoptionRequestDto(
    val id: Int,
    val petId: Int,
    val adopterId: Int,
    val message: String,
    val status: String,
    val createdAt: Long
)

@Serializable
data class CreateAdoptionRequestRequest(
    val message: String = ""
)