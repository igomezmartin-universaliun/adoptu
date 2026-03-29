package com.adoptu.services

import com.adoptu.adapters.db.AnimalShelters
import com.adoptu.adapters.db.repositories.ShelterRepository
import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.UpdateShelterRequest
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
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
class ShelterServiceTest {

    private lateinit var shelterService: ShelterService
    private val clock: TestClock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        val shelterRepository = ShelterRepository(clock)
        shelterService = ShelterService(shelterRepository)
    }

    @Test
    fun `getAll returns empty list when no shelters exist`() {
        val result = shelterService.getAll("USA")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll returns shelters for given country`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "Canada", "ON", "Toronto")

        val result = shelterService.getAll("USA")

        assertEquals(2, result.size)
        assertTrue(result.all { it.country == "USA" })
    }

    @Test
    fun `getAll filters by state when provided`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "USA", "NY", "Buffalo")

        val result = shelterService.getAll("USA", "NY")

        assertEquals(2, result.size)
        assertTrue(result.all { it.state == "NY" })
    }

    @Test
    fun `getAll returns empty list for non-existent country`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")

        val result = shelterService.getAll("France")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll throws exception when country is blank`() {
        assertThrows<IllegalArgumentException> {
            shelterService.getAll("")
        }
    }

    @Test
    fun `getAll throws exception when country is whitespace only`() {
        assertThrows<IllegalArgumentException> {
            shelterService.getAll("   ")
        }
    }

    @Test
    fun `getById returns shelter by id`() {
        val shelterId = createTestShelter("Test Shelter", "USA", "NY", "New York")

        val result = shelterService.getById(shelterId)

        assertNotNull(result)
        assertEquals("Test Shelter", result.name)
        assertEquals("USA", result.country)
        assertEquals("NY", result.state)
        assertEquals("New York", result.city)
    }

    @Test
    fun `getById returns null for non-existent id`() {
        val result = shelterService.getById(999)
        assertNull(result)
    }

    @Test
    fun `create creates new shelter with all fields`() {
        val request = CreateShelterRequest(
            name = "New Shelter",
            country = "USA",
            state = "CA",
            city = "Los Angeles",
            address = "123 Main St",
            zip = "90001",
            phone = "555-1234",
            email = "shelter@test.com",
            website = "https://test.com",
            fiscalId = "12-3456789",
            bankName = "Test Bank",
            accountHolderName = "Shelter Account",
            accountNumber = "123456789",
            iban = "US123456789",
            swiftBic = "TESTBIC",
            currency = "USD",
            description = "A test shelter"
        )

        val result = shelterService.create(request)

        assertNotNull(result)
        assertEquals("New Shelter", result.name)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
        assertEquals("Los Angeles", result.city)
        assertEquals("123 Main St", result.address)
        assertEquals("90001", result.zip)
        assertEquals("555-1234", result.phone)
        assertEquals("shelter@test.com", result.email)
        assertEquals("https://test.com", result.website)
        assertEquals("12-3456789", result.fiscalId)
        assertEquals("Test Bank", result.bankName)
        assertEquals("Shelter Account", result.accountHolderName)
        assertEquals("123456789", result.accountNumber)
        assertEquals("US123456789", result.iban)
        assertEquals("TESTBIC", result.swiftBic)
        assertEquals("USD", result.currency)
        assertEquals("A test shelter", result.description)
    }

    @Test
    fun `create creates shelter with minimal fields`() {
        val request = CreateShelterRequest(
            name = "Minimal Shelter",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val result = shelterService.create(request)

        assertNotNull(result)
        assertEquals("Minimal Shelter", result.name)
        assertEquals("USA", result.country)
        assertNull(result.state)
        assertEquals("NYC", result.city)
    }

    @Test
    fun `create throws exception when name is blank`() {
        val request = CreateShelterRequest(
            name = "",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        assertThrows<IllegalArgumentException> {
            shelterService.create(request)
        }
    }

    @Test
    fun `create throws exception when country is blank`() {
        val request = CreateShelterRequest(
            name = "Test",
            country = "",
            city = "NYC",
            address = "456 Oak Ave"
        )

        assertThrows<IllegalArgumentException> {
            shelterService.create(request)
        }
    }

    @Test
    fun `create throws exception when city is blank`() {
        val request = CreateShelterRequest(
            name = "Test",
            country = "USA",
            city = "",
            address = "456 Oak Ave"
        )

        assertThrows<IllegalArgumentException> {
            shelterService.create(request)
        }
    }

    @Test
    fun `create throws exception when address is blank`() {
        val request = CreateShelterRequest(
            name = "Test",
            country = "USA",
            city = "NYC",
            address = ""
        )

        assertThrows<IllegalArgumentException> {
            shelterService.create(request)
        }
    }

    @Test
    fun `update updates shelter successfully`() {
        val shelterId = createTestShelter("Old Name", "USA", "NY", "New York")

        val result = shelterService.update(shelterId, UpdateShelterRequest(
            name = "Updated Name",
            city = "Brooklyn"
        ))

        assertTrue(result is ServiceResult.Success)
        val updated = (result as ServiceResult.Success).data
        assertEquals("Updated Name", updated.name)
        assertEquals("Brooklyn", updated.city)
        assertEquals("NY", updated.state)
    }

    @Test
    fun `update returns not found for non-existent id`() {
        val result = shelterService.update(999, UpdateShelterRequest(name = "New Name"))

        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `update only modifies specified fields`() {
        val shelterId = createTestShelter("Original", "USA", "NY", "New York", phone = "555-0000", email = "old@test.com")

        val result = shelterService.update(shelterId, UpdateShelterRequest(name = "Updated"))

        assertTrue(result is ServiceResult.Success)
        val updated = (result as ServiceResult.Success).data
        assertEquals("Updated", updated.name)
        assertEquals("USA", updated.country)
        assertEquals("NY", updated.state)
        assertEquals("New York", updated.city)
        assertEquals("555-0000", updated.phone)
        assertEquals("old@test.com", updated.email)
    }

    @Test
    fun `delete removes shelter successfully`() {
        val shelterId = createTestShelter("To Delete", "USA", "NY", "New York")

        val result = shelterService.delete(shelterId)

        assertTrue(result is ServiceResult.Success)
        assertNull(shelterService.getById(shelterId))
    }

    @Test
    fun `delete returns not found for non-existent id`() {
        val result = shelterService.delete(999)
        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `getCountries returns distinct countries`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "Canada", "ON", "Toronto")

        val result = shelterService.getCountries()

        assertEquals(2, result.size)
        assertTrue(result.contains("USA"))
        assertTrue(result.contains("Canada"))
    }

    @Test
    fun `getStatesByCountry returns states for country`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "USA", "NY", "Buffalo")
        createTestShelter("Shelter 4", "Canada", "ON", "Toronto")

        val result = shelterService.getStatesByCountry("USA")

        assertEquals(2, result.size)
        assertTrue(result.contains("NY"))
        assertTrue(result.contains("CA"))
    }

    @Test
    fun `getStatesByCountry returns empty for non-existent country`() {
        val result = shelterService.getStatesByCountry("France")
        assertTrue(result.isEmpty())
    }

    private fun createTestShelter(
        name: String,
        country: String,
        state: String?,
        city: String,
        address: String = "123 Test St",
        zip: String? = null,
        phone: String? = null,
        email: String? = null,
        website: String? = null,
        fiscalId: String? = null,
        bankName: String? = null,
        accountHolderName: String? = null,
        accountNumber: String? = null,
        iban: String? = null,
        swiftBic: String? = null,
        currency: String = "USD",
        description: String? = null
    ): Int {
        return transaction {
            AnimalShelters.insert {
                it[AnimalShelters.name] = name
                it[AnimalShelters.country] = country
                it[AnimalShelters.state] = state
                it[AnimalShelters.city] = city
                it[AnimalShelters.address] = address
                it[AnimalShelters.zip] = zip
                it[AnimalShelters.phone] = phone
                it[AnimalShelters.email] = email
                it[AnimalShelters.website] = website
                it[AnimalShelters.fiscalId] = fiscalId
                it[AnimalShelters.bankName] = bankName
                it[AnimalShelters.accountHolderName] = accountHolderName
                it[AnimalShelters.accountNumber] = accountNumber
                it[AnimalShelters.iban] = iban
                it[AnimalShelters.swiftBic] = swiftBic
                it[AnimalShelters.currency] = currency
                it[AnimalShelters.description] = description
                it[AnimalShelters.createdAt] = clock.now().toEpochMilliseconds()
                it[AnimalShelters.updatedAt] = clock.now().toEpochMilliseconds()
            } get AnimalShelters.id
        }!!
    }
}
