package com.adoptu.services

import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestDatabase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmailVerificationServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var mockNotificationAdapter: MockNotificationAdapter
    private lateinit var emailVerificationService: EmailVerificationService

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        userRepository = UserRepository()
        mockNotificationAdapter = MockNotificationAdapter()
        emailVerificationService = EmailVerificationService(userRepository, mockNotificationAdapter)
    }

    @Test
    fun `generateAndSendVerificationEmail creates token and sends email`() = runBlocking {
        val userId = createTestUser("test@example.com", "Test User")
        mockNotificationAdapter.clear()

        val result = emailVerificationService.generateAndSendVerificationEmail(userId, "test@example.com", "Test User")

        assertTrue(result)
        val sentEmails = mockNotificationAdapter.getSentEmails()
        assertEquals(1, sentEmails.size)
        assertEquals("test@example.com", sentEmails[0].to)
        assertEquals("Verify your email - Adopt-U", sentEmails[0].subject)
        assertTrue(sentEmails[0].body.contains("Test User"))
        assertTrue(sentEmails[0].body.contains("api/auth/verify-email?token="))
    }

    @Test
    fun `generateAndSendVerificationEmail returns false when email fails`() = runBlocking {
        val userId = createTestUser("test@example.com", "Test User")
        mockNotificationAdapter.setFailMode(true)

        val result = emailVerificationService.generateAndSendVerificationEmail(userId, "test@example.com", "Test User")

        assertFalse(result)
    }

    @Test
    fun `verifyToken marks user as verified`() = runBlocking {
        val userId = createTestUser("test@example.com", "Test User")
        mockNotificationAdapter.clear()
        emailVerificationService.generateAndSendVerificationEmail(userId, "test@example.com", "Test User")

        val sentEmail = mockNotificationAdapter.getSentEmails().first()
        val token = extractTokenFromEmail(sentEmail.body)

        val result = emailVerificationService.verifyToken(token)

        assertTrue(result)
        assertTrue(emailVerificationService.isUserVerified(userId))
    }

    @Test
    fun `verifyToken returns false for invalid token`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = emailVerificationService.verifyToken("invalid-token")

        assertFalse(result)
        assertFalse(emailVerificationService.isUserVerified(userId))
    }

    @Test
    fun `verifyToken returns false for empty token`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = emailVerificationService.verifyToken("")

        assertFalse(result)
        assertFalse(emailVerificationService.isUserVerified(userId))
    }

    @Test
    fun `isUserVerified returns false for unverified user`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = emailVerificationService.isUserVerified(userId)

        assertFalse(result)
    }

    @Test
    fun `isUserVerified returns true for verified user`() {
        val userId = createTestUser("test@example.com", "Test User")
        userRepository.setEmailVerified(userId, true)

        val result = emailVerificationService.isUserVerified(userId)

        assertTrue(result)
    }

    @Test
    fun `resendVerificationEmail deletes old tokens and creates new one`() = runBlocking {
        val userId = createTestUser("test@example.com", "Test User")
        mockNotificationAdapter.clear()
        
        emailVerificationService.generateAndSendVerificationEmail(userId, "test@example.com", "Test User")
        val oldEmailCount = mockNotificationAdapter.getSentEmails().size

        emailVerificationService.resendVerificationEmail(userId, "test@example.com", "Test User")
        
        assertEquals(2, mockNotificationAdapter.getSentEmails().size)
    }

    @Test
    fun `verifyToken deletes token after successful verification`() = runBlocking {
        val userId = createTestUser("test@example.com", "Test User")
        mockNotificationAdapter.clear()
        emailVerificationService.generateAndSendVerificationEmail(userId, "test@example.com", "Test User")

        val sentEmail = mockNotificationAdapter.getSentEmails().first()
        val token = extractTokenFromEmail(sentEmail.body)

        emailVerificationService.verifyToken(token)
        
        emailVerificationService.resendVerificationEmail(userId, "test@example.com", "Test User")
        
        val newEmails = mockNotificationAdapter.getSentEmails()
        assertEquals(2, newEmails.size)
    }

    @Test
    fun `generateAndSendVerificationEmail returns false for non-existent user`() = runBlocking {
        mockNotificationAdapter.clear()

        val result = emailVerificationService.generateAndSendVerificationEmail(999, "test@example.com", "Test User")

        assertFalse(result)
    }

    @Test
    fun `isUserVerified returns false for non-existent user`() {
        val result = emailVerificationService.isUserVerified(999)

        assertFalse(result)
    }

    @Test
    fun `setEmailVerified updates verification status`() {
        val userId = createTestUser("test@example.com", "Test User")
        assertFalse(emailVerificationService.isUserVerified(userId))

        userRepository.setEmailVerified(userId, true)
        assertTrue(emailVerificationService.isUserVerified(userId))

        userRepository.setEmailVerified(userId, false)
        assertFalse(emailVerificationService.isUserVerified(userId))
    }

    private fun createTestUser(username: String, displayName: String): Int {
        return transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = System.currentTimeMillis()
                it[Users.isEmailVerified] = false
            } get Users.id
        }!!
    }

    private fun extractTokenFromEmail(body: String): String {
        val regex = Regex("token=([^\\s]+)")
        val match = regex.find(body)
        return match?.groupValues?.get(1) ?: ""
    }
}
