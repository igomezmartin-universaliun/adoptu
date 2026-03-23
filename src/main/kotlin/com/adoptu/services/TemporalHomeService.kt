package com.adoptu.services

import com.adoptu.dto.*
import com.adoptu.ports.NotificationPort
import com.adoptu.ports.TemporalHomeRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TemporalHomeService(
    private val temporalHomeRepository: TemporalHomeRepositoryPort,
    private val notificationAdapter: NotificationPort,
    private val userService: UserService
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getTemporalHome(userId: Int): TemporalHomeDto? = temporalHomeRepository.getTemporalHome(userId)

    fun createTemporalHome(userId: Int, request: CreateTemporalHomeRequest): TemporalHomeDto =
        temporalHomeRepository.createTemporalHome(userId, request)

    fun updateTemporalHome(userId: Int, request: UpdateTemporalHomeRequest): TemporalHomeDto? =
        temporalHomeRepository.updateTemporalHome(userId, request)

    fun searchTemporalHomes(params: TemporalHomeSearchParams): List<TemporalHomeDto> =
        temporalHomeRepository.searchTemporalHomes(params)

    fun sendRequest(requesterId: Int, request: SendTemporalHomeRequestRequest): Result<Int> {
        val temporalHome = getTemporalHome(request.temporalHomeId)
            ?: return Result.failure(IllegalArgumentException("Temporal home not found"))

        val isBlocked = temporalHomeRepository.isBlocked(request.temporalHomeId, requesterId)
        if (isBlocked) {
            return Result.failure(IllegalArgumentException("You have been blocked by this temporal home"))
        }

        val requester = userService.getById(requesterId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (!requester.activeRoles.contains(UserRole.RESCUER) && !requester.activeRoles.contains(UserRole.ADMIN)) {
            return Result.failure(IllegalArgumentException("Only rescuers can send temporal home requests"))
        }

        val createdRequestId = temporalHomeRepository.createTemporalHomeRequest(
            temporalHomeId = request.temporalHomeId,
            rescuerId = requesterId,
            petId = request.petId,
            message = request.message
        )

        val temporalHomeEmail = userService.getById(request.temporalHomeId)?.username
        if (temporalHomeEmail != null && notificationAdapter != null) {
            val baseUrl = "https://adopt-u.com"
            val spamReportLink = "$baseUrl/temporal-home/block/${request.temporalHomeId}?rescuer=$requesterId"
            val pet = if (request.petId != null) temporalHomeRepository.getTemporalHome(request.temporalHomeId) else null
            
            scope.launch {
                notificationAdapter.sendTemporalHomeRequest(
                    temporalHomeEmail = temporalHomeEmail,
                    temporalHomeAlias = temporalHome.alias,
                    rescuerName = requester.displayName,
                    petName = pet?.alias,
                    message = request.message,
                    spamReportLink = spamReportLink
                )
            }
        }

        return Result.success(createdRequestId)
    }

    fun isBlocked(temporalHomeId: Int, rescuerId: Int): Boolean =
        temporalHomeRepository.isBlocked(temporalHomeId, rescuerId)

    fun blockRescuer(temporalHomeId: Int, rescuerId: Int): Boolean =
        temporalHomeRepository.blockRescuer(temporalHomeId, rescuerId)

    fun getMyRequests(userId: Int): List<TemporalHomeRequestDto> =
        temporalHomeRepository.getMyRequests(userId)
}
