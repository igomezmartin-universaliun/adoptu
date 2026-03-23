package com.adoptu.ports

import com.adoptu.dto.*

interface UserRepositoryPort {
    fun getById(userId: Int): UserDto?
    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto>
    fun getRescuers(): List<UserDto>
    fun isRoleActive(userId: Int, role: UserRole): Boolean
    fun activateRescuerProfile(userId: Int): UserDto?
    fun deactivateRescuerProfile(userId: Int): UserDto?
    fun activatePhotographerProfile(userId: Int): UserDto?
    fun deactivatePhotographerProfile(userId: Int): UserDto?
    fun activateTemporalHomeProfile(userId: Int): UserDto?
    fun deactivateTemporalHomeProfile(userId: Int): UserDto?
    fun updateProfile(userId: Int, displayName: String, language: String? = null): UserDto?
    fun updateLanguage(userId: Int, language: String): UserDto?
    fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto?
    fun acceptTerms(userId: Int, request: AcceptTermsRequest): UserDto?
}
