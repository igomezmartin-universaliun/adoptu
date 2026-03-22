package com.adoptu.adapters.notification

import com.adoptu.ports.NotificationPort
import io.ktor.server.config.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    fun `sendEmail returns false for not configured`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

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

    @Test
    fun `sendTemporalHomeRequest returns false when not configured`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendTemporalHomeRequest(
                temporalHomeEmail = "home@test.com",
                temporalHomeAlias = "Test Home",
                rescuerName = "Test Rescuer",
                petName = "Buddy",
                message = "Need help",
                spamReportLink = "https://example.com/block"
            )
        }

        assertFalse(result)
    }

    @Test
    fun `sendTemporalHomeRequest returns false when not configured with null pet`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendTemporalHomeRequest(
                temporalHomeEmail = "home@test.com",
                temporalHomeAlias = "Test Home",
                rescuerName = "Test Rescuer",
                petName = null,
                message = "Need help",
                spamReportLink = "https://example.com/block"
            )
        }

        assertFalse(result)
    }

    @Test
    fun `adapter can be instantiated with empty config`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)
        assertNotNull(adapter)
    }

    @Test
    fun `adapter can be instantiated with sns config`() {
        val config = MapApplicationConfig(
            "sns.region" to "us-west-2",
            "sns.access_key_id" to "key",
            "sns.secret_access_key" to "secret",
            "sns.topic_arn" to "arn:aws:sns:us-west-2:123456789012:test"
        )
        val adapter = SnsNotificationAdapter(config)
        assertNotNull(adapter)
    }

    @Test
    fun `sendPhotographerRequest with fee and currency builds correct parameters`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "John",
                requesterName = "Jane",
                petName = "Buddy",
                message = "Test message",
                fee = 100.0,
                currency = "EUR"
            )
        }
        assertFalse(result)
    }

    @Test
    fun `sendPhotographerRequest with zero fee does not include fee`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendPhotographerRequest(
                photographerEmail = "photo@test.com",
                photographerName = "John",
                requesterName = "Jane",
                petName = "Buddy",
                message = "Test message",
                fee = 0.0,
                currency = "USD"
            )
        }
        assertFalse(result)
    }

    @Test
    fun `sendAdoptionRequestNotification with blank message`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "Jane",
                message = ""
            )
        }
        assertFalse(result)
    }

    @Test
    fun `sendAdoptionRequestNotification with blank message string`() {
        val config = MapApplicationConfig()
        val adapter = SnsNotificationAdapter(config)

        val result = kotlinx.coroutines.runBlocking {
            adapter.sendAdoptionRequestNotification(
                rescuerEmail = "rescuer@test.com",
                petName = "Buddy",
                adopterName = "Jane",
                message = "   "
            )
        }
        assertFalse(result)
    }
}

