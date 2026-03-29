package com.adoptu.services

import com.adoptu.adapters.db.*
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.UpdatePhotographyRequestRequest
import com.adoptu.dto.input.UserDto
import com.adoptu.dto.input.UserRole
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class PhotographerServiceTest {

    private val clock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))
    private lateinit var photographerService: PhotographerService

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        TestDatabase.clearAllData()
        val petRepository = PetRepositoryImpl(clock)
        val userRepository = UserRepository(clock)
        val photographerRepository = PhotographerRepositoryImpl(petRepository, userRepository, clock)
        photographerService = PhotographerService(photographerRepository, MockNotificationAdapter(), userRepository, clock)
    }

    @Test
    fun `getPhotographers returns photographers from UserService`() {
        createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val result = photographerService.getPhotographers()

        assertEquals(1, result.size)
        assertEquals("Photo 1", result.first().displayName)
    }

    @Test
    fun `canSendMessage returns true when no recent requests`() {
        val userId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val result = photographerService.canSendMessage(userId)

        assertTrue(result)
    }

    @Test
    fun `canSendMessage returns false when request sent within week`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }

        val result = photographerService.canSendMessage(requesterId)

        assertFalse(result)
    }

    @Test
    fun `canSendMessage returns true when request older than week`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        val oneWeekAgo = clock.now().toEpochMilliseconds() - (PhotographerRepositoryImpl.ONE_WEEK + 1000)

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = oneWeekAgo
            }
        }

        val result = photographerService.canSendMessage(requesterId)

        assertTrue(result)
    }

    @Test
    fun `createPhotographyRequest fails with empty photographer list`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = emptyList(),
            petId = null,
            message = "Test message"
        )

        assertTrue(result.isFailure)
        assertEquals("At least one photographer must be selected", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createPhotographyRequest fails with more than 3 photographers`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer2@test.com", displayName = "Photo 2", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer3@test.com", displayName = "Photo 3", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer4@test.com", displayName = "Photo 4", role = "PHOTOGRAPHER")

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(2, 3, 4, 5),
            petId = null,
            message = "Test message"
        )

        assertTrue(result.isFailure)
        assertEquals("Maximum 3 photographers can be selected", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createPhotographyRequest fails when rate limited`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(photographerId),
            petId = null,
            message = "Test message"
        )

        assertTrue(result.isFailure)
        assertEquals("You can only send photographer requests once per week", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createPhotographyRequest fails when user not found`() {
        val result = photographerService.createPhotographyRequest(
            requesterId = 999,
            photographerIds = listOf(1),
            petId = null,
            message = "Test message"
        )

        assertTrue(result.isFailure)
        assertEquals("User not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createPhotographyRequest creates requests successfully`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            Photographers.insert {
                it[Photographers.userId] = photographerId
                it[photographerFee] = BigDecimal("50.00")
                it[photographerCurrency] = "USD"
            }
        }

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(photographerId),
            petId = null,
            message = "Please take photos of my pet"
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)

        val requests = photographerService.getMyRequests(requesterId)
        assertEquals(1, requests.size)
        assertEquals("Photo 1", requests.first()["photographerName"])
        assertEquals("Please take photos of my pet", requests.first()["message"])
        assertEquals("PENDING", requests.first()["status"])
    }

    @Test
    fun `createPhotographyRequest creates requests with pet`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        val petId = createTestPet(rescuerId = requesterId, name = "Buddy", type = "DOG")

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(photographerId),
            petId = petId,
            message = "Please take photos of Buddy"
        )

        assertTrue(result.isSuccess)

        val requests = photographerService.getMyRequests(requesterId)
        assertEquals(petId, requests.first()["petId"])
    }

    @Test
    fun `createPhotographyRequest sends notification to photographers`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val mockAdapter = MockNotificationAdapter()
        val petRepository = PetRepositoryImpl(clock)
        val userRepository = UserRepository(clock)
        val photographerRepository = PhotographerRepositoryImpl(petRepository, userRepository, clock)
        val serviceWithMock = PhotographerService(photographerRepository, mockAdapter, userRepository, clock)

        serviceWithMock.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(photographerId),
            petId = null,
            message = "Test message"
        )

        Thread.sleep(100)

        val sentEmails = mockAdapter.getSentEmails()
        assertEquals(1, sentEmails.size)
        assertTrue(sentEmails.first().subject.contains("Photography Session Request"))
    }

    @Test
    fun `getMyRequests returns empty list when no requests`() {
        val userId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val result = photographerService.getMyRequests(userId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMyRequests returns requests for user`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }

        val result = photographerService.getMyRequests(requesterId)

        assertEquals(1, result.size)
        assertEquals("Photo 1", result.first()["photographerName"])
    }

    @Test
    fun `getRequestsForPhotographer returns empty list when no requests`() {
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val result = photographerService.getRequestsForPhotographer(photographerId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRequestsForPhotographer returns requests for photographer`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        val petId = createTestPet(rescuerId = requesterId, name = "Buddy", type = "DOG")

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.petId] = petId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }

        val result = photographerService.getRequestsForPhotographer(photographerId)

        assertEquals(1, result.size)
        assertEquals("User 1", result.first()["requesterName"])
        assertEquals("Buddy", result.first()["petName"])
    }

    @Test
    fun `createPhotographyRequest skips non-existent photographers`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(2, 999),
            petId = null,
            message = "Test message"
        )

        assertTrue(result.isSuccess)
        val requests = photographerService.getMyRequests(requesterId)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createPhotographyRequest allows exactly 3 photographers`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer2@test.com", displayName = "Photo 2", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer3@test.com", displayName = "Photo 3", role = "PHOTOGRAPHER")

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerIds = listOf(2, 3, 4),
            petId = null,
            message = "Test message"
        )

        assertTrue(result.isSuccess)
        val requests = photographerService.getMyRequests(requesterId)
        assertEquals(3, requests.size)
    }

    @Test
    fun `getPhotographers returns only photographers`() {
        createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer2@test.com", displayName = "Photo 2", role = "PHOTOGRAPHER")
        createTestUser(username = "rescuer@test.com", displayName = "Rescuer", role = "RESCUER")

        val result = photographerService.getPhotographers()

        assertEquals(2, result.size)
    }

    @Test
    fun `getPhotographers returns empty list when no photographers`() {
        createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val result = photographerService.getPhotographers()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPhotographers includes photographer fee and currency`() {
        val photographerId = createTestUser(
            username = "photographer1@test.com",
            displayName = "Photo 1",
            role = "PHOTOGRAPHER"
        )

        transaction {
            Photographers.insert {
                it[Photographers.userId] = photographerId
                it[photographerFee] = BigDecimal("50.00")
                it[photographerCurrency] = "USD"
            }
        }

        val result = photographerService.getPhotographers()

        assertEquals(1, result.size)
        assertEquals(50.0, result.first().photographerFee)
        assertEquals("USD", result.first().photographerCurrency)
    }

    @Test
    fun `updatePhotographerSettings updates fee and currency`() {
        val userId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        val result = photographerService.updatePhotographerSettings(
            userId,
            PhotographerSettingsRequest(photographerFee = 75.0, photographerCurrency = "EUR")
        )

        assertNotNull(result)
        assertEquals(75.0, result.photographerFee)
        assertEquals("EUR", result.photographerCurrency)
    }

    @Test
    fun `updatePhotographerSettings throws exception for negative fee`() {
        val userId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        val exception = assertThrows<IllegalArgumentException> {
            photographerService.updatePhotographerSettings(
                userId,
                PhotographerSettingsRequest(photographerFee = -10.0, photographerCurrency = "USD")
            )
        }

        assertEquals("Photographer fee must be zero or positive", exception.message)
    }

    @Test
    fun `updatePhotographerSettings allows zero fee`() {
        val userId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        val result = photographerService.updatePhotographerSettings(
            userId,
            PhotographerSettingsRequest(photographerFee = 0.0, photographerCurrency = "USD")
        )

        assertNotNull(result)
        assertEquals(0.0, result.photographerFee)
    }

    @Test
    fun `updatePhotographerSettings returns null for non-existent user`() {
        val result = photographerService.updatePhotographerSettings(
            999,
            PhotographerSettingsRequest(photographerFee = 50.0, photographerCurrency = "USD")
        )

        assertNull(result)
    }

    private fun createTestUser(
        username: String,
        displayName: String,
        role: String = "ADOPTER"
    ): Int {
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
            } get Users.id
        }!!
        
        transaction {
            UserActiveRoles.insert {
                it[UserActiveRoles.userId] = userId
                it[UserActiveRoles.role] = role
            }
        }
        
        return userId
    }

    private fun createTestPet(rescuerId: Int, name: String, type: String): Int {
        return transaction {
            Pets.insert {
                it[Pets.rescuerId] = rescuerId
                it[Pets.name] = name
                it[Pets.type] = type
                it[Pets.description] = "Test description"
                it[Pets.weight] = BigDecimal("10.0")
                it[Pets.ageYears] = 2
                it[Pets.ageMonths] = 0
                it[Pets.sex] = "MALE"
                it[Pets.breed] = "Test breed"
                it[Pets.status] = "AVAILABLE"
                it[Pets.size] = "MEDIUM"
                it[Pets.isUrgent] = false
                it[Pets.createdAt] = clock.now().toEpochMilliseconds()
            } get Pets.id
        }!!
    }

    @Test
    fun `getPhotographerById returns photographer when exists`() {
        val photographerId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        transaction {
            Photographers.insert {
                it[Photographers.userId] = photographerId
                it[photographerFee] = BigDecimal("50.00")
                it[photographerCurrency] = "USD"
            }
        }

        val result = photographerService.getPhotographerById(photographerId)

        assertNotNull(result)
        assertEquals(photographerId, result.userId)
        assertEquals(50.0, result.photographerFee)
    }

    @Test
    fun `getPhotographerById returns null when not found`() {
        val result = photographerService.getPhotographerById(999)

        assertNull(result)
    }

    @Test
    fun `createPhotographyRequest single creates request successfully`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            Photographers.insert {
                it[Photographers.userId] = photographerId
                it[photographerFee] = BigDecimal("50.00")
                it[photographerCurrency] = "USD"
            }
        }

        val result = photographerService.createPhotographyRequest(
            requesterId = requesterId,
            photographerId = photographerId,
            petId = null,
            message = "Please take photos"
        )

        assertNotNull(result)
        assertEquals(photographerId, result["photographerId"])
        assertEquals("PENDING", result["status"])
    }

    @Test
    fun `createPhotographyRequest single throws when photographer not found`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val exception = assertThrows<IllegalArgumentException> {
            photographerService.createPhotographyRequest(
                requesterId = requesterId,
                photographerId = 999,
                petId = null,
                message = "Please take photos"
            )
        }

        assertEquals("Photographer not found or not active", exception.message)
    }

    @Test
    fun `getRequestsForUser returns requests for photographer role`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }

        val userDto = UserDto(
            id = photographerId,
            username = "photographer1@test.com",
            displayName = "Photo 1",
            language = "en",
            activeRoles = setOf(UserRole.PHOTOGRAPHER),
            lastAcceptedPrivacyPolicy = null,
            lastAcceptedTermsAndConditions = null,
            isBanned = false
        )

        val result = photographerService.getRequestsForUser(userDto)

        assertEquals(1, result.size)
        assertEquals(requesterId, result.first()["requesterId"])
    }

    @Test
    fun `getRequestsForUser returns requests for non-photographer role`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test message"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }

        val userDto = UserDto(
            id = requesterId,
            username = "user1@test.com",
            displayName = "User 1",
            language = "en",
            activeRoles = setOf(UserRole.ADOPTER),
            lastAcceptedPrivacyPolicy = null,
            lastAcceptedTermsAndConditions = null,
            isBanned = false
        )

        val result = photographerService.getRequestsForUser(userDto)

        assertEquals(1, result.size)
        assertEquals(photographerId, result.first()["photographerId"])
    }

    @Test
    fun `activatePhotographerProfile adds photographer role`() {
        val userId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val result = photographerService.activatePhotographerProfile(userId)

        assertNotNull(result)
        assertTrue(result.activeRoles.contains(UserRole.PHOTOGRAPHER))
    }

    @Test
    fun `deactivatePhotographerProfile removes photographer role`() {
        val userId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "PHOTOGRAPHER")

        val result = photographerService.deactivatePhotographerProfile(userId)

        assertNotNull(result)
        assertFalse(result.activeRoles.contains(UserRole.PHOTOGRAPHER))
    }

    @Test
    fun `updatePhotographyRequest returns NotFound when request not exists`() {
        val userId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        val result = photographerService.updatePhotographyRequest(
            userId = userId,
            user = null,
            requestId = 999,
            body = UpdatePhotographyRequestRequest(status = "APPROVED")
        )

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `updatePhotographyRequest returns Forbidden when user not authorized`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        val otherUserId = createTestUser(username = "other@test.com", displayName = "Other", role = "ADOPTER")

        val requestId = transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            } get PhotographyRequests.id
        }!!

        val result = photographerService.updatePhotographyRequest(
            userId = otherUserId,
            user = null,
            requestId = requestId,
            body = UpdatePhotographyRequestRequest(status = "APPROVED")
        )

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `updatePhotographyRequest allows photographer to approve request`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val requestId = transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            } get PhotographyRequests.id
        }!!

        val result = photographerService.updatePhotographyRequest(
            userId = photographerId,
            user = null,
            requestId = requestId,
            body = UpdatePhotographyRequestRequest(status = "APPROVED")
        )

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `updatePhotographyRequest allows requester to cancel request`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val requestId = transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            } get PhotographyRequests.id
        }!!

        val result = photographerService.updatePhotographyRequest(
            userId = requesterId,
            user = null,
            requestId = requestId,
            body = UpdatePhotographyRequestRequest(status = "CANCELLED")
        )

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `updatePhotographyRequest allows admin to update any request`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val requestId = transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test"
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            } get PhotographyRequests.id
        }!!

        val adminUser = UserDto(
            id = 999,
            username = "admin@test.com",
            displayName = "Admin",
            language = "en",
            activeRoles = setOf(UserRole.ADMIN),
            lastAcceptedPrivacyPolicy = null,
            lastAcceptedTermsAndConditions = null,
            isBanned = false
        )

        val result = photographerService.updatePhotographyRequest(
            userId = 999,
            user = adminUser,
            requestId = requestId,
            body = UpdatePhotographyRequestRequest(status = "APPROVED")
        )

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `updatePhotographyRequest throws on invalid status transition`() {
        val requesterId = createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        val photographerId = createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")

        val requestId = transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.message] = "Test"
                it[PhotographyRequests.status] = "APPROVED"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            } get PhotographyRequests.id
        }!!

        val exception = assertThrows<IllegalArgumentException> {
            photographerService.updatePhotographyRequest(
                userId = requesterId,
                user = null,
                requestId = requestId,
                body = UpdatePhotographyRequestRequest(status = "PENDING")
            )
        }

        assertEquals("Invalid status transition", exception.message)
    }
}
