package com.adoptu.services

import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.PhotographerSettingsRequest
import com.adoptu.dto.UserRole
import com.adoptu.models.Users
import com.adoptu.models.UserActiveRoles
import com.adoptu.models.Photographers
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserServiceTest {

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
    }

    @Test
    fun `getById returns null for non-existent user`() {
        val result = UserService.getById(999)
        assertNull(result)
    }

    @Test
    fun `getById returns user by id`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val result = UserService.getById(userId)

        assertNotNull(result)
        assertEquals("testuser@test.com", result.username)
        assertEquals("Test User", result.displayName)
    }

    @Test
    fun `getById returns user with correct role`() {
        val adminId = createTestUser(username = "admin@test.com", displayName = "Admin", role = "ADMIN")
        val rescuerId = createTestUser(username = "rescuer@test.com", displayName = "Rescuer", role = "RESCUER")
        val adopterId = createTestUser(username = "adopter@test.com", displayName = "Adopter", role = "ADOPTER")

        val admin = UserService.getById(adminId)
        val rescuer = UserService.getById(rescuerId)
        val adopter = UserService.getById(adopterId)

        assertTrue(admin?.activeRoles?.contains(UserRole.ADMIN) == true)
        assertTrue(rescuer?.activeRoles?.contains(UserRole.RESCUER) == true)
        assertTrue(adopter?.activeRoles?.contains(UserRole.ADOPTER) == true)
    }

    @Test
    fun `acceptTerms updates privacy policy timestamp`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val beforeTime = System.currentTimeMillis()
        val result = UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))
        val afterTime = System.currentTimeMillis()

        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertNull(result.lastAcceptedTermsAndConditions)
        assert(result.lastAcceptedPrivacyPolicy!! >= beforeTime)
        assert(result.lastAcceptedPrivacyPolicy!! <= afterTime)
    }

    @Test
    fun `acceptTerms updates terms and conditions timestamp`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val beforeTime = System.currentTimeMillis()
        val result = UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = false,
            acceptTermsAndConditions = true
        ))
        val afterTime = System.currentTimeMillis()

        assertNotNull(result)
        assertNull(result.lastAcceptedPrivacyPolicy)
        assertNotNull(result.lastAcceptedTermsAndConditions)
        assert(result.lastAcceptedTermsAndConditions!! >= beforeTime)
        assert(result.lastAcceptedTermsAndConditions!! <= afterTime)
    }

    @Test
    fun `acceptTerms updates both timestamps`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        val beforeTime = System.currentTimeMillis()
        val result = UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = true
        ))
        val afterTime = System.currentTimeMillis()

        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertNotNull(result.lastAcceptedTermsAndConditions)
        assert(result.lastAcceptedPrivacyPolicy!! >= beforeTime)
        assert(result.lastAcceptedPrivacyPolicy!! <= afterTime)
        assert(result.lastAcceptedTermsAndConditions!! >= beforeTime)
        assert(result.lastAcceptedTermsAndConditions!! <= afterTime)
    }

    @Test
    fun `acceptTerms preserves existing timestamps when only one accepted`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Test User")

        // First accept privacy policy
        UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))

        // Then accept terms
        val result = UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = false,
            acceptTermsAndConditions = true
        ))

        // Both should be set now
        assertNotNull(result?.lastAcceptedPrivacyPolicy)
        assertNotNull(result?.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `acceptTerms returns null for non-existent user`() {
        val result = UserService.acceptTerms(999, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))
        assertNull(result)
    }

    @Test
    fun `getById returns user with all fields`() {
        val userId = createTestUser(
            username = "fulluser@test.com",
            displayName = "Full User"
        )

        val result = UserService.getById(userId)

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
                it[Users.createdAt] = System.currentTimeMillis()
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

    @Test
    fun `getPhotographers returns only photographers`() {
        createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")
        createTestUser(username = "photographer1@test.com", displayName = "Photo 1", role = "PHOTOGRAPHER")
        createTestUser(username = "photographer2@test.com", displayName = "Photo 2", role = "PHOTOGRAPHER")
        createTestUser(username = "rescuer@test.com", displayName = "Rescuer", role = "RESCUER")

        val result = UserService.getPhotographers()

        assertEquals(2, result.size)
    }

    @Test
    fun `getPhotographers returns empty list when no photographers`() {
        createTestUser(username = "user1@test.com", displayName = "User 1", role = "ADOPTER")

        val result = UserService.getPhotographers()

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
                it[photographerFee] = java.math.BigDecimal("50.00")
                it[photographerCurrency] = "USD"
            }
        }

        val result = UserService.getPhotographers()

        assertEquals(1, result.size)
        assertEquals(50.0, result.first().photographerFee)
        assertEquals("USD", result.first().photographerCurrency)
    }

    @Test
    fun `updateProfile updates display name`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Old Name")

        val result = UserService.updateProfile(userId, "New Name")

        assertNotNull(result)
        assertEquals("New Name", result.displayName)
    }

    @Test
    fun `updateProfile throws exception for blank name`() {
        val userId = createTestUser(username = "testuser@test.com", displayName = "Old Name")

        val exception = assertThrows<IllegalArgumentException> {
            UserService.updateProfile(userId, "")
        }

        assertEquals("Display name cannot be empty", exception.message)
    }

    @Test
    fun `updateProfile returns null for non-existent user`() {
        val result = UserService.updateProfile(999, "New Name")

        assertNull(result)
    }

    @Test
    fun `updatePhotographerSettings updates fee and currency`() {
        val userId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        val result = UserService.updatePhotographerSettings(
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
            UserService.updatePhotographerSettings(
                userId,
                PhotographerSettingsRequest(photographerFee = -10.0, photographerCurrency = "USD")
            )
        }

        assertEquals("Photographer fee must be zero or positive", exception.message)
    }

    @Test
    fun `updatePhotographerSettings allows zero fee`() {
        val userId = createTestUser(username = "photographer@test.com", displayName = "Photo", role = "PHOTOGRAPHER")

        val result = UserService.updatePhotographerSettings(
            userId,
            PhotographerSettingsRequest(photographerFee = 0.0, photographerCurrency = "USD")
        )

        assertNotNull(result)
        assertEquals(0.0, result.photographerFee)
    }

    @Test
    fun `updatePhotographerSettings returns null for non-existent user`() {
        val result = UserService.updatePhotographerSettings(
            999,
            PhotographerSettingsRequest(photographerFee = 50.0, photographerCurrency = "USD")
        )

        assertNull(result)
    }
}
