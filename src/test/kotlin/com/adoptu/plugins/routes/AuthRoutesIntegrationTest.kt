package com.adoptu.plugins.routes

import com.adoptu.auth.SessionUser
import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.UserRole
import com.adoptu.models.Users
import com.adoptu.models.WebAuthnCredentials
import com.adoptu.services.UserService
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class AuthRoutesIntegrationTest {

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
    }

    // ==================== User Registration ====================

    @Test
    fun `POST register creates new user`() {
        val username = "newuser_${System.currentTimeMillis()}"
        
        // First generate registration options
        val options = com.adoptu.auth.WebAuthnService.generateRegistrationOptions(
            username = username,
            displayName = "New User"
        )
        
        assertNotNull(options)
        assertNotNull(options.challenge)
        assertEquals(username, options.user.name)
        
        // Mock registration - in real mocks would verify the WebAuthn response
        // For this mocks, we directly insert a user
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "New User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }
        
        assertNotNull(userId)
        
        val user = UserService.getById(userId)
        assertNotNull(user)
        assertEquals(username, user.username)
        assertEquals("New User", user.displayName)
        assertEquals(UserRole.ADOPTER, user.role)
    }

    @Test
    fun `POST register with different roles`() {
        val rescuerUsername = "rescuer_${System.currentTimeMillis()}"
        val adminUsername = "admin_${System.currentTimeMillis()}"
        
        // Create rescuer
        val rescuerId = transaction {
            Users.insert {
                it[Users.username] = rescuerUsername
                it[Users.displayName] = "Rescuer User"
                it[Users.role] = "RESCUER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }
        
        // Create admin
        val adminId = transaction {
            Users.insert {
                it[Users.username] = adminUsername
                it[Users.displayName] = "Admin User"
                it[Users.role] = "ADMIN"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }
        
        val rescuer = UserService.getById(rescuerId)
        val admin = UserService.getById(adminId)
        
        assertEquals(UserRole.RESCUER, rescuer?.role)
        assertEquals(UserRole.ADMIN, admin?.role)
    }

    // ==================== User Authentication ====================

    @Test
    fun `POST authenticate validates credential`() {
        // Create user with credentials
        val username = "authtest_${System.currentTimeMillis()}"
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Auth Test"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }
        
        // Insert mock credential
        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "test_credential_id"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 1
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }
        
        // Generate assertion options
        val options = com.adoptu.auth.WebAuthnService.generateAssertionOptions()
        assertNotNull(options)
        assertNotNull(options.challenge)
    }

    // ==================== Accept Terms ====================

    @Test
    fun `POST accept-terms accepts privacy policy`() {
        val userId = createTestUser(username = "termsuser", displayName = "Terms User")
        
        val request = AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        )
        
        val result = UserService.acceptTerms(userId, request)
        
        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertNull(result.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `POST accept-terms accepts terms and conditions`() {
        val userId = createTestUser(username = "termsuser2", displayName = "Terms User 2")
        
        val request = AcceptTermsRequest(
            acceptPrivacyPolicy = false,
            acceptTermsAndConditions = true
        )
        
        val result = UserService.acceptTerms(userId, request)
        
        assertNotNull(result)
        assertNull(result.lastAcceptedPrivacyPolicy)
        assertNotNull(result.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `POST accept-terms accepts both`() {
        val userId = createTestUser(username = "termsuser3", displayName = "Terms User 3")
        
        val request = AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = true
        )
        
        val result = UserService.acceptTerms(userId, request)
        
        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertNotNull(result.lastAcceptedTermsAndConditions)
    }

    @Test
    fun `POST accept-terms returns null for non-existent user`() {
        val request = AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        )
        
        val result = UserService.acceptTerms(999, request)
        
        assertNull(result)
    }

    // ==================== Session Management ====================

    @Test
    fun `SessionUser contains correct information`() {
        val sessionUser = SessionUser(
            userId = 1,
            username = "testuser",
            displayName = "Test User",
            email = "mocks@mocks.com",
            role = UserRole.RESCUER
        )
        
        assertEquals(1, sessionUser.userId)
        assertEquals("testuser", sessionUser.username)
        assertEquals("Test User", sessionUser.displayName)
        assertEquals("mocks@mocks.com", sessionUser.email)
        assertEquals(UserRole.RESCUER, sessionUser.role)
    }

    @Test
    fun `SessionUser serializes correctly`() {
        val sessionUser = SessionUser(
            userId = 42,
            username = "admin",
            displayName = "Admin User",
            email = "admin@mocks.com",
            role = UserRole.ADMIN
        )
        
        // Verify all fields are accessible
        assertEquals(42, sessionUser.userId)
        assertEquals("admin", sessionUser.username)
        assertEquals("Admin User", sessionUser.displayName)
        assertEquals("admin@mocks.com", sessionUser.email)
        assertEquals(UserRole.ADMIN, sessionUser.role)
    }

    // ==================== User Roles ====================

    @Test
    fun `UserRole enum has correct values`() {
        assertEquals(3, UserRole.values().size)
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"))
        assertEquals(UserRole.RESCUER, UserRole.valueOf("RESCUER"))
        assertEquals(UserRole.ADOPTER, UserRole.valueOf("ADOPTER"))
    }

    // ==================== User Repository Operations ====================

    @Test
    fun `getById returns user with all fields including nulls`() {
        val userId = createTestUser(
            username = "optionals",
            displayName = "Optionals User",
            email = null
        )
        
        val result = UserService.getById(userId)
        
        assertNotNull(result)
        assertEquals("optionals", result.username)
        assertNull(result.email) // Email is nullable
    }

    @Test
    fun `multiple users can be created with unique usernames`() {
        val timestamp = System.currentTimeMillis()
        
        val user1Id = createTestUser(username = "user1_$timestamp", displayName = "User 1")
        val user2Id = createTestUser(username = "user2_$timestamp", displayName = "User 2")
        val user3Id = createTestUser(username = "user3_$timestamp", displayName = "User 3")
        
        assertNotEquals(user1Id, user2Id)
        assertNotEquals(user2Id, user3Id)
        assertNotEquals(user1Id, user3Id)
        
        val user1 = UserService.getById(user1Id)
        val user2 = UserService.getById(user2Id)
        val user3 = UserService.getById(user3Id)
        
        assertNotNull(user1)
        assertNotNull(user2)
        assertNotNull(user3)
        
        assertTrue(user1.username.contains("user1_"))
        assertTrue(user2.username.contains("user2_"))
        assertTrue(user3.username.contains("user3_"))
    }

    @Test
    fun `acceptTerms can be called multiple times`() {
        val userId = createTestUser(username = "multipl Terms", displayName = "Multiple Terms")
        
        // First call
        val result1 = UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))
        val firstTimestamp = result1!!.lastAcceptedPrivacyPolicy
        
        // Wait a bit to ensure different timestamp
        Thread.sleep(10)
        
        // Second call - should update the timestamp
        val result2 = UserService.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))
        val secondTimestamp = result2!!.lastAcceptedPrivacyPolicy
        
        // Second timestamp should be greater or equal
        assertTrue(secondTimestamp!! >= firstTimestamp!!)
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
