package com.adoptu.test

import com.adoptu.adapters.db.SterilizationLocations
import com.adoptu.adapters.db.repositories.SterilizationLocationRepository
import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SterilizationLocationRepositoryIT {

    private val clock = Clock.System
    private lateinit var repository: SterilizationLocationRepository

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        repository = SterilizationLocationRepository(clock)
    }

    @AfterEach
    fun cleanup() {
        transaction {
            SterilizationLocations.deleteWhere { SterilizationLocations.id greater 0 }
        }
    }

    @Test
    fun `create and get sterilization location`() {
        val request = CreateSterilizationLocationRequest(
            name = "Test Vet Clinic",
            country = "USA",
            state = "California",
            city = "Los Angeles",
            address = "123 Main St",
            zip = "90001",
            phone = "+1-555-1234",
            email = "test@vetclinic.com",
            website = "https://vetclinic.com",
            description = "A test veterinary clinic"
        )

        val created = repository.create(request)
        assertNotNull(created.id)
        assertEquals("Test Vet Clinic", created.name)
        assertEquals("USA", created.country)
        assertEquals("California", created.state)
        assertEquals("Los Angeles", created.city)
        assertEquals("123 Main St", created.address)

        val retrieved = repository.getById(created.id)
        assertNotNull(retrieved)
        assertEquals(created.name, retrieved.name)
    }

    @Test
    fun `get all sterilization locations`() {
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 1",
            country = "USA",
            state = "California",
            city = "Los Angeles",
            address = "123 Main St"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 2",
            country = "USA",
            state = "California",
            city = "San Francisco",
            address = "456 Oak Ave"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 3",
            country = "Mexico",
            state = null,
            city = "Mexico City",
            address = "789 Av. Principal"
        ))

        val allLocations = repository.getAll()
        assertEquals(3, allLocations.size)

        val usLocations = repository.getAll(country = "USA")
        assertEquals(2, usLocations.size)

        val caLocations = repository.getAll(country = "USA", state = "California")
        assertEquals(2, caLocations.size)

        val laLocations = repository.getAll(country = "USA", state = "California", city = "Los Angeles")
        assertEquals(1, laLocations.size)
        assertEquals("Vet 1", laLocations.first().name)
    }

    @Test
    fun `update sterilization location`() {
        val created = repository.create(CreateSterilizationLocationRequest(
            name = "Original Name",
            country = "USA",
            city = "Los Angeles",
            address = "123 Main St"
        ))

        val updateRequest = UpdateSterilizationLocationRequest(
            name = "Updated Name",
            state = "California"
        )

        val updated = repository.update(created.id, updateRequest)
        assertNotNull(updated)
        assertEquals("Updated Name", updated.name)
        assertEquals("California", updated.state)
        assertEquals("USA", updated.country)
    }

    @Test
    fun `delete sterilization location`() {
        val created = repository.create(CreateSterilizationLocationRequest(
            name = "To Delete",
            country = "USA",
            city = "Los Angeles",
            address = "123 Main St"
        ))

        val deleted = repository.delete(created.id)
        assertTrue(deleted)

        val retrieved = repository.getById(created.id)
        assertNull(retrieved)
    }

    @Test
    fun `get countries`() {
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 1",
            country = "USA",
            city = "Los Angeles",
            address = "123 Main St"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 2",
            country = "USA",
            city = "New York",
            address = "456 Broadway"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 3",
            country = "Mexico",
            city = "Mexico City",
            address = "789 Av. Principal"
        ))

        val countries = repository.getCountries()
        assertEquals(2, countries.size)
        assertTrue(countries.contains("USA"))
        assertTrue(countries.contains("Mexico"))
    }

    @Test
    fun `get states by country`() {
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 1",
            country = "USA",
            state = "California",
            city = "Los Angeles",
            address = "123 Main St"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 2",
            country = "USA",
            state = "California",
            city = "San Francisco",
            address = "456 Oak Ave"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 3",
            country = "USA",
            state = "New York",
            city = "New York City",
            address = "789 Broadway"
        ))

        val states = repository.getStatesByCountry("USA")
        assertEquals(2, states.size)
        assertTrue(states.contains("California"))
        assertTrue(states.contains("New York"))
    }

    @Test
    fun `get grouped by location`() {
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 1",
            country = "USA",
            state = "California",
            city = "Los Angeles",
            address = "123 Main St"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 2",
            country = "USA",
            state = "California",
            city = "San Francisco",
            address = "456 Oak Ave"
        ))
        repository.create(CreateSterilizationLocationRequest(
            name = "Vet 3",
            country = "Mexico",
            state = null,
            city = "Mexico City",
            address = "789 Av. Principal"
        ))

        val grouped = repository.getGroupedByLocation()
        assertEquals(2, grouped.size)
        
        val usaGroup = grouped.find { it.country == "USA" }
        assertNotNull(usaGroup)
        assertEquals(1, usaGroup.states.size)
        assertEquals("California", usaGroup.states.first().state)
        assertEquals(2, usaGroup.states.first().cities.size)
        
        val mexicoGroup = grouped.find { it.country == "Mexico" }
        assertNotNull(mexicoGroup)
        assertEquals(1, mexicoGroup.states.size)
        assertNull(mexicoGroup.states.first().state)
    }
}
