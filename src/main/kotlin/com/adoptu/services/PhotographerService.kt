package com.adoptu.services

import com.adoptu.dto.PhotographerDto
import com.adoptu.ports.NotificationPort
import com.adoptu.ports.PhotographerRepositoryPort
import com.adoptu.ports.UserRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotographerService(
    private val photographerRepository: PhotographerRepositoryPort,
    private val notificationAdapter: NotificationPort?,
    private val userRepository: UserRepositoryPort
) {
    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto> = 
        photographerRepository.getPhotographers(country, state)

    fun getPhotographerById(userId: Int): PhotographerDto? = photographerRepository.getPhotographerById(userId)

    fun canSendMessage(userId: Int): Boolean = photographerRepository.canSendMessage(userId)

    fun createPhotographyRequest(
        requesterId: Int,
        photographerIds: List<Int>,
        petId: Int?,
        message: String
    ): Result<List<Int>> {
        if (photographerIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one photographer must be selected"))
        }
        if (photographerIds.size > 3) {
            return Result.failure(IllegalArgumentException("Maximum 3 photographers can be selected"))
        }

        if (!canSendMessage(requesterId)) {
            return Result.failure(IllegalArgumentException("You can only send photographer requests once per week"))
        }

        val requester = userRepository.getById(requesterId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        val createdRequestIds = mutableListOf<Int>()

        for (photographerId in photographerIds) {
            val photographer = photographerRepository.getPhotographers().find { it.userId == photographerId }
                ?: continue

            val requestId = photographerRepository.createPhotographyRequest(
                requesterId = requesterId,
                photographerId = photographerId,
                petId = petId,
                message = message
            )
            createdRequestIds.add(requestId)

            val adapter = notificationAdapter
            val photo = photographer
            val req = requester
            CoroutineScope(Dispatchers.IO).launch {
                adapter?.sendPhotographerRequest(
                    photographerEmail = photo.username ?: "",
                    photographerName = photo.displayName,
                    requesterName = req.displayName,
                    petName = null,
                    message = message,
                    fee = photo.photographerFee,
                    currency = photo.photographerCurrency
                )
            }
        }

        return Result.success(createdRequestIds)
    }

    fun getMyRequests(userId: Int): List<Map<String, Any?>> {
        return photographerRepository.getMyRequests(userId).map { dto ->
            mapOf(
                "id" to dto.id,
                "photographerId" to dto.photographerId,
                "photographerName" to dto.photographerName,
                "photographerFee" to null,
                "photographerCurrency" to null,
                "petId" to dto.petId,
                "message" to dto.message,
                "status" to dto.status,
                "scheduledDate" to dto.scheduledDate,
                "createdAt" to dto.createdAt
            )
        }
    }

    fun getRequestsForPhotographer(photographerId: Int): List<Map<String, Any?>> {
        return photographerRepository.getRequestsForPhotographer(photographerId).map { dto ->
            mapOf(
                "id" to dto.id,
                "requesterId" to dto.requesterId,
                "requesterName" to dto.requesterName,
                "petId" to dto.petId,
                "petName" to dto.petName,
                "message" to dto.message,
                "status" to dto.status,
                "scheduledDate" to dto.scheduledDate,
                "createdAt" to dto.createdAt
            )
        }
    }
}
