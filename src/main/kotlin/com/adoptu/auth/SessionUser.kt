package com.adoptu.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionUser(
    val userId: Int,
    val email: String,
    val displayName: String
)
