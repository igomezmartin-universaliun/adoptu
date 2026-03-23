package com.adoptu.services

import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.PhotographerDto
import com.adoptu.dto.PhotographerSettingsRequest
import com.adoptu.dto.UserDto
import com.adoptu.ports.UserRepositoryPort

class UserService(
    private val userRepository: UserRepositoryPort,
    private val photographerService: PhotographerService
) {
    fun getById(userId: Int): UserDto? = userRepository.getById(userId)
    
    fun getPhotographerById(userId: Int): PhotographerDto? = photographerService.getPhotographerById(userId)
    
    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto> = 
        photographerService.getPhotographers(country, state)
    
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
    
    fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto? = 
        userRepository.updatePhotographerSettings(userId, request)
    
    fun acceptTerms(userId: Int, request: AcceptTermsRequest): UserDto? = userRepository.acceptTerms(userId, request)
}
