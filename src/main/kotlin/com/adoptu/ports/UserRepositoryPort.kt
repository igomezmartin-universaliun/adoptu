package com.adoptu.ports

import com.adoptu.dto.input.AcceptTermsRequest
import com.adoptu.dto.input.PhotographerDto
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.UserDto
import com.adoptu.dto.input.UserRole

interface UserRepositoryPort {
    fun getById(userId: Int): UserDto?
    fun getByEmail(email: String): UserDto?
    fun getAllUsers(): List<UserDto>
    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto>
    fun getRescuers(): List<UserDto>
    fun banUser(userId: Int, reason: String?): Boolean
    fun unbanUser(userId: Int): Boolean
    fun isBanned(userId: Int): Boolean
    fun isRoleActive(userId: Int, role: UserRole): Boolean
    fun activateRescuerProfile(userId: Int): UserDto?
    fun deactivateRescuerProfile(userId: Int): UserDto?
    fun activateTemporalHomeProfile(userId: Int): UserDto?
    fun deactivateTemporalHomeProfile(userId: Int): UserDto?
    fun updateProfile(userId: Int, displayName: String, language: String? = null): UserDto?
    fun updateLanguage(userId: Int, language: String): UserDto?
    fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto?
    fun acceptTerms(userId: Int, request: AcceptTermsRequest): UserDto?
    fun isEmailVerified(userId: Int): Boolean
    fun setEmailVerified(userId: Int, verified: Boolean): Boolean
    fun createEmailVerificationToken(userId: Int, token: String, expiresAt: Long): Boolean
    fun verifyToken(token: String): Int?
    fun deleteVerificationTokens(userId: Int)
    fun getVerificationAttemptsToday(userId: Int): Int
    fun recordVerificationAttempt(userId: Int)
}
