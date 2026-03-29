package com.adoptu.services

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.input.AcceptTermsRequest
import com.adoptu.dto.input.UserRole
import com.adoptu.mocks.TestClock
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class UserServiceTest {

    private lateinit var userService: UserService
    private val clock: TestClock = TestClock(Instant.parse("2024-01-15T10:00:00Z"))

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        val userRepository = UserRepository(clock)
        userService = UserService(userRepository)
    }

    @Test
    fun `getById returns null for non-existent user`() {
        val result = userService.getById(999)
        assertNull(result)
    }

    @Test
    fun `getById returns user by id`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val result = userService.getById(userId)

        assertNotNull(result)
        assertEquals("testuser@test.com", result.username)
        assertEquals("Test User", result.displayName)
    }

    @Test
    fun `getById returns user with correct role`() {
        val adminId = createTestUser(username = "admin@test.com", displayName = "Admin", role = "ADMIN")
        val rescuerId = createTestUser(username = "rescuer@test.com", displayName = "Rescuer", role = "RESCUER")
        val adopterId = createTestUser(username = "adopter@test.com", displayName = "Adopter", role = "ADOPTER")

        val admin = userService.getById(adminId)
        val rescuer = userService.getById(rescuerId)
        val adopter = userService.getById(adopterId)

        assertTrue(admin?.activeRoles?.contains(UserRole.ADMIN) == true)
        assertTrue(rescuer?.activeRoles?.contains(UserRole.RESCUER) == true)
        assertTrue(adopter?.activeRoles?.contains(UserRole.ADOPTER) == true)
    }

    @Test
    fun `getByEmail returns null for non-existent user`() {
        val result = userService.getByEmail("nonexistent@test.com")
        assertNull(result)
    }

    @Test
    fun `getByEmail returns user by email`() {
        val userId = createTestUser(username = "email@test.com", displayName = "Email User")

        val result = userService.getByEmail("email@test.com")

        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("Email User", result.displayName)
    }

    @Test
    fun `getAllUsers returns all users`() {
        createTestUser(username = "user1@test.com", displayName = "User 1")
        createTestUser(username = "user2@test.com", displayName = "User 2")

        val result = userService.getAllUsers()

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllUsers returns empty list when no users`() {
        val result = userService.getAllUsers()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRescuers returns only rescuers`() {
        createTestUser(username = "admin@test.com", displayName = "Admin", role = "ADMIN")
        createTestUser(username = "rescuer@test.com", displayName = "Rescuer", role = "RESCUER")
        createTestUser(username = "adopter@test.com", displayName = "Adopter", role = "ADOPTER")

        val result = userService.getRescuers()

        assertEquals(1, result.size)
        assertEquals("Rescuer", result.first().displayName)
    }

    @Test
    fun `getRescuers returns empty list when no rescuers`() {
        createTestUser(username = "admin@test.com", displayName = "Admin", role = "ADMIN")

        val result = userService.getRescuers()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `banUser sets isBanned to true`() {
        val userId = createTestUser(username = "banned@test.com", displayName = "Banned User")

        val result = userService.banUser(userId, "Spam activity")

        assertTrue(result)
        val user = userService.getById(userId)
        assertTrue(user?.isBanned == true)
        assertEquals("Spam activity", user?.banReason)
    }

    @Test
    fun `banUser returns false for non-existent user`() {
        val result = userService.banUser(999, "Test reason")
        assertFalse(result)
    }

    @Test
    fun `banUser works without reason`() {
        val userId = createTestUser(username = "banned2@test.com", displayName = "Banned User 2")

        val result = userService.banUser(userId)

        assertTrue(result)
        val user = userService.getById(userId)
        assertTrue(user?.isBanned == true)
    }

    @Test
    fun `unbanUser sets isBanned to false`() {
        val userId = createTestUser(username = "unbanned@test.com", displayName = "Unbanned User")
        userService.banUser(userId, "Was banned")

        val result = userService.unbanUser(userId)

        assertTrue(result)
        val user = userService.getById(userId)
        assertFalse(user?.isBanned == true)
        assertNull(user?.banReason)
    }

    @Test
    fun `unbanUser returns false for non-existent user`() {
        val result = userService.unbanUser(999)
        assertFalse(result)
    }

    @Test
    fun `isBanned returns true for banned user`() {
        val userId = createTestUser(username = "banned3@test.com", displayName = "Banned 3")
        userService.banUser(userId, "Test")

        val result = userService.isBanned(userId)

        assertTrue(result)
    }

    @Test
    fun `isBanned returns false for non-banned user`() {
        val userId = createTestUser(username = "normal@test.com", displayName = "Normal User")

        val result = userService.isBanned(userId)

        assertFalse(result)
    }

    @Test
    fun `isBanned returns false for non-existent user`() {
        val result = userService.isBanned(999)
        assertFalse(result)
    }

    @Test
    fun `isRoleActive returns true for active role`() {
        val userId = createTestUser(username = "role@test.com", displayName = "Role User", role = "RESCUER")

        val result = userService.isRoleActive(userId, UserRole.RESCUER)

        assertTrue(result)
    }

    @Test
    fun `isRoleActive returns false for inactive role`() {
        val userId = createTestUser(username = "norole@test.com", displayName = "No Role User", role = "ADOPTER")

        val result = userService.isRoleActive(userId, UserRole.RESCUER)

        assertFalse(result)
    }

    @Test
    fun `activateRescuerProfile adds rescuer role`() {
        val userId = createTestUser(username = "activate@test.com", displayName = "Activate User", role = "ADOPTER")

        val result = userService.activateRescuerProfile(userId)

        assertNotNull(result)
        assertTrue(result?.activeRoles?.contains(UserRole.RESCUER) == true)
    }

    @Test
    fun `activateRescuerProfile returns null for non-existent user`() {
        val result = userService.activateRescuerProfile(999)
        assertNull(result)
    }

    @Test
    fun `deactivateRescuerProfile removes rescuer role`() {
        val userId = createTestUser(username = "deactivate@test.com", displayName = "Deactivate User", role = "RESCUER")

        val result = userService.deactivateRescuerProfile(userId)

        assertNotNull(result)
        assertFalse(result?.activeRoles?.contains(UserRole.RESCUER) == true)
    }

    @Test
    fun `activateTemporalHomeProfile adds temporal home role`() {
        val userId = createTestUser(username = "temporal@test.com", displayName = "Temporal User", role = "ADOPTER")

        val result = userService.activateTemporalHomeProfile(userId)

        assertNotNull(result)
        assertTrue(result?.activeRoles?.contains(UserRole.TEMPORAL_HOME) == true)
    }

    @Test
    fun `deactivateTemporalHomeProfile removes temporal home role`() {
        val userId = createTestUser(username = "detemporal@test.com", displayName = "DeTemporal User", role = "TEMPORAL_HOME")

        val result = userService.deactivateTemporalHomeProfile(userId)

        assertNotNull(result)
        assertFalse(result?.activeRoles?.contains(UserRole.TEMPORAL_HOME) == true)
    }

    @Test
    fun `updateProfile updates display name`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Old Name")

        val result = userService.updateProfile(userId, "New Name")

        assertNotNull(result)
        assertEquals("New Name", result.displayName)
    }

    @Test
    fun `updateProfile updates language`() {
        val userId = createTestUser(username = "lang@test.com", displayName = "Lang User")

        val result = userService.updateProfile(userId, "Lang User", "es")

        assertNotNull(result)
        assertEquals("es", result.language)
    }

    @Test
    fun `updateProfile throws exception for blank name`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Old Name")

        val exception = assertThrows<IllegalArgumentException> {
            userService.updateProfile(userId, "")
        }

        assertEquals("Display name cannot be empty", exception.message)
    }

    @Test
    fun `updateProfile returns null for non-existent user`() {
        val result = userService.updateProfile(999, "New Name")
        assertNull(result)
    }

    @Test
    fun `updateLanguage updates language`() {
        val userId = createTestUser(username = "update@test.com", displayName = "Update User")

        val result = userService.updateLanguage(userId, "fr")

        assertNotNull(result)
        assertEquals("fr", result.language)
    }

    @Test
    fun `updateLanguage returns null for non-existent user`() {
        val result = userService.updateLanguage(999, "de")
        assertNull(result)
    }

    @Test
    fun `acceptTerms updates privacy policy timestamp`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val expectedTime = clock.now().toEpochMilliseconds()
        val result = userService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))

        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertNull(result.lastAcceptedTermsAndConditions)
        assertEquals(expectedTime, result.lastAcceptedPrivacyPolicy)
    }

    @Test
    fun `acceptTerms updates terms and conditions timestamp`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val expectedTime = clock.now().toEpochMilliseconds()
        val result = userService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = false,
            acceptTermsAndConditions = true
        ))

        assertNotNull(result)
        assertNull(result.lastAcceptedPrivacyPolicy)
        assertNotNull(result.lastAcceptedTermsAndConditions)
        assertEquals(expectedTime, result.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `acceptTerms updates both timestamps`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val expectedTime = clock.now().toEpochMilliseconds()
        val result = userService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = true
        ))

        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertNotNull(result.lastAcceptedTermsAndConditions)
        assertEquals(expectedTime, result.lastAcceptedPrivacyPolicy)
        assertEquals(expectedTime, result.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `acceptTerms preserves existing timestamps when only one accepted`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        userService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))

        val result = userService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = false,
            acceptTermsAndConditions = true
        ))

        assertNotNull(result?.lastAcceptedPrivacyPolicy)
        assertNotNull(result?.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `acceptTerms returns null for non-existent user`() {
        val result = userService.acceptTerms(999, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))
        assertNull(result)
    }

    @Test
    fun `isUserVerified returns false for unverified user`() {
        val userId = createTestUser(username = "unverified@test.com", displayName = "Unverified User")

        val result = userService.isUserVerified(userId)

        assertFalse(result)
    }

    @Test
    fun `isUserVerified returns true for verified user`() {
        val userId = createTestUser(username = "verified@test.com", displayName = "Verified User")
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.isEmailVerified] = true
            }
        }

        val result = userService.isUserVerified(userId)

        assertTrue(result)
    }

    @Test
    fun `verifyToken returns false for invalid token`() {
        val result = userService.verifyToken("invalid-token")
        assertFalse(result)
    }

    @Test
    fun `verifyTokenAndGetLanguage returns false and en for invalid token`() {
        val result = userService.verifyTokenAndGetLanguage("invalid-token")
        
        assertFalse(result.first)
        assertEquals("en", result.second)
    }

    @Test
    fun `getById returns user with all fields`() {
        val userId = createTestUser(
            username = "fulluser@test.com",
            displayName = "Full User"
        )

        val result = userService.getById(userId)

        assertNotNull(result)
        assertEquals("fulluser@test.com", result.username)
        assertEquals("Full User", result.displayName)
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
}
