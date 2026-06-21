package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.Photographers
import com.adoptu.adapters.db.PhotographyRequests
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.dto.input.PhotographerDto
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.PhotographyRequestDto
import com.adoptu.dto.input.UserDto
import com.adoptu.dto.input.UserRole
import com.adoptu.ports.PetRepositoryPort
import com.adoptu.ports.PhotographerRepositoryPort
import com.adoptu.ports.UserRepositoryPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PhotographerRepositoryImpl(
    private val petRepository: PetRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val clock: Clock
) : PhotographerRepositoryPort {

    companion object {
        const val ONE_WEEK = 7 * 24 * 60 * 60 * 1000L
    }

    override fun canSendMessage(userId: Int): Boolean {
        val oneWeekAgo = clock.now().toEpochMilliseconds() - ONE_WEEK
        val requests = transaction {
            PhotographyRequests.selectAll()
                .where { PhotographyRequests.requesterId.eq(userId).and(PhotographyRequests.createdAt.greaterEq(oneWeekAgo)) }
                .count()
        }
        return requests == 0L
    }

    override fun createPhotographyRequest(
        requesterId: Int,
        photographerId: Int,
        petId: Int?,
        message: String
    ): Int = transaction {
        val createdAt = clock.now().toEpochMilliseconds()
        val requestId = PhotographyRequests.insert {
            it[PhotographyRequests.photographerId] = photographerId
            it[PhotographyRequests.requesterId] = requesterId
            it[PhotographyRequests.petId] = petId
            it[PhotographyRequests.message] = message
            it[PhotographyRequests.status] = "PENDING"
            it[PhotographyRequests.createdAt] = createdAt
        } get PhotographyRequests.id

        requestId
    }

    override fun getMyRequests(userId: Int): List<PhotographyRequestDto> = transaction {
        PhotographyRequests.selectAll()
            .where { PhotographyRequests.requesterId eq userId }
            .map { row ->
                val photographer = userRepository.getPhotographers().find { it.userId == row[PhotographyRequests.photographerId] }
                PhotographyRequestDto(
                    id = row[PhotographyRequests.id],
                    photographerId = row[PhotographyRequests.photographerId],
                    photographerName = photographer?.displayName,
                    requesterId = row[PhotographyRequests.requesterId],
                    requesterName = null,
                    petId = row[PhotographyRequests.petId],
                    petName = null,
                    message = row[PhotographyRequests.message],
                    status = row[PhotographyRequests.status],
                    scheduledDate = row[PhotographyRequests.scheduledDate],
                    createdAt = row[PhotographyRequests.createdAt]
                )
            }
    }

    override fun getRequestsForPhotographer(photographerId: Int): List<PhotographyRequestDto> = transaction {
        PhotographyRequests.selectAll()
            .where { PhotographyRequests.photographerId eq photographerId }
            .map { row ->
                val requester = userRepository.getById(row[PhotographyRequests.requesterId])
                val pet = if (row[PhotographyRequests.petId] != null) petRepository.getById(row[PhotographyRequests.petId]!!) else null
                PhotographyRequestDto(
                    id = row[PhotographyRequests.id],
                    photographerId = row[PhotographyRequests.photographerId],
                    photographerName = null,
                    requesterId = row[PhotographyRequests.requesterId],
                    requesterName = requester?.displayName,
                    petId = row[PhotographyRequests.petId],
                    petName = pet?.name,
                    message = row[PhotographyRequests.message],
                    status = row[PhotographyRequests.status],
                    scheduledDate = row[PhotographyRequests.scheduledDate],
                    createdAt = row[PhotographyRequests.createdAt]
                )
            }
    }

    override fun getRequestById(requestId: Int): PhotographyRequestDto? = transaction {
        val row = PhotographyRequests.selectAll()
            .where { PhotographyRequests.id eq requestId }
            .firstOrNull() ?: return@transaction null

        val photographer = userRepository.getPhotographers().find { it.userId == row[PhotographyRequests.photographerId] }
        val requester = userRepository.getById(row[PhotographyRequests.requesterId])
        val pet = if (row[PhotographyRequests.petId] != null) petRepository.getById(row[PhotographyRequests.petId]!!) else null

        PhotographyRequestDto(
            id = row[PhotographyRequests.id],
            photographerId = row[PhotographyRequests.photographerId],
            photographerName = photographer?.displayName,
            requesterId = row[PhotographyRequests.requesterId],
            requesterName = requester?.displayName,
            petId = row[PhotographyRequests.petId],
            petName = pet?.name,
            message = row[PhotographyRequests.message],
            status = row[PhotographyRequests.status],
            scheduledDate = row[PhotographyRequests.scheduledDate],
            createdAt = row[PhotographyRequests.createdAt]
        )
    }

    override fun updatePhotographyRequest(requestId: Int, status: String?, scheduledDate: Long?) {
        transaction {
            PhotographyRequests.update({ PhotographyRequests.id eq requestId }) {
                if (status != null) it[PhotographyRequests.status] = status
                if (scheduledDate != null) it[PhotographyRequests.scheduledDate] = scheduledDate
            }
        }
    }

    override fun getPhotographerById(userId: Int): PhotographerDto? {
        val hasPhotographerRole = transaction {
            UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name) }
                .count() > 0
        }
        if (!hasPhotographerRole) return null
        
        return transaction {
            val user = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            val photographer = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            if (user != null) {
                PhotographerDto(
                    userId = userId,
                    displayName = user[Users.displayName],
                    username = user[Users.username],
                    photographerFee = photographer?.get(Photographers.photographerFee)?.toDouble(),
                    photographerCurrency = photographer?.get(Photographers.photographerCurrency),
                    country = photographer?.get(Photographers.country),
                    state = photographer?.get(Photographers.state)
                )
            } else null
        }
    }

    override fun getPhotographers(country: String?, state: String?): List<PhotographerDto> = transaction {
        val photographerUserIds = UserActiveRoles.selectAll()
            .where { UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name }
            .map { row -> row[UserActiveRoles.userId] }
            .distinct()
        
        photographerUserIds.mapNotNull { userId ->
            val user = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            val photographer = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            
            if (user != null) {
                val photographerCountry = photographer?.get(Photographers.country)
                val photographerState = photographer?.get(Photographers.state)
                
                val matchesCountry = country.isNullOrBlank() || photographerCountry == country
                val matchesState = state.isNullOrBlank() || photographerState == state
                
                if (matchesCountry && matchesState) {
                    PhotographerDto(
                        userId = userId,
                        displayName = user[Users.displayName],
                        username = user[Users.username],
                        photographerFee = photographer?.get(Photographers.photographerFee)?.toDouble(),
                        photographerCurrency = photographer?.get(Photographers.photographerCurrency),
                        country = photographerCountry,
                        state = photographerState
                    )
                } else null
            } else null
        }
    }

    override fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto? {
        if (request.photographerFee < 0) {
            throw IllegalArgumentException("Photographer fee must be zero or positive")
        }
        
        val userExists = transaction {
            Users.selectAll().where { Users.id eq userId }.firstOrNull() != null
        }
        if (!userExists) return null
        
        transaction {
            val existing = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            if (existing != null) {
                Photographers.update({ Photographers.userId eq userId }) {
                    it[photographerFee] = BigDecimal.valueOf(request.photographerFee)
                    it[photographerCurrency] = request.photographerCurrency
                    it[country] = request.country
                    it[state] = request.state
                }
            } else {
                Photographers.insert {
                    it[Photographers.userId] = userId
                    it[photographerFee] = BigDecimal.valueOf(request.photographerFee)
                    it[photographerCurrency] = request.photographerCurrency
                    it[country] = request.country
                    it[state] = request.state
                }
            }
        }
        
        return getPhotographerById(userId)
    }

    override fun activatePhotographerProfile(userId: Int): UserDto? {
        val user = userRepository.getById(userId) ?: return null
        
        transaction {
            val existingRole = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name) }
                .firstOrNull()
            if (existingRole == null) {
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = userId
                    it[UserActiveRoles.role] = UserRole.PHOTOGRAPHER.name
                }
            }
            val existingPhotographer = Photographers.selectAll()
                .where { Photographers.userId eq userId }
                .firstOrNull()
            if (existingPhotographer == null) {
                Photographers.insert {
                    it[Photographers.userId] = userId
                    it[country] = null
                    it[state] = null
                }
            }
        }
        return userRepository.getById(userId)
    }

    override fun deactivatePhotographerProfile(userId: Int): UserDto? {
        transaction {
            UserActiveRoles.deleteWhere {
                (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name)
            }
        }
        return userRepository.getById(userId)
    }
}
