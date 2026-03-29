package com.adoptu.ports

import com.adoptu.dto.input.PhotographerDto
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.PhotographyRequestDto
import com.adoptu.dto.input.UserDto

interface PhotographerRepositoryPort {
    fun canSendMessage(userId: Int): Boolean
    fun createPhotographyRequest(requesterId: Int, photographerId: Int, petId: Int?, message: String): Int
    fun getMyRequests(userId: Int): List<PhotographyRequestDto>
    fun getRequestsForPhotographer(photographerId: Int): List<PhotographyRequestDto>
    fun getRequestById(requestId: Int): PhotographyRequestDto?
    fun updatePhotographyRequest(requestId: Int, status: String?, scheduledDate: Long?)
    fun getPhotographerById(userId: Int): PhotographerDto?
    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto>
    fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto?
    fun activatePhotographerProfile(userId: Int): UserDto?
    fun deactivatePhotographerProfile(userId: Int): UserDto?
}
