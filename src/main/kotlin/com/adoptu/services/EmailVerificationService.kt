package com.adoptu.services

import com.adoptu.ports.UserRepositoryPort
import com.adoptu.ports.NotificationPort
import java.security.SecureRandom
import java.util.Base64

class EmailVerificationService(
    private val userRepository: UserRepositoryPort,
    private val notificationPort: NotificationPort
) {
    private val secureRandom = SecureRandom()
    private val tokenExpirationMs = 24 * 60 * 60 * 1000L

    suspend fun generateAndSendVerificationEmail(userId: Int, email: String, displayName: String): Boolean {
        val token = generateToken()
        val expiresAt = System.currentTimeMillis() + tokenExpirationMs
        
        val tokenCreated = userRepository.createEmailVerificationToken(userId, token, expiresAt)
        if (!tokenCreated) return false
        
        val verificationUrl = "http://localhost:8080/api/auth/verify-email?token=$token"
        val subject = "Verify your email - Adopt-U"
        val body = """
            Hello $displayName,
            
            Thank you for registering with Adopt-U!
            
            Please verify your email address by clicking the link below:
            $verificationUrl
            
            This link will expire in 24 hours.
            
            If you did not create an account, please ignore this email.
        """.trimIndent()
        
        return notificationPort.sendEmail(email, subject, body)
    }

    fun verifyToken(token: String): Boolean {
        val userId = userRepository.verifyToken(token) ?: return false
        val updated = userRepository.setEmailVerified(userId, true)
        if (updated) {
            userRepository.deleteVerificationTokens(userId)
        }
        return updated
    }

    fun isUserVerified(userId: Int): Boolean {
        return userRepository.isEmailVerified(userId)
    }

    suspend fun resendVerificationEmail(userId: Int, email: String, displayName: String): Boolean {
        userRepository.deleteVerificationTokens(userId)
        return generateAndSendVerificationEmail(userId, email, displayName)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
