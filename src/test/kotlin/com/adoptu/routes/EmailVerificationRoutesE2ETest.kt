package com.adoptu.routes

import com.adoptu.adapters.db.EmailVerificationTokens
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.services.EmailVerificationService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
private data class RegistrationResponse(val success: Boolean, val message: String?, val emailVerificationSent: Boolean)

@Serializable
private data class VerificationResponse(val success: Boolean, val message: String?)

@OptIn(ExperimentalTime::class)
class EmailVerificationRoutesE2ETest {

    private val clock = Clock.System
    private lateinit var mockNotificationAdapter: MockNotificationAdapter

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        TestDatabase.clearAllData()
        mockNotificationAdapter = MockNotificationAdapter()
    }

    private fun TestApplicationBuilder.setupApp() {
        val config = MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to "8080",
            "admin.email" to "admin@test.com"
        )

        val testModules = module {
            single<ApplicationConfig> { config }
            single<kotlin.time.Clock> { kotlin.time.Clock.System }
            single { com.adoptu.services.auth.WebAuthnService(get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:8080") }
            single { MockImageStorage() }
            single { mockNotificationAdapter }
            single<com.adoptu.ports.NotificationPort> { mockNotificationAdapter }
            single<com.adoptu.ports.PetRepositoryPort> { com.adoptu.adapters.db.repositories.PetRepositoryImpl(get()) }
            single<com.adoptu.ports.UserRepositoryPort> { UserRepository(get()) }
            single<com.adoptu.ports.PhotographerRepositoryPort> { com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl(get(), get(), get()) }
            single { com.adoptu.services.PhotographerService(get(), get(), get(), get()) }
            single { com.adoptu.services.UserService(get()) }
            single { com.adoptu.services.PetService(get(), get(), get(), get()) }
            single { EmailVerificationService(get(), get(), get()) }
        }

        environment {
            this.config = config
        }

        application {
            configureSerialization()
            configureSessions()
            install(Koin) {
                modules(testModules)
            }
            routing {
                authRoutes()
            }
        }
    }

    @Test
    fun `GET verify-email returns success for valid token`() {
        val userId = createVerifiedUser()

        val token = createValidToken(userId)

        testApplication {
            setupApp()

            val response = client.get("/api/auth/verify-email?token=$token")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertTrue(body.success)
            assertEquals("Email verified successfully. You can now login.", body.message)
        }
    }

    @Test
    fun `GET verify-email returns error for invalid token`() {
        testApplication {
            setupApp()

            val response = client.get("/api/auth/verify-email?token=invalid-token")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Invalid or expired token", body.message)
        }
    }

    @Test
    fun `GET verify-email returns error when token is missing`() {
        testApplication {
            setupApp()

            val response = client.get("/api/auth/verify-email")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Token is required", body.message)
        }
    }

    @Test
    fun `POST resend-verification returns Unauthorized when not authenticated`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/resend-verification")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    private fun createVerifiedUser(): Int {
        return transaction {
            Users.insert {
                it[Users.username] = "test@test.com"
                it[Users.displayName] = "Test User"
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
                it[Users.isEmailVerified] = true
            } get Users.id
        }!!
    }

    private fun createValidToken(userId: Int): String {
        val token = "valid-test-token-${clock.now().toEpochMilliseconds()}"
        transaction {
            EmailVerificationTokens.insert {
                it[EmailVerificationTokens.userId] = userId
                it[EmailVerificationTokens.token] = token
                it[EmailVerificationTokens.expiresAt] = clock.now().toEpochMilliseconds() + 86400000
                it[EmailVerificationTokens.createdAt] = clock.now().toEpochMilliseconds()
            }
        }
        return token
    }
}
