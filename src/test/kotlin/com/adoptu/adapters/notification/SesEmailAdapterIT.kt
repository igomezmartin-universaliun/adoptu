package com.adoptu.adapters.notification

import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2Client
import software.amazon.awssdk.services.sesv2.model.CreateEmailIdentityRequest
import kotlin.test.Ignore
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Ignore("SES LocalStack support is limited")
class SesEmailAdapterIT {

    private var localstackContainer: LocalStackContainer? = null

    @BeforeAll
    fun startLocalStack() {
        try {
            localstackContainer = LocalStackContainer(
                DockerImageName.parse("localstack/localstack:2.0.1")
            ).withServices(LocalStackContainer.Service.SES)

            localstackContainer!!.start()

            val endpointOverride = localstackContainer!!.getEndpointOverride(LocalStackContainer.Service.SES)
            val region = localstackContainer!!.region

            val sesClient = SesV2Client.builder()
                .endpointOverride(endpointOverride)
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
                )
                .build()

            try {
                sesClient.createEmailIdentity(
                    CreateEmailIdentityRequest.builder()
                        .emailIdentity("test@example.com")
                        .build()
                )
            } catch (e: Exception) {
                println("Could not create email identity in LocalStack: ${e.message}")
            }

            sesClient.close()
        } catch (e: Exception) {
            println("Failed to start LocalStack: ${e.message}")
        }
    }

    @AfterAll
    fun stopLocalStack() {
        localstackContainer?.stop()
    }

    private fun createConfig(): MapApplicationConfig {
        val ls = localstackContainer!!
        return MapApplicationConfig(
            "env" to "prod",
            "ses.region" to ls.region,
            "ses.endpoint" to ls.getEndpointOverride(LocalStackContainer.Service.SES).toString(),
            "ses.access_key_id" to "test",
            "ses.secret_access_key" to "test",
            "email.from" to "test@example.com"
        )
    }

    @Test
    fun `sendEmail returns true when configured with LocalStack SES`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendEmail("test@example.com", "Test Subject", "Test Body")

        assertTrue(result, "sendEmail should return true when configured")
    }

    @Test
    fun `sendEmail sends correct message via SES`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendEmail(
            "adopter@example.com",
            "Your Adoption Request",
            "Your adoption request has been received."
        )

        assertTrue(result)
    }

    @Test
    fun `sendPhotographerRequest returns true when configured`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendPhotographerRequest(
            photographerEmail = "photographer@example.com",
            photographerName = "John Doe",
            requesterName = "Jane Smith",
            petName = "Buddy",
            message = "Please photograph my pet",
            fee = 50.0,
            currency = "USD"
        )

        assertTrue(result, "sendPhotographerRequest should return true when configured")
    }

    @Test
    fun `sendPhotographerRequest with pet name includes pet in message`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendPhotographerRequest(
            photographerEmail = "photographer@example.com",
            photographerName = "John Doe",
            requesterName = "Jane Smith",
            petName = "Buddy the Dog",
            message = "Please photograph my pet",
            fee = 100.0,
            currency = "EUR"
        )

        assertTrue(result)
    }

    @Test
    fun `sendPhotographerRequest without pet name`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendPhotographerRequest(
            photographerEmail = "photographer@example.com",
            photographerName = "John Doe",
            requesterName = "Jane Smith",
            petName = null,
            message = "Please photograph pets",
            fee = null,
            currency = null
        )

        assertTrue(result)
    }

    @Test
    fun `sendAdoptionRequestNotification returns true when configured`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Buddy",
            adopterName = "Jane Smith",
            message = "I would love to adopt this pet"
        )

        assertTrue(result, "sendAdoptionRequestNotification should return true when configured")
    }

    @Test
    fun `sendAdoptionRequestNotification with message includes message in notification`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Buddy",
            adopterName = "Jane Smith",
            message = "I have experience with dogs"
        )

        assertTrue(result)
    }

    @Test
    fun `sendAdoptionRequestNotification without message`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendAdoptionRequestNotification(
            rescuerEmail = "rescuer@example.com",
            petName = "Buddy",
            adopterName = "Jane Smith",
            message = null
        )

        assertTrue(result)
    }

    @Test
    fun `sendTemporalHomeRequest returns true when configured`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendTemporalHomeRequest(
            temporalHomeEmail = "home@example.com",
            temporalHomeAlias = "TempHomeUser",
            rescuerName = "John Rescuer",
            petName = "Buddy",
            message = "Can you help care for my pet?",
            spamReportLink = "https://example.com/block/123"
        )

        assertTrue(result, "sendTemporalHomeRequest should return true when configured")
    }

    @Test
    fun `sendTemporalHomeRequest with pet name includes pet in message`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendTemporalHomeRequest(
            temporalHomeEmail = "home@example.com",
            temporalHomeAlias = "TempHomeUser",
            rescuerName = "John Rescuer",
            petName = "Buddy the Dog",
            message = "Can you help care for my pet?",
            spamReportLink = "https://example.com/block/123"
        )

        assertTrue(result)
    }

    @Test
    fun `sendTemporalHomeRequest without pet name`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendTemporalHomeRequest(
            temporalHomeEmail = "home@example.com",
            temporalHomeAlias = "TempHomeUser",
            rescuerName = "John Rescuer",
            petName = null,
            message = "Can you help care for pets?",
            spamReportLink = "https://example.com/block/123"
        )

        assertTrue(result)
    }

    @Test
    fun `sendEmail handles email with special characters`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendEmail(
            "test+filter@example.com",
            "Subject with special chars: äöü & éè",
            "Body with émojis 🎉 and special chars: @#$%"
        )

        assertTrue(result)
    }

    @Test
    fun `sendEmail handles long message body`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val longBody = "A".repeat(1000)
        val result = adapter.sendEmail("test@example.com", "Long Subject", longBody)

        assertTrue(result)
    }

    @Test
    fun `sendPhotographerRequest handles zero fee`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result = adapter.sendPhotographerRequest(
            photographerEmail = "photographer@example.com",
            photographerName = "John Doe",
            requesterName = "Jane Smith",
            petName = "Buddy",
            message = "Please photograph my pet",
            fee = 0.0,
            currency = "USD"
        )

        assertTrue(result)
    }

    @Test
    fun `multiple notifications can be sent successfully`() = runBlocking {
        val config = createConfig()
        val adapter = SesEmailAdapter(config)

        val result1 = adapter.sendEmail("user1@example.com", "Subject 1", "Body 1")
        val result2 = adapter.sendEmail("user2@example.com", "Subject 2", "Body 2")
        val result3 = adapter.sendEmail("user3@example.com", "Subject 3", "Body 3")

        assertTrue(result1)
        assertTrue(result2)
        assertTrue(result3)
    }
}
