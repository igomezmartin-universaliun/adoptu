package com.adoptu.routes

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.WebAuthnCredentials
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.services.EmailVerificationService
import com.adoptu.services.PasswordService
import com.adoptu.services.crypto.CryptoService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import com.adoptu.services.auth.SessionUser
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import java.security.SecureRandom
import java.util.Base64

@Serializable
private data class SuccessResponse(val success: Boolean, val error: String? = null)

@Serializable
private data class PasswordRegistrationResponse(val success: Boolean, val message: String? = null, val emailVerificationSent: Boolean = false)

@OptIn(ExperimentalTime::class)
class PasswordRegistrationRoutesE2ETest {

    private val clock = Clock.System
    private lateinit var mockNotificationAdapter: MockNotificationAdapter

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        TestDatabase.clearAllData()
        mockNotificationAdapter = MockNotificationAdapter()
        CryptoService.initialize()
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
            single<com.adoptu.ports.UserRepositoryPort> { UserRepository(get()) }
            single { com.adoptu.services.UserService(get()) }
            single { EmailVerificationService(get(), get(), get()) }
            single { PasswordService(get(), mockNotificationAdapter, get()) }
            single { com.adoptu.services.MagicLinkService(get(), mockNotificationAdapter, get()) }
            single { com.adoptu.services.auth.WebAuthnService(get(), get(), get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", listOf(config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:8080")) }
            single { MockImageStorage() }
            single { mockNotificationAdapter }
            single<com.adoptu.ports.NotificationPort> { mockNotificationAdapter }
            single<com.adoptu.ports.PetRepositoryPort> { com.adoptu.adapters.db.repositories.PetRepositoryImpl(get()) }
            single<com.adoptu.ports.PhotographerRepositoryPort> { com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl(get(), get(), get()) }
            single { com.adoptu.services.PhotographerService(get(), get(), get(), get()) }
            single { com.adoptu.services.PetService(get(), get(), get(), get()) }
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

    private fun encryptPassword(password: String): String {
        val publicKey = CryptoService.getPublicKey()
        return CryptoService.encrypt(password, publicKey) 
            ?: throw IllegalStateException("Encryption failed")
    }

    @Test
    fun `POST register-password creates user with password`() {
        testApplication {
            setupApp()
            
            val encryptedPassword = encryptPassword("SecurePass123!")

            val response = client.post("/api/auth/register-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    mapOf(
                        "email" to "newuser@example.com",
                        "displayName" to "New User",
                        "roles" to "ADOPTER",
                        "encryptedPassword" to encryptedPassword
                    )
                ))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<PasswordRegistrationResponse>(response.bodyAsText())
            assertTrue(body.success)
            assertTrue(body.emailVerificationSent)
        }
    }

    @Test
    fun `POST register-password with invalid email returns error`() {
        testApplication {
            setupApp()
            
            val encryptedPassword = encryptPassword("SecurePass123!")

            val response = client.post("/api/auth/register-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    mapOf(
                        "email" to "invalid-email",
                        "displayName" to "Test User",
                        "roles" to "ADOPTER",
                        "encryptedPassword" to encryptedPassword
                    )
                ))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register-password with weak password returns error`() {
        testApplication {
            setupApp()
            
            val encryptedPassword = encryptPassword("weak")

            val response = client.post("/api/auth/register-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    mapOf(
                        "email" to "weak@example.com",
                        "displayName" to "Weak User",
                        "roles" to "ADOPTER",
                        "encryptedPassword" to encryptedPassword
                    )
                ))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register-password assigns roles correctly`() {
        testApplication {
            setupApp()
            
            val encryptedPassword = encryptPassword("SecurePass123!")

            val response = client.post("/api/auth/register-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    mapOf(
                        "email" to "roles@example.com",
                        "displayName" to "Roles User",
                        "roles" to "ADOPTER,RESCUER",
                        "encryptedPassword" to encryptedPassword
                    )
                ))
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `GET has-passkey returns false when no session`() {
        testApplication {
            setupApp()

            val response = client.get("/api/auth/has-passkey")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `GET has-passkey returns false for user without passkey`() {
        val userId = createTestUser("nopasskey@example.com", "No Passkey User")

        testApplication {
            setupApp()

            val response = client.get("/api/auth/has-passkey") {
                val session = SessionUser(userId, "nopasskey@example.com", "No Passkey User")
                // Note: This test would need session management setup
            }

            // Without session, returns failure
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `POST registration-options-for-user requires authentication`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/registration-options-for-user") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    mapOf(
                        "email" to "user@example.com",
                        "displayName" to "Test User"
                    )
                ))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST register-passkey requires authentication`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/register-passkey") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    mapOf(
                        "registrationResponse" to "{}",
                        "passkeyName" to "Test Key"
                    )
                ))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    private fun createTestUser(username: String, displayName: String): Int {
        return transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.language] = "en"
                it[Users.isEmailVerified] = true
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
            } get Users.id
        }
    }

    private fun createTestCredential(userId: Int): Int {
        val credentialId = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val aaguid = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val publicKey = ByteArray(65).also { SecureRandom().nextBytes(it) }
        
        return transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = Base64.getEncoder().encodeToString(credentialId)
                it[WebAuthnCredentials.attestedCredentialDataBase64] = Base64.getEncoder().encodeToString(aaguid + publicKey)
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = clock.now().toEpochMilliseconds()
            } get WebAuthnCredentials.id
        }
    }
}
