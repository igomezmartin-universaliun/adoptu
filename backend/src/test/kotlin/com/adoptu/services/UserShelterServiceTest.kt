package com.adoptu.services

import com.adoptu.adapters.db.UserShelters
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserShelterRepository
import com.adoptu.dto.input.CreateUserShelterRequest
import com.adoptu.dto.input.UpdateUserShelterRequest
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
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
class UserShelterServiceTest {

    private lateinit var userShelterService: UserShelterService
    private val clock: TestClock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        val userShelterRepository = UserShelterRepository(clock)
        userShelterService = UserShelterService(userShelterRepository)
    }

    @Test
    fun `getByUserId returns null when no shelter exists`() {
        val userId = createTestUser("alice")

        val result = userShelterService.getByUserId(userId)

        assertNull(result)
    }

    @Test
    fun `getByUserId returns shelter for existing user`() {
        val userId = createTestUser("bob")
        createTestUserShelter(userId, name = "Bob's Shelter", country = "USA", state = "NY", city = "New York")

        val result = userShelterService.getByUserId(userId)

        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("Bob's Shelter", result.name)
        assertEquals("USA", result.country)
        assertEquals("NY", result.state)
        assertEquals("New York", result.city)
    }

    @Test
    fun `create creates new shelter with all fields`() {
        val userId = createTestUser("carol")
        val request = CreateUserShelterRequest(
            name = "Carol's Shelter",
            country = "USA",
            state = "CA",
            city = "Los Angeles",
            neighborhood = "Downtown",
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

        val result = userShelterService.create(userId, request)

        assertEquals(userId, result.userId)
        assertEquals("Carol's Shelter", result.name)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
        assertEquals("Los Angeles", result.city)
        assertEquals("Downtown", result.neighborhood)
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

        val stored = userShelterService.getByUserId(userId)
        assertNotNull(stored)
        assertEquals("Carol's Shelter", stored.name)
    }

    @Test
    fun `create creates shelter with minimal fields`() {
        val userId = createTestUser("dave")
        val request = CreateUserShelterRequest(
            name = "Minimal Shelter",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val result = userShelterService.create(userId, request)

        assertEquals(userId, result.userId)
        assertEquals("Minimal Shelter", result.name)
        assertEquals("USA", result.country)
        assertNull(result.state)
        assertEquals("NYC", result.city)
    }

    @Test
    fun `create throws exception when name is blank`() {
        val userId = createTestUser("erin")
        val request = CreateUserShelterRequest(
            name = "",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val exception = assertThrows<IllegalArgumentException> {
            userShelterService.create(userId, request)
        }
        assertEquals("Name is required", exception.message)
    }

    @Test
    fun `create throws exception when country is blank`() {
        val userId = createTestUser("frank")
        val request = CreateUserShelterRequest(
            name = "Test",
            country = "",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val exception = assertThrows<IllegalArgumentException> {
            userShelterService.create(userId, request)
        }
        assertEquals("Country is required", exception.message)
    }

    @Test
    fun `create throws exception when city is blank`() {
        val userId = createTestUser("grace")
        val request = CreateUserShelterRequest(
            name = "Test",
            country = "USA",
            city = "",
            address = "456 Oak Ave"
        )

        val exception = assertThrows<IllegalArgumentException> {
            userShelterService.create(userId, request)
        }
        assertEquals("City is required", exception.message)
    }

    @Test
    fun `create throws exception when address is blank`() {
        val userId = createTestUser("heidi")
        val request = CreateUserShelterRequest(
            name = "Test",
            country = "USA",
            city = "NYC",
            address = ""
        )

        val exception = assertThrows<IllegalArgumentException> {
            userShelterService.create(userId, request)
        }
        assertEquals("Address is required", exception.message)
    }

    @Test
    fun `create called twice for same user updates rather than duplicates`() {
        val userId = createTestUser("ivan")
        val firstRequest = CreateUserShelterRequest(
            name = "First Shelter",
            country = "USA",
            state = "NY",
            city = "New York",
            address = "123 Main St"
        )
        val secondRequest = CreateUserShelterRequest(
            name = "Second Shelter",
            country = "Canada",
            state = "ON",
            city = "Toronto",
            address = "456 Queen St"
        )

        val firstResult = userShelterService.create(userId, firstRequest)
        assertEquals("First Shelter", firstResult.name)

        val secondResult = userShelterService.create(userId, secondRequest)

        assertEquals(userId, secondResult.userId)
        assertEquals("Second Shelter", secondResult.name)
        assertEquals("Canada", secondResult.country)
        assertEquals("Toronto", secondResult.city)

        val stored = userShelterService.getByUserId(userId)
        assertNotNull(stored)
        assertEquals("Second Shelter", stored.name)
        assertEquals("Canada", stored.country)

        val count = transaction {
            UserShelters.selectAll().count()
        }
        assertEquals(1L, count)
    }

    @Test
    fun `update updates shelter successfully`() {
        val userId = createTestUser("judy")
        createTestUserShelter(userId, name = "Old Name", country = "USA", state = "NY", city = "New York")

        val result = userShelterService.update(userId, UpdateUserShelterRequest(
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
    fun `update returns not found for user without shelter`() {
        val userId = createTestUser("kevin")

        val result = userShelterService.update(userId, UpdateUserShelterRequest(name = "New Name"))

        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `update only modifies specified fields`() {
        val userId = createTestUser("laura")
        createTestUserShelter(
            userId, name = "Original", country = "USA", state = "NY", city = "New York",
            phone = "555-0000", email = "old@test.com"
        )

        val result = userShelterService.update(userId, UpdateUserShelterRequest(name = "Updated"))

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
        val userId = createTestUser("mallory")
        createTestUserShelter(userId, name = "To Delete", country = "USA", state = "NY", city = "New York")

        val result = userShelterService.delete(userId)

        assertTrue(result is ServiceResult.Success)
        assertNull(userShelterService.getByUserId(userId))
    }

    @Test
    fun `delete returns not found for user without shelter`() {
        val userId = createTestUser("nina")

        val result = userShelterService.delete(userId)

        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `search throws exception when country is blank`() {
        assertThrows<IllegalArgumentException> {
            userShelterService.search("")
        }
    }

    @Test
    fun `search throws exception when country is whitespace only`() {
        assertThrows<IllegalArgumentException> {
            userShelterService.search("   ")
        }
    }

    @Test
    fun `search filters by country only`() {
        val u1 = createTestUser("o1")
        val u2 = createTestUser("o2")
        val u3 = createTestUser("o3")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York")
        createTestUserShelter(u2, name = "Shelter 2", country = "USA", state = "CA", city = "Los Angeles")
        createTestUserShelter(u3, name = "Shelter 3", country = "Canada", state = "ON", city = "Toronto")

        val result = userShelterService.search("USA")

        assertEquals(2, result.size)
        assertTrue(result.all { it.country == "USA" })
    }

    @Test
    fun `search filters by country and state`() {
        val u1 = createTestUser("p1")
        val u2 = createTestUser("p2")
        val u3 = createTestUser("p3")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York")
        createTestUserShelter(u2, name = "Shelter 2", country = "USA", state = "CA", city = "Los Angeles")
        createTestUserShelter(u3, name = "Shelter 3", country = "USA", state = "NY", city = "Buffalo")

        val result = userShelterService.search("USA", state = "NY")

        assertEquals(2, result.size)
        assertTrue(result.all { it.state == "NY" })
    }

    @Test
    fun `search filters by country state and city`() {
        val u1 = createTestUser("q1")
        val u2 = createTestUser("q2")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York")
        createTestUserShelter(u2, name = "Shelter 2", country = "USA", state = "NY", city = "Buffalo")

        val result = userShelterService.search("USA", state = "NY", city = "New York")

        assertEquals(1, result.size)
        assertEquals("Shelter 1", result.first().name)
    }

    @Test
    fun `search filters by country state city and neighborhood`() {
        val u1 = createTestUser("r1")
        val u2 = createTestUser("r2")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York", neighborhood = "Brooklyn")
        createTestUserShelter(u2, name = "Shelter 2", country = "USA", state = "NY", city = "New York", neighborhood = "Queens")

        val result = userShelterService.search("USA", state = "NY", city = "New York", neighborhood = "Brooklyn")

        assertEquals(1, result.size)
        assertEquals("Shelter 1", result.first().name)
    }

    @Test
    fun `search filters by all parameters including zip`() {
        val u1 = createTestUser("s1")
        val u2 = createTestUser("s2")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York", neighborhood = "Brooklyn", zip = "11201")
        createTestUserShelter(u2, name = "Shelter 2", country = "USA", state = "NY", city = "New York", neighborhood = "Brooklyn", zip = "11211")

        val result = userShelterService.search("USA", state = "NY", city = "New York", neighborhood = "Brooklyn", zip = "11201")

        assertEquals(1, result.size)
        assertEquals("Shelter 1", result.first().name)
    }

    @Test
    fun `search ignores blank optional filters`() {
        val u1 = createTestUser("t1")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York")

        val result = userShelterService.search("USA", state = "", city = "  ", neighborhood = null, zip = "")

        assertEquals(1, result.size)
    }

    @Test
    fun `search returns empty list for no match`() {
        val u1 = createTestUser("u1")
        createTestUserShelter(u1, name = "Shelter 1", country = "USA", state = "NY", city = "New York")

        val result = userShelterService.search("France")

        assertTrue(result.isEmpty())
    }

    private fun createTestUser(username: String, displayName: String = username): Int {
        return transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
            } get Users.id
        }
    }

    private fun createTestUserShelter(
        userId: Int,
        name: String,
        country: String,
        state: String?,
        city: String,
        neighborhood: String? = null,
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
    ) {
        transaction {
            UserShelters.insert {
                it[UserShelters.userId] = userId
                it[UserShelters.name] = name
                it[UserShelters.country] = country
                it[UserShelters.state] = state
                it[UserShelters.city] = city
                it[UserShelters.neighborhood] = neighborhood
                it[UserShelters.address] = address
                it[UserShelters.zip] = zip
                it[UserShelters.phone] = phone
                it[UserShelters.email] = email
                it[UserShelters.website] = website
                it[UserShelters.fiscalId] = fiscalId
                it[UserShelters.bankName] = bankName
                it[UserShelters.accountHolderName] = accountHolderName
                it[UserShelters.accountNumber] = accountNumber
                it[UserShelters.iban] = iban
                it[UserShelters.swiftBic] = swiftBic
                it[UserShelters.currency] = currency
                it[UserShelters.description] = description
                it[UserShelters.createdAt] = clock.now().toEpochMilliseconds()
                it[UserShelters.updatedAt] = clock.now().toEpochMilliseconds()
            }
        }
    }
}
