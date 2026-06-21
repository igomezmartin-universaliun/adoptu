package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.UserShelters
import com.adoptu.dto.input.CreateUserShelterRequest
import com.adoptu.dto.input.UpdateUserShelterRequest
import com.adoptu.dto.input.UserShelterDto
import com.adoptu.ports.UserShelterRepositoryPort
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
class UserShelterRepository(private val clock: Clock) : UserShelterRepositoryPort {

    private fun rowToDto(row: org.jetbrains.exposed.v1.core.ResultRow): UserShelterDto {
        return UserShelterDto(
            userId = row[UserShelters.userId],
            name = row[UserShelters.name],
            country = row[UserShelters.country],
            state = row[UserShelters.state],
            city = row[UserShelters.city],
            neighborhood = row[UserShelters.neighborhood],
            address = row[UserShelters.address],
            zip = row[UserShelters.zip],
            phone = row[UserShelters.phone],
            email = row[UserShelters.email],
            website = row[UserShelters.website],
            fiscalId = row[UserShelters.fiscalId],
            bankName = row[UserShelters.bankName],
            accountHolderName = row[UserShelters.accountHolderName],
            accountNumber = row[UserShelters.accountNumber],
            iban = row[UserShelters.iban],
            swiftBic = row[UserShelters.swiftBic],
            currency = row[UserShelters.currency],
            description = row[UserShelters.description],
            createdAt = row[UserShelters.createdAt]
        )
    }

    override fun getByUserId(userId: Int): UserShelterDto? = transaction {
        UserShelters.selectAll()
            .where { UserShelters.userId eq userId }
            .firstOrNull()
            ?.let { rowToDto(it) }
    }

    override fun create(userId: Int, request: CreateUserShelterRequest): UserShelterDto {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            UserShelters.insert {
                it[UserShelters.userId] = userId
                it[UserShelters.name] = request.name
                it[UserShelters.country] = request.country
                it[UserShelters.state] = request.state
                it[UserShelters.city] = request.city
                it[UserShelters.neighborhood] = request.neighborhood
                it[UserShelters.address] = request.address
                it[UserShelters.zip] = request.zip
                it[UserShelters.phone] = request.phone
                it[UserShelters.email] = request.email
                it[UserShelters.website] = request.website
                it[UserShelters.fiscalId] = request.fiscalId
                it[UserShelters.bankName] = request.bankName
                it[UserShelters.accountHolderName] = request.accountHolderName
                it[UserShelters.accountNumber] = request.accountNumber
                it[UserShelters.iban] = request.iban
                it[UserShelters.swiftBic] = request.swiftBic
                it[UserShelters.currency] = request.currency
                it[UserShelters.description] = request.description
                it[UserShelters.createdAt] = now
                it[UserShelters.updatedAt] = now
            }

            UserShelterDto(
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
                fiscalId = request.fiscalId,
                bankName = request.bankName,
                accountHolderName = request.accountHolderName,
                accountNumber = request.accountNumber,
                iban = request.iban,
                swiftBic = request.swiftBic,
                currency = request.currency,
                description = request.description,
                createdAt = now
            )
        }
    }

    override fun update(userId: Int, request: UpdateUserShelterRequest): UserShelterDto? {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            val existing = UserShelters.selectAll().where { UserShelters.userId eq userId }.firstOrNull()
                ?: return@transaction null

            UserShelters.update({ UserShelters.userId eq userId }) { row ->
                request.name?.let { row[UserShelters.name] = it }
                request.country?.let { row[UserShelters.country] = it }
                request.state?.let { row[UserShelters.state] = it }
                request.city?.let { row[UserShelters.city] = it }
                request.neighborhood?.let { row[UserShelters.neighborhood] = it }
                request.address?.let { row[UserShelters.address] = it }
                request.zip?.let { row[UserShelters.zip] = it }
                request.phone?.let { row[UserShelters.phone] = it }
                request.email?.let { row[UserShelters.email] = it }
                request.website?.let { row[UserShelters.website] = it }
                request.fiscalId?.let { row[UserShelters.fiscalId] = it }
                request.bankName?.let { row[UserShelters.bankName] = it }
                request.accountHolderName?.let { row[UserShelters.accountHolderName] = it }
                request.accountNumber?.let { row[UserShelters.accountNumber] = it }
                request.iban?.let { row[UserShelters.iban] = it }
                request.swiftBic?.let { row[UserShelters.swiftBic] = it }
                request.currency?.let { row[UserShelters.currency] = it }
                request.description?.let { row[UserShelters.description] = it }
                row[UserShelters.updatedAt] = now
            }

            getByUserId(userId)
        }
    }

    override fun delete(userId: Int): Boolean = transaction {
        val rowsDeleted = UserShelters.deleteWhere { UserShelters.userId eq userId }
        rowsDeleted > 0
    }

    override fun search(country: String, state: String?, city: String?, neighborhood: String?, zip: String?): List<UserShelterDto> = transaction {
        var conditions: org.jetbrains.exposed.v1.core.Op<Boolean> = UserShelters.country eq country
        
        if (!state.isNullOrBlank()) {
            conditions = conditions.and(UserShelters.state eq state)
        }
        if (!city.isNullOrBlank()) {
            conditions = conditions.and(UserShelters.city eq city)
        }
        if (!neighborhood.isNullOrBlank()) {
            conditions = conditions.and(UserShelters.neighborhood eq neighborhood)
        }
        if (!zip.isNullOrBlank()) {
            conditions = conditions.and(UserShelters.zip eq zip)
        }

        UserShelters.selectAll()
            .where { conditions }
            .map { rowToDto(it) }
    }
}