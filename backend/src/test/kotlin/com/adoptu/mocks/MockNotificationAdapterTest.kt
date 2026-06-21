package com.adoptu.mocks

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockNotificationAdapterTest {

    private lateinit var adapter: MockNotificationAdapter

    @BeforeEach
    fun setup() {
        adapter = MockNotificationAdapter()
    }

    @Test
    fun `sendEmail adds email to sent list`() {
        val result = kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }

        assertTrue(result)
        assertEquals(1, adapter.getSentEmails().size)
        val email = adapter.getSentEmails().first()
        assertEquals("test@example.com", email.to)
        assertEquals("Test Subject", email.subject)
        assertEquals("Test Body", email.body)
    }

    @Test
    fun `sendEmail returns false in fail mode`() {
        adapter.setFailMode(true)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }

        assertFalse(result)
        assertTrue(adapter.getSentEmails().isEmpty())
    }

    @Test
    fun `sendEmail multiple emails tracks all`() {
        kotlinx.coroutines.runBlocking {
            adapter.sendEmail("user1@example.com", "Subject 1", "Body 1")
            adapter.sendEmail("user2@example.com", "Subject 2", "Body 2")
            adapter.sendEmail("user3@example.com", "Subject 3", "Body 3")
        }

        assertEquals(3, adapter.getSentEmails().size)
    }

    @Test
    fun `clear removes all emails and resets fail mode`() {
        adapter.setFailMode(true)
        kotlinx.coroutines.runBlocking {
            adapter.sendEmail("user1@example.com", "Subject 1", "Body 1")
        }

        adapter.clear()

        assertTrue(adapter.getSentEmails().isEmpty())
    }

    @Test
    fun `sendAdoptionRequestNotification sends correct email`() {
        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "John Doe",
                message = "I would love to adopt this pet"
            )
        }

        assertTrue(result)
        val emails = adapter.getSentEmails()
        assertEquals(1, emails.size)
        assertEquals("rescuer@test.com", emails.first().to)
        assertTrue(emails.first().subject.contains("Buddy"))
        assertTrue(emails.first().body.contains("John Doe"))
    }

    @Test
    fun `sendAdoptionRequestNotification with null message`() {
        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "John Doe",
                message = null
            )
        }

        assertTrue(result)
        val emails = adapter.getSentEmails()
        assertEquals(1, emails.size)
    }

    @Test
    fun `sendPhotographerRequest sends correct email`() {
        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "John Photographer",
                requesterName = "Jane Requester",
                petName = "Buddy",
                message = "Please take photos",
                fee = 50.0,
                currency = "USD"
            )
        }

        assertTrue(result)
        val emails = adapter.getSentEmails()
        assertEquals(1, emails.size)
        assertEquals("photo@test.com", emails.first().to)
        assertTrue(emails.first().subject.contains("Photography Session Request"))
        assertTrue(emails.first().body.contains("Jane Requester"))
    }

    @Test
    fun `sendPhotographerRequest with null pet and fee`() {
        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "John Photographer",
                requesterName = "Jane Requester",
                petName = null,
                message = "Please take photos",
                fee = null,
                currency = null
            )
        }

        assertTrue(result)
        val emails = adapter.getSentEmails()
        assertEquals(1, emails.size)
    }

    @Test
    fun `sendPhotographerRequest in fail mode returns false`() {
        adapter.setFailMode(true)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "John Photographer",
                requesterName = "Jane Requester",
                petName = "Buddy",
                message = "Please take photos",
                fee = 50.0,
                currency = "USD"
            )
        }

        assertFalse(result)
        assertTrue(adapter.getSentEmails().isEmpty())
    }

    @Test
    fun `sendAdoptionRequestNotification in fail mode returns false`() {
        adapter.setFailMode(true)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "John Doe",
                message = "Test message"
            )
        }

        assertFalse(result)
        assertTrue(adapter.getSentEmails().isEmpty())
    }
}
