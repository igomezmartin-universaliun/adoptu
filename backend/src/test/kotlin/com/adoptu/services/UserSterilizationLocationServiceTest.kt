package com.adoptu.services

import com.adoptu.adapters.db.UserSterilizationLocations
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserSterilizationLocationRepository
import com.adoptu.dto.input.CreateUserSterilizationLocationRequest
import com.adoptu.dto.input.UpdateUserSterilizationLocationRequest
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
class UserSterilizationLocationServiceTest {

    private lateinit var userSterilizationLocationService: UserSterilizationLocationService
    private val clock: TestClock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        val repository = UserSterilizationLocationRepository(clock)
        userSterilizationLocationService = UserSterilizationLocationService(repository)
    }

    @Test
    fun `getByUserId returns null when no location exists`() {
        val userId = createTestUser("alice")

        val result = userSterilizationLocationService.getByUserId(userId)

        assertNull(result)
    }

    @Test
    fun `getByUserId returns location for existing user`() {
        val userId = createTestUser("bob")
        createTestUserLocation(userId, name = "Bob's Clinic", country = "USA", state = "NY", city = "New York")

        val result = userSterilizationLocationService.getByUserId(userId)

        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("Bob's Clinic", result.name)
        assertEquals("USA", result.country)
        assertEquals("NY", result.state)
        assertEquals("New York", result.city)
    }

    @Test
    fun `create creates new location with all fields`() {
        val userId = createTestUser("carol")
        val request = CreateUserSterilizationLocationRequest(
            name = "Carol's Vet Clinic",
            country = "USA",
            state = "CA",
            city = "Los Angeles",
            neighborhood = "Downtown",
            address = "123 Vet Street",
            zip = "90001",
            phone = "+1-555-1234",
            email = "vet@clinic.com",
            website = "https://vet.com",
            description = "A great veterinary clinic"
        )

        val result = userSterilizationLocationService.create(userId, request)

        assertEquals(userId, result.userId)
        assertEquals("Carol's Vet Clinic", result.name)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
        assertEquals("Los Angeles", result.city)
        assertEquals("Downtown", result.neighborhood)
        assertEquals("123 Vet Street", result.address)
        assertEquals("90001", result.zip)
        assertEquals("+1-555-1234", result.phone)
        assertEquals("vet@clinic.com", result.email)
        assertEquals("https://vet.com", result.website)
        assertEquals("A great veterinary clinic", result.description)

        val stored = userSterilizationLocationService.getByUserId(userId)
        assertNotNull(stored)
        assertEquals("Carol's Vet Clinic", stored.name)
    }

    @Test
    fun `create creates location with minimal fields`() {
        val userId = createTestUser("dave")
        val request = CreateUserSterilizationLocationRequest(
            name = "Minimal Clinic",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val result = userSterilizationLocationService.create(userId, request)

        assertEquals(userId, result.userId)
        assertEquals("Minimal Clinic", result.name)
        assertEquals("USA", result.country)
        assertNull(result.state)
        assertEquals("NYC", result.city)
    }

    @Test
    fun `create throws exception when name is blank`() {
        val userId = createTestUser("erin")
        val request = CreateUserSterilizationLocationRequest(
            name = "",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val exception = assertThrows<IllegalArgumentException> {
            userSterilizationLocationService.create(userId, request)
        }
        assertEquals("Name is required", exception.message)
    }

    @Test
    fun `create throws exception when country is blank`() {
        val userId = createTestUser("frank")
        val request = CreateUserSterilizationLocationRequest(
            name = "Test",
            country = "",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val exception = assertThrows<IllegalArgumentException> {
            userSterilizationLocationService.create(userId, request)
        }
        assertEquals("Country is required", exception.message)
    }

    @Test
    fun `create throws exception when city is blank`() {
        val userId = createTestUser("grace")
        val request = CreateUserSterilizationLocationRequest(
            name = "Test",
            country = "USA",
            city = "",
            address = "456 Oak Ave"
        )

        val exception = assertThrows<IllegalArgumentException> {
            userSterilizationLocationService.create(userId, request)
        }
        assertEquals("City is required", exception.message)
    }

    @Test
    fun `create throws exception when address is blank`() {
        val userId = createTestUser("heidi")
        val request = CreateUserSterilizationLocationRequest(
            name = "Test",
            country = "USA",
            city = "NYC",
            address = ""
        )

        val exception = assertThrows<IllegalArgumentException> {
            userSterilizationLocationService.create(userId, request)
        }
        assertEquals("Address is required", exception.message)
    }

    @Test
    fun `create called twice for same user updates rather than duplicates`() {
        val userId = createTestUser("ivan")
        val firstRequest = CreateUserSterilizationLocationRequest(
            name = "First Clinic",
            country = "USA",
            state = "NY",
            city = "New York",
            address = "123 Main St"
        )
        val secondRequest = CreateUserSterilizationLocationRequest(
            name = "Second Clinic",
            country = "Canada",
            state = "ON",
            city = "Toronto",
            address = "456 Queen St"
        )

        val firstResult = userSterilizationLocationService.create(userId, firstRequest)
        assertEquals("First Clinic", firstResult.name)

        val secondResult = userSterilizationLocationService.create(userId, secondRequest)

        assertEquals(userId, secondResult.userId)
        assertEquals("Second Clinic", secondResult.name)
        assertEquals("Canada", secondResult.country)
        assertEquals("Toronto", secondResult.city)

        val stored = userSterilizationLocationService.getByUserId(userId)
        assertNotNull(stored)
        assertEquals("Second Clinic", stored.name)
        assertEquals("Canada", stored.country)

        val count = transaction {
            UserSterilizationLocations.selectAll().count()
        }
        assertEquals(1L, count)
    }

    @Test
    fun `update updates location successfully`() {
        val userId = createTestUser("judy")
        createTestUserLocation(userId, name = "Old Name", country = "USA", state = "NY", city = "New York")

        val result = userSterilizationLocationService.update(userId, UpdateUserSterilizationLocationRequest(
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
    fun `update returns not found for user without location`() {
        val userId = createTestUser("kevin")

        val result = userSterilizationLocationService.update(userId, UpdateUserSterilizationLocationRequest(name = "New Name"))

        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `update only modifies specified fields`() {
        val userId = createTestUser("laura")
        createTestUserLocation(
            userId, name = "Original", country = "USA", state = "NY", city = "New York",
            phone = "555-0000", email = "old@test.com"
        )

        val result = userSterilizationLocationService.update(userId, UpdateUserSterilizationLocationRequest(name = "Updated"))

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
    fun `delete removes location successfully`() {
        val userId = createTestUser("mallory")
        createTestUserLocation(userId, name = "To Delete", country = "USA", state = "NY", city = "New York")

        val result = userSterilizationLocationService.delete(userId)

        assertTrue(result is ServiceResult.Success)
        assertNull(userSterilizationLocationService.getByUserId(userId))
    }

    @Test
    fun `delete returns not found for user without location`() {
        val userId = createTestUser("nina")

        val result = userSterilizationLocationService.delete(userId)

        assertTrue(result is ServiceResult.NotFound)
    }

    @Test
    fun `search throws exception when country is blank`() {
        assertThrows<IllegalArgumentException> {
            userSterilizationLocationService.search("")
        }
    }

    @Test
    fun `search throws exception when country is whitespace only`() {
        assertThrows<IllegalArgumentException> {
            userSterilizationLocationService.search("   ")
        }
    }

    @Test
    fun `search filters by country only`() {
        val u1 = createTestUser("o1")
        val u2 = createTestUser("o2")
        val u3 = createTestUser("o3")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York")
        createTestUserLocation(u2, name = "Clinic 2", country = "USA", state = "CA", city = "Los Angeles")
        createTestUserLocation(u3, name = "Clinic 3", country = "Canada", state = "ON", city = "Toronto")

        val result = userSterilizationLocationService.search("USA")

        assertEquals(2, result.size)
        assertTrue(result.all { it.country == "USA" })
    }

    @Test
    fun `search filters by country and state`() {
        val u1 = createTestUser("p1")
        val u2 = createTestUser("p2")
        val u3 = createTestUser("p3")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York")
        createTestUserLocation(u2, name = "Clinic 2", country = "USA", state = "CA", city = "Los Angeles")
        createTestUserLocation(u3, name = "Clinic 3", country = "USA", state = "NY", city = "Buffalo")

        val result = userSterilizationLocationService.search("USA", state = "NY")

        assertEquals(2, result.size)
        assertTrue(result.all { it.state == "NY" })
    }

    @Test
    fun `search filters by country state and city`() {
        val u1 = createTestUser("q1")
        val u2 = createTestUser("q2")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York")
        createTestUserLocation(u2, name = "Clinic 2", country = "USA", state = "NY", city = "Buffalo")

        val result = userSterilizationLocationService.search("USA", state = "NY", city = "New York")

        assertEquals(1, result.size)
        assertEquals("Clinic 1", result.first().name)
    }

    @Test
    fun `search filters by country state city and neighborhood`() {
        val u1 = createTestUser("r1")
        val u2 = createTestUser("r2")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York", neighborhood = "Brooklyn")
        createTestUserLocation(u2, name = "Clinic 2", country = "USA", state = "NY", city = "New York", neighborhood = "Queens")

        val result = userSterilizationLocationService.search("USA", state = "NY", city = "New York", neighborhood = "Brooklyn")

        assertEquals(1, result.size)
        assertEquals("Clinic 1", result.first().name)
    }

    @Test
    fun `search filters by all parameters including zip`() {
        val u1 = createTestUser("s1")
        val u2 = createTestUser("s2")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York", neighborhood = "Brooklyn", zip = "11201")
        createTestUserLocation(u2, name = "Clinic 2", country = "USA", state = "NY", city = "New York", neighborhood = "Brooklyn", zip = "11211")

        val result = userSterilizationLocationService.search("USA", state = "NY", city = "New York", neighborhood = "Brooklyn", zip = "11201")

        assertEquals(1, result.size)
        assertEquals("Clinic 1", result.first().name)
    }

    @Test
    fun `search ignores blank optional filters`() {
        val u1 = createTestUser("t1")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York")

        val result = userSterilizationLocationService.search("USA", state = "", city = "  ", neighborhood = null, zip = "")

        assertEquals(1, result.size)
    }

    @Test
    fun `search returns empty list for no match`() {
        val u1 = createTestUser("u1")
        createTestUserLocation(u1, name = "Clinic 1", country = "USA", state = "NY", city = "New York")

        val result = userSterilizationLocationService.search("France")

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

    private fun createTestUserLocation(
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
        description: String? = null
    ) {
        transaction {
            UserSterilizationLocations.insert {
                it[UserSterilizationLocations.userId] = userId
                it[UserSterilizationLocations.name] = name
                it[UserSterilizationLocations.country] = country
                it[UserSterilizationLocations.state] = state
                it[UserSterilizationLocations.city] = city
                it[UserSterilizationLocations.neighborhood] = neighborhood
                it[UserSterilizationLocations.address] = address
                it[UserSterilizationLocations.zip] = zip
                it[UserSterilizationLocations.phone] = phone
                it[UserSterilizationLocations.email] = email
                it[UserSterilizationLocations.website] = website
                it[UserSterilizationLocations.description] = description
                it[UserSterilizationLocations.createdAt] = clock.now().toEpochMilliseconds()
                it[UserSterilizationLocations.updatedAt] = clock.now().toEpochMilliseconds()
            }
        }
    }
}
