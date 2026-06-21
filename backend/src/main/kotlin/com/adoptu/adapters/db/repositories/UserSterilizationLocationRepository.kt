package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.UserSterilizationLocations
import com.adoptu.dto.input.CreateUserSterilizationLocationRequest
import com.adoptu.dto.input.UpdateUserSterilizationLocationRequest
import com.adoptu.dto.input.UserSterilizationLocationDto
import com.adoptu.ports.UserSterilizationLocationRepositoryPort
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UserSterilizationLocationRepository(private val clock: Clock) : UserSterilizationLocationRepositoryPort {

    private fun rowToDto(row: org.jetbrains.exposed.v1.core.ResultRow): UserSterilizationLocationDto {
        return UserSterilizationLocationDto(
            userId = row[UserSterilizationLocations.userId],
            name = row[UserSterilizationLocations.name],
            country = row[UserSterilizationLocations.country],
            state = row[UserSterilizationLocations.state],
            city = row[UserSterilizationLocations.city],
            neighborhood = row[UserSterilizationLocations.neighborhood],
            address = row[UserSterilizationLocations.address],
            zip = row[UserSterilizationLocations.zip],
            phone = row[UserSterilizationLocations.phone],
            email = row[UserSterilizationLocations.email],
            website = row[UserSterilizationLocations.website],
            description = row[UserSterilizationLocations.description],
            createdAt = row[UserSterilizationLocations.createdAt]
        )
    }

    override fun getByUserId(userId: Int): UserSterilizationLocationDto? = transaction {
        UserSterilizationLocations.selectAll()
            .where { UserSterilizationLocations.userId eq userId }
            .firstOrNull()
            ?.let { rowToDto(it) }
    }

    override fun create(userId: Int, request: CreateUserSterilizationLocationRequest): UserSterilizationLocationDto {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            UserSterilizationLocations.insert {
                it[UserSterilizationLocations.userId] = userId
                it[UserSterilizationLocations.name] = request.name
                it[UserSterilizationLocations.country] = request.country
                it[UserSterilizationLocations.state] = request.state
                it[UserSterilizationLocations.city] = request.city
                it[UserSterilizationLocations.neighborhood] = request.neighborhood
                it[UserSterilizationLocations.address] = request.address
                it[UserSterilizationLocations.zip] = request.zip
                it[UserSterilizationLocations.phone] = request.phone
                it[UserSterilizationLocations.email] = request.email
                it[UserSterilizationLocations.website] = request.website
                it[UserSterilizationLocations.description] = request.description
                it[UserSterilizationLocations.createdAt] = now
                it[UserSterilizationLocations.updatedAt] = now
            }

            UserSterilizationLocationDto(
                userId = userId,
                name = request.name,
                country = request.country,
                state = request.state,
                city = request.city,
                neighborhood = request.neighborhood,
                address = request.address,
                zip = request.zip,
                phone = request.phone,
                email = request.email,
                website = request.website,
                description = request.description,
                createdAt = now
            )
        }
    }

    override fun update(userId: Int, request: UpdateUserSterilizationLocationRequest): UserSterilizationLocationDto? {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            val existing = UserSterilizationLocations.selectAll().where { UserSterilizationLocations.userId eq userId }.firstOrNull()
                ?: return@transaction null

            UserSterilizationLocations.update({ UserSterilizationLocations.userId eq userId }) { row ->
                request.name?.let { row[UserSterilizationLocations.name] = it }
                request.country?.let { row[UserSterilizationLocations.country] = it }
                request.state?.let { row[UserSterilizationLocations.state] = it }
                request.city?.let { row[UserSterilizationLocations.city] = it }
                request.neighborhood?.let { row[UserSterilizationLocations.neighborhood] = it }
                request.address?.let { row[UserSterilizationLocations.address] = it }
                request.zip?.let { row[UserSterilizationLocations.zip] = it }
                request.phone?.let { row[UserSterilizationLocations.phone] = it }
                request.email?.let { row[UserSterilizationLocations.email] = it }
                request.website?.let { row[UserSterilizationLocations.website] = it }
                request.description?.let { row[UserSterilizationLocations.description] = it }
                row[UserSterilizationLocations.updatedAt] = now
            }

            getByUserId(userId)
        }
    }

    override fun delete(userId: Int): Boolean = transaction {
        val rowsDeleted = UserSterilizationLocations.deleteWhere { UserSterilizationLocations.userId eq userId }
        rowsDeleted > 0
    }

    override fun search(country: String, state: String?, city: String?, neighborhood: String?, zip: String?): List<UserSterilizationLocationDto> = transaction {
        var conditions: org.jetbrains.exposed.v1.core.Op<Boolean> = UserSterilizationLocations.country eq country
        
        if (!state.isNullOrBlank()) {
            conditions = conditions.and(UserSterilizationLocations.state eq state)
        }
        if (!city.isNullOrBlank()) {
            conditions = conditions.and(UserSterilizationLocations.city eq city)
        }
        if (!neighborhood.isNullOrBlank()) {
            conditions = conditions.and(UserSterilizationLocations.neighborhood eq neighborhood)
        }
        if (!zip.isNullOrBlank()) {
            conditions = conditions.and(UserSterilizationLocations.zip eq zip)
        }

        UserSterilizationLocations.selectAll()
            .where { conditions }
            .map { rowToDto(it) }
    }
}