package com.adoptu.services

import com.adoptu.dto.*
import com.adoptu.models.BlockedRescuers
import com.adoptu.models.TemporalHomeRequests
import com.adoptu.models.TemporalHomes
import com.adoptu.ports.NotificationPort
import com.adoptu.repositories.PetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TemporalHomeService {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var notificationAdapter: NotificationPort? = null
    private val petRepository = PetRepository

    fun setNotificationAdapter(adapter: NotificationPort) {
        notificationAdapter = adapter
    }

    fun getTemporalHome(userId: Int): TemporalHomeDto? = transaction {
        val result = TemporalHomes.selectAll()
            .where { TemporalHomes.userId eq userId }
            .firstOrNull()
        if (result != null) {
            TemporalHomeDto(
                userId = result[TemporalHomes.userId],
                alias = result[TemporalHomes.alias],
                country = result[TemporalHomes.country],
                state = result[TemporalHomes.state],
                city = result[TemporalHomes.city],
                zip = result[TemporalHomes.zip],
                neighborhood = result[TemporalHomes.neighborhood],
                createdAt = result[TemporalHomes.createdAt]
            )
        } else null
    }

    fun createTemporalHome(userId: Int, request: CreateTemporalHomeRequest): TemporalHomeDto {
        val createdAt = System.currentTimeMillis()
        return transaction {
            TemporalHomes.insert {
                it[TemporalHomes.userId] = userId
                it[TemporalHomes.alias] = request.alias
                it[TemporalHomes.country] = request.country
                it[TemporalHomes.state] = request.state
                it[TemporalHomes.city] = request.city
                it[TemporalHomes.zip] = request.zip
                it[TemporalHomes.neighborhood] = request.neighborhood
                it[TemporalHomes.createdAt] = createdAt
            }

            TemporalHomeDto(
                userId = userId,
                alias = request.alias,
                country = request.country,
                state = request.state,
                city = request.city,
                zip = request.zip,
                neighborhood = request.neighborhood,
                createdAt = createdAt
            )
        }
    }

    fun searchTemporalHomes(params: TemporalHomeSearchParams): List<TemporalHomeDto> = transaction {
        var query = TemporalHomes.selectAll()

        params.country?.let { country ->
            query = query.where { TemporalHomes.country eq country }
        }
        params.state?.let { state ->
            query = query.where { TemporalHomes.state eq state }
        }
        params.city?.let { city ->
            query = query.where { TemporalHomes.city eq city }
        }
        params.zip?.let { zip ->
            query = query.where { TemporalHomes.zip eq zip }
        }
        params.neighborhood?.let { neighborhood ->
            query = query.where { TemporalHomes.neighborhood eq neighborhood }
        }

        query.map { row ->
            TemporalHomeDto(
                userId = row[TemporalHomes.userId],
                alias = row[TemporalHomes.alias],
                country = row[TemporalHomes.country],
                state = row[TemporalHomes.state],
                city = row[TemporalHomes.city],
                zip = row[TemporalHomes.zip],
                neighborhood = row[TemporalHomes.neighborhood],
                createdAt = row[TemporalHomes.createdAt]
            )
        }
    }

    fun sendRequest(requesterId: Int, request: SendTemporalHomeRequestRequest): Result<Int> {
        val temporalHome = getTemporalHome(request.temporalHomeId)
            ?: return Result.failure(IllegalArgumentException("Temporal home not found"))

        val isBlocked = isBlocked(request.temporalHomeId, requesterId)
        if (isBlocked) {
            return Result.failure(IllegalArgumentException("You have been blocked by this temporal home"))
        }

        val requester = UserService.getById(requesterId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (!requester.activeRoles.contains(UserRole.RESCUER) && !requester.activeRoles.contains(UserRole.ADMIN)) {
            return Result.failure(IllegalArgumentException("Only rescuers can send temporal home requests"))
        }

        val pet = if (request.petId != null) petRepository.getById(request.petId) else null

        val createdRequestId = transaction {
            val createdAt = System.currentTimeMillis()
            TemporalHomeRequests.insert {
                it[TemporalHomeRequests.temporalHomeId] = request.temporalHomeId
                it[TemporalHomeRequests.rescuerId] = requesterId
                it[TemporalHomeRequests.petId] = request.petId
                it[TemporalHomeRequests.message] = request.message
                it[TemporalHomeRequests.status] = "SENT"
                it[TemporalHomeRequests.createdAt] = createdAt
            } get TemporalHomeRequests.id
        }

        val temporalHomeEmail = UserService.getById(request.temporalHomeId)?.username
        if (temporalHomeEmail != null && notificationAdapter != null) {
            val baseUrl = "https://adopt-u.com"
            val spamReportLink = "$baseUrl/temporal-home/block/${request.temporalHomeId}?rescuer=$requesterId"
            
            scope.launch {
                notificationAdapter?.sendTemporalHomeRequest(
                    temporalHomeEmail = temporalHomeEmail,
                    temporalHomeAlias = temporalHome.alias,
                    rescuerName = requester.displayName,
                    petName = pet?.name,
                    message = request.message,
                    spamReportLink = spamReportLink
                )
            }
        }

        return Result.success(createdRequestId)
    }

    fun isBlocked(temporalHomeId: Int, rescuerId: Int): Boolean = transaction {
        val blocked = BlockedRescuers.selectAll()
            .where { (BlockedRescuers.temporalHomeId eq temporalHomeId).and(BlockedRescuers.rescuerId eq rescuerId) }
            .firstOrNull()
        blocked != null
    }

    fun blockRescuer(temporalHomeId: Int, rescuerId: Int): Boolean = transaction {
        val alreadyBlocked = BlockedRescuers.selectAll()
            .where { (BlockedRescuers.temporalHomeId eq temporalHomeId).and(BlockedRescuers.rescuerId eq rescuerId) }
            .firstOrNull()

        if (alreadyBlocked != null) {
            return@transaction false
        }

        val createdAt = System.currentTimeMillis()
        BlockedRescuers.insert {
            it[BlockedRescuers.temporalHomeId] = temporalHomeId
            it[BlockedRescuers.rescuerId] = rescuerId
            it[BlockedRescuers.createdAt] = createdAt
        }
        true
    }

    fun getMyRequests(userId: Int): List<TemporalHomeRequestDto> = transaction {
        val requests = TemporalHomeRequests.selectAll()
            .where { TemporalHomeRequests.temporalHomeId eq userId }

        requests.map { row ->
            val rescuer = UserService.getById(row[TemporalHomeRequests.rescuerId])
            val petId = row[TemporalHomeRequests.petId]
            val pet = if (petId != null) petRepository.getById(petId) else null

            TemporalHomeRequestDto(
                id = row[TemporalHomeRequests.id],
                temporalHomeId = row[TemporalHomeRequests.temporalHomeId],
                rescuerId = row[TemporalHomeRequests.rescuerId],
                rescuerName = rescuer?.displayName,
                petId = petId,
                petName = pet?.name,
                message = row[TemporalHomeRequests.message],
                status = row[TemporalHomeRequests.status],
                createdAt = row[TemporalHomeRequests.createdAt]
            )
        }
    }
}