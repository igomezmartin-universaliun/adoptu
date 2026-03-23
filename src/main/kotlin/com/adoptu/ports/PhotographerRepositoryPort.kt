package com.adoptu.ports

import com.adoptu.dto.PhotographyRequestDto
import com.adoptu.dto.PhotographerDto
import com.adoptu.dto.PhotographerSettingsRequest

interface PhotographerRepositoryPort {
    fun canSendMessage(userId: Int): Boolean
    fun createPhotographyRequest(requesterId: Int, photographerId: Int, petId: Int?, message: String): Int
    fun getMyRequests(userId: Int): List<PhotographyRequestDto>
    fun getRequestsForPhotographer(photographerId: Int): List<PhotographyRequestDto>
    fun getPhotographerById(userId: Int): PhotographerDto?
    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto>
    fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto?
}
