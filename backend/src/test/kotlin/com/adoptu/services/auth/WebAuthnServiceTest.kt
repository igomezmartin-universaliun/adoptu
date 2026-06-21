package com.adoptu.services.auth

import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WebAuthnServiceTest {

    private lateinit var webAuthnService: WebAuthnService

    @BeforeEach
    fun setup() {
        webAuthnService = WebAuthnService(
            clock = com.adoptu.mocks.TestClock(kotlin.time.Instant.parse("2024-01-15T10:00:00Z")),
            emailVerificationService = mockk(relaxed = true),
            userService = mockk(relaxed = true),
            passwordService = mockk(relaxed = true),
            magicLinkService = mockk(relaxed = true),
            adminEmail = "admin@adopt-u.com",
            rpId = "localhost",
            rpName = "Adopt-U Pet Adoption",
            origins = listOf("http://localhost:80")
        )
    }

    @Nested
    inner class GenerateRegistrationOptions {
        @Test
        fun `generates valid registration options with correct structure`() {
            val result = webAuthnService.generateRegistrationOptions("test@example.com", "Test User")

            assertEquals("localhost", result.publicKey.rp.id)
            assertEquals("Adopt-U Pet Adoption", result.publicKey.rp.name)
            assertEquals("test@example.com", result.publicKey.user.name)
            assertEquals("Test User", result.publicKey.user.displayName)
            assertTrue(result.publicKey.challenge.isNotEmpty())
            assertEquals(2, result.publicKey.pubKeyCredParams.size)
        }

        @Test
        fun `includes ES256 and RS256 algorithms`() {
            val result = webAuthnService.generateRegistrationOptions("test@example.com", "Test User")

            val es256 = result.publicKey.pubKeyCredParams.find { it.alg == -7 }
            val rs256 = result.publicKey.pubKeyCredParams.find { it.alg == -257 }

            assertTrue(es256?.type == "public-key")
            assertTrue(rs256?.type == "public-key")
        }
    }

    @Nested
    inner class GenerateAssertionOptions {
        @Test
        fun `generates valid assertion options`() {
            val result = webAuthnService.generateAssertionOptions()

            assertEquals("localhost", result.rpId)
            assertEquals("required", result.userVerification)
            assertTrue(result.challenge.isNotEmpty())
        }
    }
}
