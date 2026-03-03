package com.adoptu.dto

import kotlinx.serialization.Serializable

enum class UserRole {
    ADMIN, RESCUER, ADOPTER
}

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val role: UserRole,
    val lastAcceptedPrivacyPolicy: Long? = null,
    val lastAcceptedTermsAndConditions: Long? = null
)

@Serializable
data class AcceptTermsRequest(
    val acceptPrivacyPolicy: Boolean = false,
    val acceptTermsAndConditions: Boolean = false
)