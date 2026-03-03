package com.adoptu.services

import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.UserRole
import com.adoptu.models.Users
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        val userId = createTestUser(username = "testuser", displayName = "Test User", email = "mocks@mocks.com")

        val result = UserService.getById(userId)

        assertNotNull(result)
        assertEquals("testuser", result.username)
        assertEquals("Test User", result.displayName)
        assertEquals("mocks@mocks.com", result.email)
    }

    @Test
    fun `getById returns user with correct role`() {
        val adminId = createTestUser(username = "admin", displayName = "Admin", role = "ADMIN")
        val rescuerId = createTestUser(username = "rescuer", displayName = "Rescuer", role = "RESCUER")
        val adopterId = createTestUser(username = "adopter", displayName = "Adopter", role = "ADOPTER")

        val admin = UserService.getById(adminId)
        val rescuer = UserService.getById(rescuerId)
        val adopter = UserService.getById(adopterId)

        assertEquals(UserRole.ADMIN, admin?.role)
        assertEquals(UserRole.RESCUER, rescuer?.role)
        assertEquals(UserRole.ADOPTER, adopter?.role)
    }

    @Test
    fun `acceptTerms updates privacy policy timestamp`() {
        val userId = createTestUser(username = "testuser", displayName = "Test User")

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
        val userId = createTestUser(username = "testuser", displayName = "Test User")

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
        val userId = createTestUser(username = "testuser", displayName = "Test User")

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
        val userId = createTestUser(username = "testuser", displayName = "Test User")

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
            username = "fulluser",
            displayName = "Full User",
            email = "full@mocks.com"
        )

        val result = UserService.getById(userId)

        assertNotNull(result)
        assertEquals("fulluser", result.username)
        assertEquals("Full User", result.displayName)
        assertEquals("full@mocks.com", result.email)
    }

    private fun createTestUser(
        username: String,
        displayName: String,
        email: String? = null,
        role: String = "ADOPTER"
    ): Int {
        return transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.email] = email
                it[Users.role] = role
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }!!
    }
}
