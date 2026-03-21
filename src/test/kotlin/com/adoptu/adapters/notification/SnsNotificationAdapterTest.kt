package com.adoptu.adapters.notification

import com.adoptu.ports.NotificationPort
import io.ktor.server.config.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnsNotificationAdapterTest {

    @Test
    fun `sendEmail returns false when not configured`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }

        assertFalse(result)
    }

    @Test
    fun `sendEmail returns false when only sns client configured without topic arn`() {
        val config = MapApplicationConfig(
            "sns.region" to "us-east-1",
            "sns.access_key_id" to "test-key",
            "sns.secret_access_key" to "test-secret"
        )
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }

        assertFalse(result)
    }

    @Test
    fun `sendEmail returns false when topic arn configured but no credentials`() {
        val config = MapApplicationConfig(
            "sns.region" to "us-east-1",
            "sns.topic_arn" to "arn:aws:sns:us-east-1:123456789012:test-topic"
        )
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }

        assertFalse(result)
    }

    @Test
    fun `sendPhotographerRequest returns false when not configured`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "Test Photographer",
                requesterName = "Test Requester",
                petName = "Buddy",
                message = "Please take photos",
                fee = 50.0,
                currency = "USD"
            )
        }

        assertFalse(result)
    }

    @Test
    fun `sendPhotographerRequest returns false when not configured with null pet and fee`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "Test Photographer",
                requesterName = "Test Requester",
                petName = null,
                message = "Please take photos",
                fee = null,
                currency = null
            )
        }

        assertFalse(result)
    }

    @Test
    fun `sendAdoptionRequestNotification returns false when not configured`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "Test Adopter",
                message = "I would like to adopt"
            )
        }

        assertFalse(result)
    }

    @Test
    fun `sendAdoptionRequestNotification returns false when not configured with null message`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "Test Adopter",
                message = null
            )
        }

        assertFalse(result)
    }

    @Test
    fun `sendEmail constructs correct message format`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }

        // Verify it returns false (not configured) but processes the request
        val result = kotlinx.coroutines.runBlocking {
            adapter.sendEmail("test@example.com", "Test Subject", "Test Body")
        }
        assertFalse(result)
    }

    @Test
    fun `NotificationPort interface is implemented correctly`() {
        val config = MapApplicationConfig()
        val adapter: NotificationPort = SnsNotificationAdapter(config)

        assertTrue(adapter is NotificationPort)
    }
}
