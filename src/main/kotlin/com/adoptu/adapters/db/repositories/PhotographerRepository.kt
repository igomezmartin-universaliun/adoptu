package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.PhotographyRequests
import com.adoptu.adapters.db.Photographers
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.dto.PhotographyRequestDto
import com.adoptu.dto.PhotographerDto
import com.adoptu.dto.UserRole
import com.adoptu.ports.PhotographerRepositoryPort
import com.adoptu.ports.PetRepositoryPort
import com.adoptu.ports.UserRepositoryPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class PhotographerRepositoryImpl(
    private val petRepository: PetRepositoryPort,
    private val userRepository: UserRepositoryPort
) : PhotographerRepositoryPort {

    companion object {
        const val ONE_WEEK = 7 * 24 * 60 * 60 * 1000L
    }

    override fun canSendMessage(userId: Int): Boolean {
        val oneWeekAgo = System.currentTimeMillis() - ONE_WEEK
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
        val createdAt = System.currentTimeMillis()
        val requestId = PhotographyRequests.insert {
            it[PhotographyRequests.photographerId] = photographerId
            it[PhotographyRequests.requesterId] = requesterId
            it[PhotographyRequests.petId] = petId
            it[PhotographyRequests.message] = message
            it[PhotographyRequests.status] = "PENDING"
            it[PhotographyRequests.createdAt] = createdAt
        } get PhotographyRequests.id

        requestId!!
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
}
