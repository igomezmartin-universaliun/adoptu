package com.adoptu.auth

import com.adoptu.dto.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class SessionUser(
    val userId: Int,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val role: UserRole
)
