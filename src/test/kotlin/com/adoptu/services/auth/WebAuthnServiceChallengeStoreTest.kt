package com.adoptu.services.auth

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WebAuthnServiceChallengeStoreTest {

    private lateinit var webAuthnService: WebAuthnService

    @BeforeEach
    fun setup() {
        webAuthnService = WebAuthnService(
            clock = com.adoptu.mocks.TestClock(kotlin.time.Instant.parse("2024-01-15T10:00:00Z")),
            emailVerificationService = io.mockk.mockk(relaxed = true),
            userService = io.mockk.mockk(relaxed = true),
            passwordService = io.mockk.mockk(relaxed = true),
            magicLinkService = io.mockk.mockk(relaxed = true),
            adminEmail = "admin@adopt-u.com",
            rpId = "localhost",
            rpName = "Adopt-U Pet Adoption",
            origin = "http://localhost:8080"
        )
    }

    @Test
    fun `generateRegistrationOptions returns valid options`() {
        val result = webAuthnService.generateRegistrationOptions("test@example.com", "Test User")

        assertNotNull(result)
        assertEquals("localhost", result.rp.id)
        assertEquals("Adopt-U Pet Adoption", result.rp.name)
        assertNotNull(result.user)
        assertEquals("test@example.com", result.user.name)
        assertEquals("Test User", result.user.displayName)
        assertTrue(result.challenge.isNotEmpty())
        assertNotNull(result.pubKeyCredParams)
        assertEquals(2, result.pubKeyCredParams.size)
        assertTrue(result.pubKeyCredParams.any { it.type == "public-key" && it.alg == -7 })
        assertTrue(result.pubKeyCredParams.any { it.type == "public-key" && it.alg == -257 })
    }

    @Test
    fun `generateAssertionOptions returns valid options`() {
        val result = webAuthnService.generateAssertionOptions()

        assertNotNull(result)
        assertTrue(result.challenge.isNotEmpty())
        assertEquals("localhost", result.rpId)
        assertEquals("required", result.userVerification)
    }

    @Nested
    inner class RelyingPartyDataClass {
        @Test
        fun `RelyingParty serializes correctly`() {
            val rp = WebAuthnService.RelyingParty(id = "test-rp-id", name = "Test RP")

            assertEquals("test-rp-id", rp.id)
            assertEquals("Test RP", rp.name)
        }

        @Test
        fun `RelyingParty equality works`() {
            val rp1 = WebAuthnService.RelyingParty(id = "id", name = "name")
            val rp2 = WebAuthnService.RelyingParty(id = "id", name = "name")
            val rp3 = WebAuthnService.RelyingParty(id = "other", name = "name")

            assertEquals(rp1, rp2)
            assertTrue(rp1 != rp3)
        }
    }

    @Nested
    inner class PublicKeyUserDataClass {
        @Test
        fun `PublicKeyUser serializes correctly`() {
            val user = WebAuthnService.PublicKeyUser(
                id = "user-id",
                name = "user@example.com",
                displayName = "Display Name"
            )

            assertEquals("user-id", user.id)
            assertEquals("user@example.com", user.name)
            assertEquals("Display Name", user.displayName)
        }
    }

    @Nested
    inner class PubKeyCredParamDataClass {
        @Test
        fun `PubKeyCredParam serializes correctly`() {
            val param = WebAuthnService.PubKeyCredParam(type = "public-key", alg = -7)

            assertEquals("public-key", param.type)
            assertEquals(-7, param.alg)
        }

        @Test
        fun `PubKeyCredParam equality works`() {
            val param1 = WebAuthnService.PubKeyCredParam(type = "public-key", alg = -7)
            val param2 = WebAuthnService.PubKeyCredParam(type = "public-key", alg = -7)
            val param3 = WebAuthnService.PubKeyCredParam(type = "public-key", alg = -257)

            assertEquals(param1, param2)
            assertTrue(param1 != param3)
        }
    }

    @Nested
    inner class RegistrationOptionsResponseDataClass {
        @Test
        fun `RegistrationOptionsResponse contains all fields`() {
            val response = WebAuthnService.RegistrationOptionsResponse(
                rp = WebAuthnService.RelyingParty(id = "rp-id", name = "RP Name"),
                user = WebAuthnService.PublicKeyUser(id = "user-id", name = "name", displayName = "Display"),
                challenge = "challenge-value",
                pubKeyCredParams = listOf(
                    WebAuthnService.PubKeyCredParam(type = "public-key", alg = -7)
                )
            )

            assertEquals("rp-id", response.rp.id)
            assertEquals("user-id", response.user.id)
            assertEquals("challenge-value", response.challenge)
            assertEquals(1, response.pubKeyCredParams.size)
        }
    }

    @Nested
    inner class AssertionOptionsResponseDataClass {
        @Test
        fun `AssertionOptionsResponse contains all fields`() {
            val response = WebAuthnService.AssertionOptionsResponse(
                challenge = "challenge-value",
                rpId = "rp-id",
                userVerification = "required"
            )

            assertEquals("challenge-value", response.challenge)
            assertEquals("rp-id", response.rpId)
            assertEquals("required", response.userVerification)
        }
    }

    @Nested
    inner class AuthenticatedUserDataClass {
        @Test
        fun `AuthenticatedUser serializes correctly`() {
            val user = WebAuthnService.AuthenticatedUser(
                id = 1,
                username = "user@example.com",
                displayName = "Display Name",
                role = "ADMIN"
            )

            assertEquals(1, user.id)
            assertEquals("user@example.com", user.username)
            assertEquals("Display Name", user.displayName)
            assertEquals("ADMIN", user.role)
        }
    }

    @Nested
    inner class AuthResultDataClass {
        @Test
        fun `AuthResult serializes correctly`() {
            val result = WebAuthnService.AuthResult(
                userId = 42,
                user = WebAuthnService.AuthenticatedUser(
                    id = 42,
                    username = "user@example.com",
                    displayName = "User",
                    role = "ADOPTER"
                )
            )

            assertEquals(42, result.userId)
            assertEquals(42, result.user.id)
            assertEquals("ADOPTER", result.user.role)
        }
    }

    @Nested
    inner class RegistrationResultDataClass {
        @Test
        fun `RegistrationResult serializes correctly`() {
            val result = WebAuthnService.RegistrationResult(
                userId = 100,
                emailSent = true
            )

            assertEquals(100, result.userId)
            assertTrue(result.emailSent)
        }

        @Test
        fun `RegistrationResult can have emailSent false`() {
            val result = WebAuthnService.RegistrationResult(
                userId = 101,
                emailSent = false
            )

            assertEquals(101, result.userId)
            assertTrue(!result.emailSent)
        }
    }
}
