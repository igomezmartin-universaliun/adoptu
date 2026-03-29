package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.SterilizationLocations
import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.SterilizationLocationDto
import com.adoptu.dto.input.SterilizationLocationsByCity
import com.adoptu.dto.input.SterilizationLocationsByLocation
import com.adoptu.dto.input.SterilizationLocationsByState
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.ports.SterilizationLocationRepositoryPort
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
class SterilizationLocationRepository(private val clock: Clock) : SterilizationLocationRepositoryPort {

    private fun rowToDto(row: ResultRow): SterilizationLocationDto {
        return SterilizationLocationDto(
            id = row[SterilizationLocations.id],
            name = row[SterilizationLocations.name],
            country = row[SterilizationLocations.country],
            state = row[SterilizationLocations.state],
            city = row[SterilizationLocations.city],
            address = row[SterilizationLocations.address],
            zip = row[SterilizationLocations.zip],
            phone = row[SterilizationLocations.phone],
            email = row[SterilizationLocations.email],
            website = row[SterilizationLocations.website],
            description = row[SterilizationLocations.description],
            createdAt = row[SterilizationLocations.createdAt],
            updatedAt = row[SterilizationLocations.updatedAt]
        )
    }

    override fun getById(id: Int): SterilizationLocationDto? = transaction {
        SterilizationLocations.selectAll()
            .where { SterilizationLocations.id eq id }
            .firstOrNull()
            ?.let { rowToDto(it) }
    }

    override fun getAll(country: String?, state: String?, city: String?): List<SterilizationLocationDto> = transaction {
        val query = if (country.isNullOrBlank() && state.isNullOrBlank() && city.isNullOrBlank()) {
            SterilizationLocations.selectAll()
        } else if (state.isNullOrBlank() && city.isNullOrBlank()) {
            SterilizationLocations.selectAll().where { SterilizationLocations.country eq country!! }
        } else if (city.isNullOrBlank()) {
            SterilizationLocations.selectAll().where { (SterilizationLocations.country eq country!!) and (SterilizationLocations.state eq state!!) }
        } else {
            SterilizationLocations.selectAll().where { 
                (SterilizationLocations.country eq country!!) and 
                (SterilizationLocations.state eq state!!) and 
                (SterilizationLocations.city eq city!!) 
            }
        }
        query.map { rowToDto(it) }
    }

    override fun create(request: CreateSterilizationLocationRequest): SterilizationLocationDto {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            val id = SterilizationLocations.insert {
                it[name] = request.name
                it[country] = request.country
                it[state] = request.state
                it[city] = request.city
                it[address] = request.address
                it[zip] = request.zip
                it[phone] = request.phone
                it[email] = request.email
                it[website] = request.website
                it[description] = request.description
                it[createdAt] = now
                it[updatedAt] = now
            } get SterilizationLocations.id

            getById(id)!!
        }
    }

    override fun update(id: Int, request: UpdateSterilizationLocationRequest): SterilizationLocationDto? {
        val now = clock.now().toEpochMilliseconds()
        return transaction {
            val existing = SterilizationLocations.selectAll().where { SterilizationLocations.id eq id }.firstOrNull()
                ?: return@transaction null

            SterilizationLocations.update({ SterilizationLocations.id eq id }) { row ->
                request.name?.let { row[name] = it }
                request.country?.let { row[country] = it }
                request.state?.let { row[state] = it }
                request.city?.let { row[city] = it }
                request.address?.let { row[address] = it }
                request.zip?.let { row[zip] = it }
                request.phone?.let { row[phone] = it }
                request.email?.let { row[email] = it }
                request.website?.let { row[website] = it }
                request.description?.let { row[description] = it }
                row[updatedAt] = now
            }

            getById(id)
        }
    }

    override fun delete(id: Int): Boolean = transaction {
        val rowsDeleted = SterilizationLocations.deleteWhere { SterilizationLocations.id eq id }
        rowsDeleted > 0
    }

    override fun getCountries(): List<String> = transaction {
        SterilizationLocations.selectAll()
            .map { it[SterilizationLocations.country] }
            .distinct()
            .sorted()
    }

    override fun getStatesByCountry(country: String): List<String> = transaction {
        SterilizationLocations.selectAll()
            .where { SterilizationLocations.country eq country }
            .mapNotNull { it[SterilizationLocations.state] }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    override fun getCitiesByCountryAndState(country: String, state: String?): List<String> = transaction {
        if (!state.isNullOrBlank()) {
            SterilizationLocations.selectAll()
                .where { (SterilizationLocations.country eq country) and (SterilizationLocations.state eq state) }
                .map { it[SterilizationLocations.city] }
                .distinct()
                .sorted()
        } else {
            SterilizationLocations.selectAll()
                .where { SterilizationLocations.country eq country }
                .map { it[SterilizationLocations.city] }
                .distinct()
                .sorted()
        }
    }

    override fun getGroupedByLocation(): List<SterilizationLocationsByLocation> = transaction {
        val allLocations = SterilizationLocations.selectAll().map { rowToDto(it) }
        
        allLocations.groupBy { it.country }
            .map { (country, locations) ->
                val statesData = locations.groupBy { it.state }
                    .map { (state, stateLocations) ->
                        val citiesData = stateLocations.groupBy { it.city }
                            .map { (city, cityLocations) ->
                                SterilizationLocationsByCity(city, cityLocations)
                            }
                            .sortedBy { it.city }
                        SterilizationLocationsByState(state, citiesData)
                    }
                    .sortedBy { it.state ?: "" }
                SterilizationLocationsByLocation(country, statesData)
            }
            .sortedBy { it.country }
    }
}
