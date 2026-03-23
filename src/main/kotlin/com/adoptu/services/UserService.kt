package com.adoptu.services

import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.UserDto
import com.adoptu.ports.UserRepositoryPort

class UserService(
    private val userRepository: UserRepositoryPort
) {
    fun getById(userId: Int): UserDto? = userRepository.getById(userId)
    
    fun getRescuers(): List<UserDto> = userRepository.getRescuers()
    
    fun isRoleActive(userId: Int, role: com.adoptu.dto.UserRole): Boolean = 
        userRepository.isRoleActive(userId, role)
    
    fun activateRescuerProfile(userId: Int): UserDto? = userRepository.activateRescuerProfile(userId)
    
    fun deactivateRescuerProfile(userId: Int): UserDto? = userRepository.deactivateRescuerProfile(userId)
    
    fun activatePhotographerProfile(userId: Int): UserDto? = userRepository.activatePhotographerProfile(userId)
    
    fun deactivatePhotographerProfile(userId: Int): UserDto? = userRepository.deactivatePhotographerProfile(userId)
    
    fun activateTemporalHomeProfile(userId: Int): UserDto? = userRepository.activateTemporalHomeProfile(userId)
    
    fun deactivateTemporalHomeProfile(userId: Int): UserDto? = userRepository.deactivateTemporalHomeProfile(userId)
    
    fun updateProfile(userId: Int, displayName: String, language: String? = null): UserDto? = 
        userRepository.updateProfile(userId, displayName, language)
    
    fun updateLanguage(userId: Int, language: String): UserDto? = userRepository.updateLanguage(userId, language)
    
    fun acceptTerms(userId: Int, request: AcceptTermsRequest): UserDto? = userRepository.acceptTerms(userId, request)
}
