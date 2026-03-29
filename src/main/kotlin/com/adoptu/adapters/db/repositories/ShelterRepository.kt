package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.AnimalShelters
import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.ShelterDto
import com.adoptu.dto.input.UpdateShelterRequest
import com.adoptu.ports.ShelterRepositoryPort
import org.jetbrains.exposed.v1.core.ResultRow
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
class ShelterRepository(private val clock: Clock) : ShelterRepositoryPort {

    private fun rowToDto(row: ResultRow): ShelterDto {
        return ShelterDto(
            id = row[AnimalShelters.id],
            name = row[AnimalShelters.name],
            country = row[AnimalShelters.country],
            state = row[AnimalShelters.state],
            city = row[AnimalShelters.city],
            address = row[AnimalShelters.address],
            zip = row[AnimalShelters.zip],
            phone = row[AnimalShelters.phone],
            email = row[AnimalShelters.email],
            website = row[AnimalShelters.website],
            fiscalId = row[AnimalShelters.fiscalId],
            bankName = row[AnimalShelters.bankName],
            accountHolderName = row[AnimalShelters.accountHolderName],
            accountNumber = row[AnimalShelters.accountNumber],
            iban = row[AnimalShelters.iban],
            swiftBic = row[AnimalShelters.swiftBic],
            currency = row[AnimalShelters.currency],
            description = row[AnimalShelters.description],
            createdAt = row[AnimalShelters.createdAt],
            updatedAt = row[AnimalShelters.updatedAt]
        )
    }

    override fun getById(id: Int): ShelterDto? = transaction {
        AnimalShelters.selectAll()
            .where { AnimalShelters.id eq id }
            .firstOrNull()
            ?.let { rowToDto(it) }
    }

    override fun getAll(country: String, state: String?): List<ShelterDto> = transaction {
        val query = if (state.isNullOrBlank()) {
            AnimalShelters.selectAll().where { AnimalShelters.country eq country }
        } else {
            AnimalShelters.selectAll().where { (AnimalShelters.country eq country) and (AnimalShelters.state eq state) }
        }
        query.map { rowToDto(it) }
    }

    override fun create(request: CreateShelterRequest): ShelterDto {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            val id = AnimalShelters.insert {
                it[name] = request.name
                it[country] = request.country
                it[state] = request.state
                it[city] = request.city
                it[address] = request.address
                it[zip] = request.zip
                it[phone] = request.phone
                it[email] = request.email
                it[website] = request.website
                it[fiscalId] = request.fiscalId
                it[bankName] = request.bankName
                it[accountHolderName] = request.accountHolderName
                it[accountNumber] = request.accountNumber
                it[iban] = request.iban
                it[swiftBic] = request.swiftBic
                it[currency] = request.currency
                it[description] = request.description
                it[createdAt] = now
                it[updatedAt] = now
            } get AnimalShelters.id

            getById(id)!!
        }
    }

    override fun update(id: Int, request: UpdateShelterRequest): ShelterDto? {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            val existing = AnimalShelters.selectAll().where { AnimalShelters.id eq id }.firstOrNull()
                ?: return@transaction null

            AnimalShelters.update({ AnimalShelters.id eq id }) { row ->
                request.name?.let { row[name] = it }
                request.country?.let { row[country] = it }
                request.state?.let { row[state] = it }
                request.city?.let { row[city] = it }
                request.address?.let { row[address] = it }
                request.zip?.let { row[zip] = it }
                request.phone?.let { row[phone] = it }
                request.email?.let { row[email] = it }
                request.website?.let { row[website] = it }
                request.fiscalId?.let { row[fiscalId] = it }
                request.bankName?.let { row[bankName] = it }
                request.accountHolderName?.let { row[accountHolderName] = it }
                request.accountNumber?.let { row[accountNumber] = it }
                request.iban?.let { row[iban] = it }
                request.swiftBic?.let { row[swiftBic] = it }
                request.currency?.let { row[currency] = it }
                request.description?.let { row[description] = it }
                row[updatedAt] = now
            }

            getById(id)
        }
    }

    override fun delete(id: Int): Boolean = transaction {
        val rowsDeleted = AnimalShelters.deleteWhere { AnimalShelters.id eq id }
        rowsDeleted > 0
    }

    override fun getCountries(): List<String> = transaction {
        AnimalShelters.selectAll()
            .map { it[AnimalShelters.country] }
            .distinct()
            .sorted()
    }

    override fun getStatesByCountry(country: String): List<String> = transaction {
        AnimalShelters.selectAll()
            .where { AnimalShelters.country eq country }
            .mapNotNull { it[AnimalShelters.state] }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
}
