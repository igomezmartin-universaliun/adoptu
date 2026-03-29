package com.adoptu.services

import com.adoptu.adapters.db.repositories.SterilizationLocationRepository
import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class SterilizationLocationServiceTest {

    private lateinit var service: SterilizationLocationService
    private val clock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        val repository = SterilizationLocationRepository(clock)
        service = SterilizationLocationService(repository)
    }

    private fun cleanup() {
        transaction {
            com.adoptu.adapters.db.SterilizationLocations.deleteWhere { com.adoptu.adapters.db.SterilizationLocations.id greater 0 }
        }
    }

    @Test
    fun `getAll returns empty list when no locations exist`() {
        val result = service.getAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll returns all locations`() {
        createLocation(name = "Location 1", country = "USA", city = "LA", address = "123 Main St")
        createLocation(name = "Location 2", country = "USA", city = "NY", address = "456 Broadway")

        val result = service.getAll()

        assertEquals(2, result.size)
    }

    @Test
    fun `getAll filters by country`() {
        createLocation(name = "US Location", country = "USA", city = "LA", address = "123 Main St")
        createLocation(name = "Mexico Location", country = "Mexico", city = "Mexico City", address = "789 Av Principal")

        val result = service.getAll(country = "USA")

        assertEquals(1, result.size)
        assertEquals("US Location", result.first().name)
    }

    @Test
    fun `getAll filters by country and state`() {
        createLocation(name = "CA Location", country = "USA", state = "California", city = "LA", address = "123 Main St")
        createLocation(name = "NY Location", country = "USA", state = "New York", city = "NY", address = "456 Broadway")

        val result = service.getAll(country = "USA", state = "California")

        assertEquals(1, result.size)
        assertEquals("CA Location", result.first().name)
    }

    @Test
    fun `getAll filters by country state and city`() {
        createLocation(name = "LA Location", country = "USA", state = "California", city = "Los Angeles", address = "123 Main St")
        createLocation(name = "SF Location", country = "USA", state = "California", city = "San Francisco", address = "456 Oak Ave")

        val result = service.getAll(country = "USA", state = "California", city = "Los Angeles")

        assertEquals(1, result.size)
        assertEquals("LA Location", result.first().name)
    }

    @Test
    fun `getById returns null for non-existent location`() {
        val result = service.getById(999)
        assertNull(result)
    }

    @Test
    fun `getById returns location by id`() {
        val id = createLocation(name = "Test Location", country = "USA", city = "LA", address = "123 Main St")

        val result = service.getById(id)

        assertNotNull(result)
        assertEquals("Test Location", result.name)
    }

    @Test
    fun `create creates location successfully`() {
        val request = CreateSterilizationLocationRequest(
            name = "New Vet Clinic",
            country = "USA",
            state = "California",
            city = "Los Angeles",
            address = "123 Vet Street",
            zip = "90001",
            phone = "+1-555-1234",
            email = "vet@clinic.com",
            website = "https://vet.com",
            description = "A great veterinary clinic"
        )

        val result = service.create(request)

        assertNotNull(result)
        assertEquals("New Vet Clinic", result.name)
        assertEquals("USA", result.country)
        assertEquals("California", result.state)
        assertEquals("Los Angeles", result.city)
        assertEquals("123 Vet Street", result.address)
        assertEquals("+1-555-1234", result.phone)
        assertEquals("vet@clinic.com", result.email)
    }

    @Test
    fun `create throws exception when name is blank`() {
        val request = CreateSterilizationLocationRequest(
            name = "",
            country = "USA",
            city = "LA",
            address = "123 Main St"
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.create(request)
        }

        assertEquals("Name is required", exception.message)
    }

    @Test
    fun `create throws exception when country is blank`() {
        val request = CreateSterilizationLocationRequest(
            name = "Test",
            country = "",
            city = "LA",
            address = "123 Main St"
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.create(request)
        }

        assertEquals("Country is required", exception.message)
    }

    @Test
    fun `create throws exception when city is blank`() {
        val request = CreateSterilizationLocationRequest(
            name = "Test",
            country = "USA",
            city = "",
            address = "123 Main St"
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.create(request)
        }

        assertEquals("City is required", exception.message)
    }

    @Test
    fun `create throws exception when address is blank`() {
        val request = CreateSterilizationLocationRequest(
            name = "Test",
            country = "USA",
            city = "LA",
            address = ""
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.create(request)
        }

        assertEquals("Address is required", exception.message)
    }

    @Test
    fun `update returns NotFound for non-existent location`() {
        val request = UpdateSterilizationLocationRequest(name = "Updated")

        val result = service.update(999, request)

        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `update updates location successfully`() {
        val id = createLocation(name = "Original Name", country = "USA", city = "LA", address = "123 Main St")
        val request = UpdateSterilizationLocationRequest(
            name = "Updated Name",
            state = "California"
        )

        val result = service.update(id, request)

        assertTrue(result is ServiceResult.Success)
        val updated = (result as ServiceResult.Success).data
        assertEquals("Updated Name", updated.name)
        assertEquals("California", updated.state)
        assertEquals("USA", updated.country)
    }

    @Test
    fun `update allows partial updates`() {
        val id = createLocation(
            name = "Original",
            country = "USA",
            state = "California",
            city = "LA",
            address = "123 Main St"
        )
        val request = UpdateSterilizationLocationRequest(phone = "+1-555-9999")

        val result = service.update(id, request)

        assertTrue(result is ServiceResult.Success)
        val updated = (result as ServiceResult.Success).data
        assertEquals("Original", updated.name)
        assertEquals("+1-555-9999", updated.phone)
    }

    @Test
    fun `delete returns NotFound for non-existent location`() {
        val result = service.delete(999)
        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `delete removes location successfully`() {
        val id = createLocation(name = "To Delete", country = "USA", city = "LA", address = "123 Main St")

        val result = service.delete(id)

        assertTrue(result is ServiceResult.Success)
        assertNull(service.getById(id))
    }

    @Test
    fun `getCountries returns list of countries`() {
        createLocation(name = "US 1", country = "USA", city = "LA", address = "123 Main St")
        createLocation(name = "US 2", country = "USA", city = "NY", address = "456 Broadway")
        createLocation(name = "MX 1", country = "Mexico", city = "Mexico City", address = "789 Av")

        val result = service.getCountries()

        assertEquals(2, result.size)
        assertTrue(result.contains("USA"))
        assertTrue(result.contains("Mexico"))
    }

    @Test
    fun `getStatesByCountry returns list of states`() {
        createLocation(name = "CA 1", country = "USA", state = "California", city = "LA", address = "123 Main St")
        createLocation(name = "CA 2", country = "USA", state = "California", city = "SF", address = "456 Oak")
        createLocation(name = "NY 1", country = "USA", state = "New York", city = "NY", address = "789 Broadway")

        val result = service.getStatesByCountry("USA")

        assertEquals(2, result.size)
        assertTrue(result.contains("California"))
        assertTrue(result.contains("New York"))
    }

    @Test
    fun `getCitiesByCountryAndState returns list of cities`() {
        createLocation(name = "LA 1", country = "USA", state = "California", city = "Los Angeles", address = "123 Main St")
        createLocation(name = "LA 2", country = "USA", state = "California", city = "Los Angeles", address = "456 Oak")
        createLocation(name = "SF 1", country = "USA", state = "California", city = "San Francisco", address = "789 Pine")

        val result = service.getCitiesByCountryAndState("USA", "California")

        assertEquals(2, result.size)
        assertTrue(result.contains("Los Angeles"))
        assertTrue(result.contains("San Francisco"))
    }

    @Test
    fun `getCitiesByCountryAndState returns cities without state filter`() {
        createLocation(name = "CA City", country = "USA", state = "California", city = "LA", address = "123 Main St")
        createLocation(name = "No State", country = "USA", state = null, city = "Unknown", address = "456 Oak")

        val result = service.getCitiesByCountryAndState("USA", null)

        assertEquals(2, result.size)
        assertTrue(result.contains("LA"))
        assertTrue(result.contains("Unknown"))
    }

    @Test
    fun `getGroupedByLocation returns locations grouped by country state and city`() {
        createLocation(name = "LA Vet 1", country = "USA", state = "California", city = "Los Angeles", address = "123 Main St")
        createLocation(name = "LA Vet 2", country = "USA", state = "California", city = "Los Angeles", address = "456 Oak")
        createLocation(name = "SF Vet 1", country = "USA", state = "California", city = "San Francisco", address = "789 Pine")
        createLocation(name = "Mexico Vet", country = "Mexico", state = null, city = "Mexico City", address = "Av Principal")

        val result = service.getGroupedByLocation()

        assertEquals(2, result.size)
        
        val usaGroup = result.find { it.country == "USA" }
        assertNotNull(usaGroup)
        assertEquals(1, usaGroup.states.size)
        
        val caState = usaGroup.states.first()
        assertEquals("California", caState.state)
        assertEquals(2, caState.cities.size)
        
        val laCities = caState.cities.find { it.city == "Los Angeles" }
        assertNotNull(laCities)
        assertEquals(2, laCities.locations.size)
        
        val mexicoGroup = result.find { it.country == "Mexico" }
        assertNotNull(mexicoGroup)
        assertEquals(1, mexicoGroup.states.size)
        assertNull(mexicoGroup.states.first().state)
    }

    private fun createLocation(
        name: String,
        country: String,
        city: String,
        address: String,
        state: String? = null,
        zip: String? = null,
        phone: String? = null,
        email: String? = null,
        website: String? = null,
        description: String? = null
    ): Int {
        return service.create(
            CreateSterilizationLocationRequest(
                name = name,
                country = country,
                state = state,
                city = city,
                address = address,
                zip = zip,
                phone = phone,
                email = email,
                website = website,
                description = description
            )
        ).id
    }
}
