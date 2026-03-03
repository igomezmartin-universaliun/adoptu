package com.adoptu.services

import com.adoptu.mocks.MockEmailService
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailServiceTest {

    private lateinit var mockEmailService: MockEmailService

    @BeforeEach
    fun setup() {
        mockEmailService = MockEmailService()
    }

    @Test
    fun `sendEmail returns true and records email when successful`() = runBlocking {
        val result = mockEmailService.sendEmail(
            to = "mocks@example.com",
            subject = "Test Subject",
            body = "Test Body"
        )

        assertTrue(result)
        assertEquals(1, mockEmailService.getSentEmails().size)
        
        val sentEmail = mockEmailService.getSentEmails().first()
        assertEquals("mocks@example.com", sentEmail.to)
        assertEquals("Test Subject", sentEmail.subject)
        assertEquals("Test Body", sentEmail.body)
    }

    @Test
    fun `sendEmail returns false when fail mode is enabled`() = runBlocking {
        mockEmailService.setFailMode(true)

        val result = mockEmailService.sendEmail(
            to = "mocks@example.com",
            subject = "Test Subject",
            body = "Test Body"
        )

        assertFalse(result)
        assertTrue(mockEmailService.getSentEmails().isEmpty())
    }

    @Test
    fun `sendEmail can send multiple emails`() = runBlocking {
        mockEmailService.sendEmail("user1@example.com", "Subject 1", "Body 1")
        mockEmailService.sendEmail("user2@example.com", "Subject 2", "Body 2")
        mockEmailService.sendEmail("user3@example.com", "Subject 3", "Body 3")

        assertEquals(3, mockEmailService.getSentEmails().size)
    }

    @Test
    fun `clear removes all sent emails and resets fail mode`() {
        mockEmailService.setFailMode(true)
        
        runBlocking {
            mockEmailService.sendEmail("mocks@example.com", "Subject", "Body")
        }
        
        mockEmailService.clear()

        assertTrue(mockEmailService.getSentEmails().isEmpty())
        
        val result = runBlocking {
            mockEmailService.sendEmail("mocks@example.com", "Subject", "Body")
        }
        assertTrue(result)
    }

    @Test
    fun `sendAdoptionRequestNotification sends correct email`() = runBlocking {
        val result = mockEmailService.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Buddy",
            adopterName = "John Doe",
            message = "I would love to adopt Buddy!"
        )

        assertTrue(result)
        
        val sentEmail = mockEmailService.getSentEmails().first()
        assertEquals("rescuer@example.com", sentEmail.to)
        assertEquals("New Adoption Request for Buddy", sentEmail.subject)
        assertContains(sentEmail.body, "Buddy")
        assertContains(sentEmail.body, "John Doe")
    }

    @Test
    fun `sendAdoptionRequestNotification handles null message`() = runBlocking {
        val result = mockEmailService.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Max",
            adopterName = "Jane Smith",
            message = null
        )

        assertTrue(result)
        
        val sentEmail = mockEmailService.getSentEmails().first()
        assertContains(sentEmail.body, "Max")
        assertContains(sentEmail.body, "Jane Smith")
    }

    @Test
    fun `sendAdoptionRequestNotification handles blank message`() = runBlocking {
        val result = mockEmailService.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Luna",
            adopterName = "Alice",
            message = "   "
        )

        assertTrue(result)
        
        val sentEmail = mockEmailService.getSentEmails().first()
        assertContains(sentEmail.body, "Luna")
        assertContains(sentEmail.body, "Alice")
    }
}

class RealEmailServiceTest {

    @Test
    fun `EmailService returns false when not configured`() = runBlocking {
        val config = MapApplicationConfig()
        val emailService = EmailService(config)

        val result = emailService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test body"
        )

        assertFalse(result)
    }

    @Test
    fun `EmailService returns false when only host is configured`() = runBlocking {
        val config = MapApplicationConfig(
            "email.host" to "smtp.example.com"
        )
        val emailService = EmailService(config)

        val result = emailService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test body"
        )

        assertFalse(result)
    }

    @Test
    fun `EmailService returns false when only host and username are configured`() = runBlocking {
        val config = MapApplicationConfig(
            "email.host" to "smtp.example.com",
            "email.username" to "user"
        )
        val emailService = EmailService(config)

        val result = emailService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test body"
        )

        assertFalse(result)
    }

    @Test
    fun `EmailService uses default from address when not configured`() = runBlocking {
        val config = MapApplicationConfig(
            "email.host" to "smtp.example.com",
            "email.username" to "user",
            "email.password" to "pass"
        )
        val emailService = EmailService(config)

        val result = emailService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test body"
        )

        assertFalse(result)
    }

    @Test
    fun `EmailService uses configured from address`() = runBlocking {
        val config = MapApplicationConfig(
            "email.host" to "smtp.example.com",
            "email.username" to "user",
            "email.password" to "pass",
            "email.from" to "custom@adopt-u.com"
        )
        val emailService = EmailService(config)

        val result = emailService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test body"
        )

        assertFalse(result)
    }

    @Test
    fun `EmailService uses configured port`() = runBlocking {
        val config = MapApplicationConfig(
            "email.host" to "smtp.example.com",
            "email.username" to "user",
            "email.password" to "pass",
            "email.port" to "465"
        )
        val emailService = EmailService(config)

        val result = emailService.sendEmail(
            to = "test@example.com",
            subject = "Test",
            body = "Test body"
        )

        assertFalse(result)
    }

    @Test
    fun `sendAdoptionRequestNotification returns false when not configured`() = runBlocking {
        val config = MapApplicationConfig()
        val emailService = EmailService(config)

        val result = emailService.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Buddy",
            adopterName = "John",
            message = "Hello"
        )

        assertFalse(result)
    }

    @Test
    fun `sendAdoptionRequestNotification returns false with blank inputs when not configured`() = runBlocking {
        val config = MapApplicationConfig()
        val emailService = EmailService(config)

        val result = emailService.sendAdoptionRequestNotification(
            rescuerEmail = "",
            petName = "",
            adopterName = "",
            message = null
        )

        assertFalse(result)
    }
}
