package com.adoptu.ports

data class EmailVerificationTokenInfo(
    val token: String,
    val expiresAt: Long,
    val createdAt: Long
)
