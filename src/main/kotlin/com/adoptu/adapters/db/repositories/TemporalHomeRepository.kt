package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.BlockedRescuers
import com.adoptu.adapters.db.TemporalHomeRequests
import com.adoptu.adapters.db.TemporalHomes
import com.adoptu.dto.input.CreateTemporalHomeRequest
import com.adoptu.dto.input.TemporalHomeDto
import com.adoptu.dto.input.TemporalHomeRequestDto
import com.adoptu.dto.input.TemporalHomeSearchParams
import com.adoptu.dto.input.UpdateTemporalHomeRequest
import com.adoptu.ports.PetRepositoryPort
import com.adoptu.ports.TemporalHomeRepositoryPort
import com.adoptu.ports.UserRepositoryPort
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TemporalHomeRepositoryImpl(
    private val petRepository: PetRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val clock: Clock
) : TemporalHomeRepositoryPort {

    override fun getTemporalHome(userId: Int): TemporalHomeDto? = transaction {
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

    override fun createTemporalHome(userId: Int, request: CreateTemporalHomeRequest): TemporalHomeDto {
        val createdAt = clock.now().toEpochMilliseconds()
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

    override fun updateTemporalHome(userId: Int, request: UpdateTemporalHomeRequest): TemporalHomeDto? = transaction {
        val existing = TemporalHomes.selectAll()
            .where { TemporalHomes.userId eq userId }
            .firstOrNull()
            ?: return@transaction null

        val updatedAlias = request.alias ?: existing[TemporalHomes.alias]
        val updatedCountry = request.country ?: existing[TemporalHomes.country]
        val updatedState = request.state ?: existing[TemporalHomes.state]
        val updatedCity = request.city ?: existing[TemporalHomes.city]
        val updatedZip = request.zip ?: existing[TemporalHomes.zip]
        val updatedNeighborhood = request.neighborhood ?: existing[TemporalHomes.neighborhood]

        TemporalHomes.update({ TemporalHomes.userId eq userId }) {
            it[TemporalHomes.alias] = updatedAlias
            it[TemporalHomes.country] = updatedCountry
            it[TemporalHomes.state] = updatedState
            it[TemporalHomes.city] = updatedCity
            it[TemporalHomes.zip] = updatedZip
            it[TemporalHomes.neighborhood] = updatedNeighborhood
        }

        TemporalHomeDto(
            userId = userId,
            alias = updatedAlias,
            country = updatedCountry,
            state = updatedState,
            city = updatedCity,
            zip = updatedZip,
            neighborhood = updatedNeighborhood,
            createdAt = existing[TemporalHomes.createdAt]
        )
    }

    override fun searchTemporalHomes(params: TemporalHomeSearchParams): List<TemporalHomeDto> = transaction {
        var conditions: Op<Boolean>? = null

        params.country?.let { country ->
            conditions = conditions?.and(TemporalHomes.country eq country) ?: (TemporalHomes.country eq country)
        }
        params.state?.let { state ->
            conditions = conditions?.and(TemporalHomes.state eq state) ?: (TemporalHomes.state eq state)
        }
        params.city?.let { city ->
            conditions = conditions?.and(TemporalHomes.city eq city) ?: (TemporalHomes.city eq city)
        }
        params.zip?.let { zip ->
            conditions = conditions?.and(TemporalHomes.zip eq zip) ?: (TemporalHomes.zip eq zip)
        }
        params.neighborhood?.let { neighborhood ->
            conditions = conditions?.and(TemporalHomes.neighborhood eq neighborhood) ?: (TemporalHomes.neighborhood eq neighborhood)
        }

        val query = if (conditions != null) {
            TemporalHomes.selectAll().where { conditions!! }
        } else {
            TemporalHomes.selectAll()
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

    override fun createTemporalHomeRequest(
        temporalHomeId: Int,
        rescuerId: Int,
        petId: Int?,
        message: String
    ): Int = transaction {
        val createdAt = clock.now().toEpochMilliseconds()
        val requestId = TemporalHomeRequests.insert {
            it[TemporalHomeRequests.temporalHomeId] = temporalHomeId
            it[TemporalHomeRequests.rescuerId] = rescuerId
            it[TemporalHomeRequests.petId] = petId
            it[TemporalHomeRequests.message] = message
            it[TemporalHomeRequests.status] = "SENT"
            it[TemporalHomeRequests.createdAt] = createdAt
        } get TemporalHomeRequests.id

        requestId!!
    }

    override fun isBlocked(temporalHomeId: Int, rescuerId: Int): Boolean = transaction {
        val blocked = BlockedRescuers.selectAll()
            .where { (BlockedRescuers.temporalHomeId eq temporalHomeId).and(BlockedRescuers.rescuerId eq rescuerId) }
            .firstOrNull()
        blocked != null
    }

    override fun blockRescuer(temporalHomeId: Int, rescuerId: Int): Boolean = transaction {
        val alreadyBlocked = BlockedRescuers.selectAll()
            .where { (BlockedRescuers.temporalHomeId eq temporalHomeId).and(BlockedRescuers.rescuerId eq rescuerId) }
            .firstOrNull()

        if (alreadyBlocked != null) {
            return@transaction false
        }

        val createdAt = clock.now().toEpochMilliseconds()
        BlockedRescuers.insert {
            it[BlockedRescuers.temporalHomeId] = temporalHomeId
            it[BlockedRescuers.rescuerId] = rescuerId
            it[BlockedRescuers.createdAt] = createdAt
        }
        true
    }

    override fun getMyRequests(userId: Int): List<TemporalHomeRequestDto> = transaction {
        val requests = TemporalHomeRequests.selectAll()
            .where { TemporalHomeRequests.temporalHomeId eq userId }

        requests.map { row ->
            val rescuer = userRepository.getById(row[TemporalHomeRequests.rescuerId])
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
