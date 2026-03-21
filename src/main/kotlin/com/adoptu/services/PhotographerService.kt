package com.adoptu.services

import com.adoptu.dto.PhotographerDto
import com.adoptu.dto.UserDto
import com.adoptu.models.PhotographyRequests
import com.adoptu.models.Photographers
import com.adoptu.ports.NotificationPort
import com.adoptu.repositories.PetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object PhotographerService {
    const val ONE_WEEK = 7 * 24 * 60 * 60 * 1000L

    private val petRepository = PetRepository
    private var notificationAdapter: NotificationPort? = null

    fun setNotificationAdapter(adapter: NotificationPort) {
        notificationAdapter = adapter
    }

    fun getPhotographers(): List<PhotographerDto> = UserService.getPhotographers()

    fun canSendMessage(userId: Int): Boolean {
        val oneWeekAgo = System.currentTimeMillis() - ONE_WEEK
        val requests = transaction {
            PhotographyRequests.selectAll()
                .where { PhotographyRequests.requesterId.eq(userId).and(PhotographyRequests.createdAt.greaterEq(oneWeekAgo)) }
                .count()
        }
        return requests == 0L
    }

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

        val requester = UserService.getById(requesterId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        val pet = if (petId != null) petRepository.getById(petId) else null

        val createdRequestIds = mutableListOf<Int>()

        transaction {
            for (photographerId in photographerIds) {
                val photographer = UserService.getPhotographers().find { it.userId == photographerId }
                    ?: continue

                val createdAt = System.currentTimeMillis()
                val requestId = PhotographyRequests.insert {
                    it[PhotographyRequests.photographerId] = photographerId
                    it[PhotographyRequests.requesterId] = requesterId
                    it[PhotographyRequests.petId] = petId
                    it[PhotographyRequests.message] = message
                    it[PhotographyRequests.status] = "PENDING"
                    it[PhotographyRequests.createdAt] = createdAt
                } get PhotographyRequests.id

                createdRequestIds.add(requestId!!)

                val adapter = notificationAdapter
                val photo = photographer
                val req = requester
                val petData = pet
                CoroutineScope(Dispatchers.IO).launch {
                    adapter?.sendPhotographerRequest(
                        photographerEmail = photo.username ?: "",
                        photographerName = photo.displayName,
                        requesterName = req.displayName,
                        petName = petData?.name,
                        message = message,
                        fee = photo.photographerFee,
                        currency = photo.photographerCurrency
                    )
                }
            }
        }

        return Result.success(createdRequestIds)
    }

    fun getMyRequests(userId: Int): List<Map<String, Any?>> {
        return transaction {
            PhotographyRequests.selectAll()
                .where { PhotographyRequests.requesterId eq userId }
                .map { row ->
                    val photographer = UserService.getPhotographers().find { it.userId == row[PhotographyRequests.photographerId] }
                    mapOf(
                        "id" to row[PhotographyRequests.id],
                        "photographerId" to row[PhotographyRequests.photographerId],
                        "photographerName" to photographer?.displayName,
                        "photographerFee" to photographer?.photographerFee,
                        "photographerCurrency" to photographer?.photographerCurrency,
                        "petId" to row[PhotographyRequests.petId],
                        "message" to row[PhotographyRequests.message],
                        "status" to row[PhotographyRequests.status],
                        "scheduledDate" to row[PhotographyRequests.scheduledDate],
                        "createdAt" to row[PhotographyRequests.createdAt]
                    )
                }
        }
    }

    fun getRequestsForPhotographer(photographerId: Int): List<Map<String, Any?>> {
        return transaction {
            PhotographyRequests.selectAll()
                .where { PhotographyRequests.photographerId eq photographerId }
                .map { row ->
                    val requester = UserService.getById(row[PhotographyRequests.requesterId])
                    val pet = if (row[PhotographyRequests.petId] != null) petRepository.getById(row[PhotographyRequests.petId]!!) else null
                    mapOf(
                        "id" to row[PhotographyRequests.id],
                        "requesterId" to row[PhotographyRequests.requesterId],
                        "requesterName" to requester?.displayName,
                        "petId" to row[PhotographyRequests.petId],
                        "petName" to pet?.name,
                        "message" to row[PhotographyRequests.message],
                        "status" to row[PhotographyRequests.status],
                        "scheduledDate" to row[PhotographyRequests.scheduledDate],
                        "createdAt" to row[PhotographyRequests.createdAt]
                    )
                }
        }
    }
}
