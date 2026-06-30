package com.adoptu.routes

import com.adoptu.adapters.db.EmailVerificationAttempts
import com.adoptu.adapters.db.EmailVerificationTokens
import com.adoptu.adapters.db.MagicLinkTokens
import com.adoptu.adapters.db.PasswordResetTokens
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.UserPasswords
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.output.AuthMeResponse
import com.adoptu.dto.output.RegistrationResponse
import com.adoptu.dto.output.SuccessWithErrorResponse
import com.adoptu.dto.output.VerificationResponse
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.services.EmailVerificationService
import com.adoptu.services.crypto.CryptoService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Covers endpoints/branches in AuthRoutes.kt not already exercised by
 * PasswordRegistrationRoutesE2ETest and EmailVerificationRoutesE2ETest:
 * registration-options, register, has-passkey (authenticated), registration-options-for-user
 * (authenticated), register-passkey (authenticated), resend-verification (all branches),
 * assertion-options, authenticate, logout, me, request-magic-link, magic-link-login,
 * login-with-password, forgot-password, reset-password, encryption-key.
 */
@OptIn(ExperimentalTime::class)
class AuthRoutesE2ETest {

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
            "ktor.deployment.port" to "80",
            "admin.email" to "admin@test.com"
        )

        val testModules = module {
            single<ApplicationConfig> { config }
            single<kotlin.time.Clock> { kotlin.time.Clock.System }
            single<com.adoptu.ports.UserRepositoryPort> { UserRepository(get()) }
            single { com.adoptu.services.UserService(get()) }
            single { EmailVerificationService(get(), get(), get(), "http://localhost:80") }
            single { com.adoptu.services.PasswordService(get(), mockNotificationAdapter, get(), "http://localhost:80") }
            single { com.adoptu.services.MagicLinkService(get(), mockNotificationAdapter, get(), "http://localhost:80", get()) }
            single { com.adoptu.services.auth.WebAuthnService(get(), get(), get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption",             listOf(config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:80")) }
            single { com.adoptu.services.validation.AuthValidationService() }
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

    private fun encryptValue(value: String): String {
        val publicKey = CryptoService.getPublicKey()
        return CryptoService.encrypt(value, publicKey) ?: throw IllegalStateException("Encryption failed")
    }

    // ==================== Helpers: real registration / login flows ====================

    private suspend fun ApplicationTestBuilder.registerUnverifiedUser(
        email: String,
        displayName: String = "Test User",
        password: String = "SecurePass123!",
        roles: String = "ADOPTER"
    ): Int {
        val response = client.post("/api/auth/register-password") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(mapOf(
                "email" to email,
                "displayName" to displayName,
                "roles" to roles,
                "encryptedPassword" to encryptValue(password)
            )))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return transaction { Users.selectAll().where { Users.username eq email }.first()[Users.id] }
    }

    private suspend fun ApplicationTestBuilder.registerVerifiedUser(
        email: String,
        displayName: String = "Test User",
        password: String = "SecurePass123!",
        roles: String = "ADOPTER"
    ): Int {
        val userId = registerUnverifiedUser(email, displayName, password, roles)
        transaction {
            Users.update({ Users.id eq userId }) { it[Users.isEmailVerified] = true }
        }
        return userId
    }

    private suspend fun ApplicationTestBuilder.loginAndGetCookie(email: String, password: String): String {
        val response = client.post("/api/auth/login-with-password") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue(password))))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val setCookie = response.headers[HttpHeaders.SetCookie] ?: error("Missing Set-Cookie header on login response")
        return setCookie.substringBefore(";")
    }

    // ==================== POST /api/auth/registration-options ====================

    @Test
    fun `POST registration-options returns 400 when email missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("displayName" to "Name").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST registration-options returns 400 when displayName missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "new@example.com").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST registration-options returns localized error for invalid email format`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "not-an-email", "displayName" to "Name", "language" to "es").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("formato de correo"))
        }
    }

    @Test
    fun `POST registration-options returns options for new user`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "brandnew@example.com", "displayName" to "Brand New").formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("challenge"))
        }
    }

    @Test
    fun `POST registration-options returns localized already-registered error for verified user`() {
        val email = "verified-dup@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)

            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to email, "displayName" to "Name", "language" to "fr").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("déjà enregistré"))
        }
    }

    @Test
    fun `POST registration-options resends verification and returns localized message for unverified user`() {
        val email = "unverified-dup@example.com"
        testApplication {
            setupApp()
            registerUnverifiedUser(email)

            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to email, "displayName" to "Name", "language" to "pt").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("e-mail de verificação enviado"))
        }
    }

    // ==================== POST /api/auth/register ====================

    @Test
    fun `POST register returns 400 when email missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("displayName" to "Name", "registrationResponse" to "{}").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register returns 400 when displayName missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "a@b.com", "registrationResponse" to "{}").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register returns 400 for invalid email format`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "bad-email", "displayName" to "Name", "registrationResponse" to "{}").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register returns 400 when registrationResponse missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "a@b.com", "displayName" to "Name").formUrlEncode())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register with default roles fails gracefully for invalid attestation`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to "garbage1@example.com", "displayName" to "Name", "registrationResponse" to "not-real-json").formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<RegistrationResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Registration failed", body.message)
        }
    }

    @Test
    fun `POST register parses explicit roles list for invalid attestation`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf(
                    "email" to "garbage2@example.com",
                    "displayName" to "Name",
                    "roles" to "ADOPTER,RESCUER",
                    "registrationResponse" to "not-real-json"
                ).formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<RegistrationResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `POST register adds ADMIN role for admin email with invalid attestation`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf(
                    "email" to "admin@test.com",
                    "displayName" to "Admin",
                    "registrationResponse" to "not-real-json"
                ).formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<RegistrationResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    // ==================== POST /api/auth/register-password (additional branches) ====================

    @Test
    fun `POST register-password defaults roles to ADOPTER when roles field is absent`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(mapOf(
                    "email" to "noroles@example.com",
                    "displayName" to "No Roles",
                    "encryptedPassword" to encryptValue("SecurePass123!")
                )))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `POST register-password adds ADMIN role for admin email`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/register-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(mapOf(
                    "email" to "admin@test.com",
                    "displayName" to "Admin",
                    "roles" to "ADOPTER",
                    "encryptedPassword" to encryptValue("SecurePass123!")
                )))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val userId = transaction { Users.selectAll().where { Users.username eq "admin@test.com" }.first()[Users.id] }
            val roles = transaction {
                UserActiveRoles.selectAll().where { UserActiveRoles.userId eq userId }.map { it[UserActiveRoles.role] }
            }
            assertTrue(roles.contains("ADMIN"))
        }
    }

    // ==================== GET /api/auth/has-passkey (authenticated) ====================

    @Test
    fun `GET has-passkey returns false for authenticated user without passkey`() {
        val email = "haspasskey@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.get("/api/auth/has-passkey") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    // ==================== POST /api/auth/registration-options-for-user ====================

    @Test
    fun `POST registration-options-for-user returns options with explicit email and displayName`() {
        val email = "optsforuser@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.post("/api/auth/registration-options-for-user") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(mapOf("email" to email, "displayName" to "Explicit Name")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("challenge"))
        }
    }

    @Test
    fun `POST registration-options-for-user falls back to session email and displayName`() {
        val email = "optsforuserfallback@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.post("/api/auth/registration-options-for-user") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("challenge"))
        }
    }

    // ==================== POST /api/auth/register-passkey (authenticated) ====================

    @Test
    fun `POST register-passkey returns 400 when registrationResponse missing`() {
        val email = "regpasskey@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.post("/api/auth/register-passkey") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST register-passkey returns failure when no challenge was issued`() {
        val email = "regpasskeyfail@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.post("/api/auth/register-passkey") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(mapOf("registrationResponse" to "garbage")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Failed to register passkey"))
        }
    }

    // ==================== POST /api/auth/resend-verification ====================

    @Test
    fun `POST resend-verification returns 401 for form request without email`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/resend-verification") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("foo" to "bar").formUrlEncode())
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST resend-verification sends email for unverified user via form email`() {
        val email = "resendform@example.com"
        testApplication {
            setupApp()
            registerUnverifiedUser(email)

            val response = client.post("/api/auth/resend-verification") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to email).formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    @Test
    fun `POST resend-verification fails for already verified user via form email`() {
        val email = "resendformverified@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)

            val response = client.post("/api/auth/resend-verification") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("email" to email).formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `POST resend-verification fails for authenticated already-verified user`() {
        val email = "resendsession@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.post("/api/auth/resend-verification") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `POST resend-verification succeeds for authenticated user flipped back to unverified`() {
        val email = "resendsessionunverified@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")
            // Session cookie only carries userId/email/displayName; flip verification status
            // directly in the DB to exercise the "authenticated but unverified" branch.
            transaction { Users.update({ Users.id eq userId }) { it[Users.isEmailVerified] = false } }

            val response = client.post("/api/auth/resend-verification") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<VerificationResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    // ==================== GET /api/auth/assertion-options ====================

    @Test
    fun `GET assertion-options returns challenge`() {
        testApplication {
            setupApp()
            val response = client.get("/api/auth/assertion-options")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("challenge"))
        }
    }

    // ==================== POST /api/auth/authenticate ====================

    @Test
    fun `POST authenticate returns failure when credential missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/authenticate") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf<Pair<String, String>>().formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("No credential", body.error)
        }
    }

    @Test
    fun `POST authenticate returns failure for invalid credential`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/authenticate") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf("credential" to "not-real-json").formUrlEncode())
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Authentication failed", body.error)
        }
    }

    // ==================== POST /api/auth/logout ====================

    @Test
    fun `POST logout returns success`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/logout")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    // ==================== GET /api/auth/me ====================

    @Test
    fun `GET me returns unauthenticated when no session`() {
        testApplication {
            setupApp()
            val response = client.get("/api/auth/me")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<AuthMeResponse>(response.bodyAsText())
            assertFalse(body.authenticated)
        }
    }

    @Test
    fun `GET me returns authenticated user details for valid session`() {
        val email = "meuser@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email, displayName = "Me User", roles = "ADOPTER,RESCUER")
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            val response = client.get("/api/auth/me") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<AuthMeResponse>(response.bodyAsText())
            assertTrue(body.authenticated)
            assertEquals(email, body.email)
            assertTrue(body.emailVerified)
            assertTrue(body.activeRoles.contains("ADOPTER"))
        }
    }

    @Test
    fun `GET me returns unauthenticated when session user was deleted`() {
        val email = "medeleted@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            val cookie = loginAndGetCookie(email, "SecurePass123!")

            transaction {
                EmailVerificationAttempts.deleteWhere { EmailVerificationAttempts.userId eq userId }
                EmailVerificationTokens.deleteWhere { EmailVerificationTokens.userId eq userId }
                UserPasswords.deleteWhere { UserPasswords.userId eq userId }
                UserActiveRoles.deleteWhere { UserActiveRoles.userId eq userId }
                Users.deleteWhere { Users.id eq userId }
            }

            val response = client.get("/api/auth/me") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<AuthMeResponse>(response.bodyAsText())
            assertFalse(body.authenticated)
        }
    }

    // ==================== POST /api/auth/request-magic-link ====================

    @Test
    fun `POST request-magic-link returns 400 for invalid body`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/request-magic-link") {
                contentType(ContentType.Application.Json)
                setBody("""{"notEncryptedData":"x"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST request-magic-link returns 400 when decryption fails`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/request-magic-link") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest("not-encrypted-data")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST request-magic-link returns 400 for decrypted invalid email format`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/request-magic-link") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("not-an-email"))))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST request-magic-link returns success for unknown email to avoid enumeration`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/request-magic-link") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("unknown@example.com"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    @Test
    fun `POST request-magic-link returns failure for unverified user`() {
        val email = "magicunverified@example.com"
        testApplication {
            setupApp()
            registerUnverifiedUser(email)

            val response = client.post("/api/auth/request-magic-link") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue(email))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals(email, body.email)
        }
    }

    // ==================== GET /api/auth/magic-link-login ====================

    @Test
    fun `GET magic-link-login redirects with invalid_token when token missing`() {
        testApplication {
            setupApp()
            val response = client.config { followRedirects = false }.get("/api/auth/magic-link-login")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/login?error=invalid_token", response.headers[HttpHeaders.Location])
        }
    }

    @Test
    fun `GET magic-link-login redirects with invalid_or_expired for unknown token`() {
        testApplication {
            setupApp()
            val response = client.config { followRedirects = false }.get("/api/auth/magic-link-login?token=nonexistent")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/login?error=invalid_or_expired", response.headers[HttpHeaders.Location])
        }
    }

    @Test
    fun `GET magic-link-login redirects with not_verified when token valid but email unverified and not expired`() {
        val email = "magiclogin-notverified@example.com"
        testApplication {
            setupApp()
            val userId = registerUnverifiedUser(email)
            val token = insertMagicLinkToken(userId)

            val response = client.config { followRedirects = false }.get("/api/auth/magic-link-login?token=$token")
            assertEquals(HttpStatusCode.Found, response.status)
            val location = response.headers[HttpHeaders.Location] ?: ""
            assertTrue(location.startsWith("/login?error=not_verified"))
            assertFalse(location.contains("resent=true"))
        }
    }

    @Test
    fun `GET magic-link-login redirects with not_verified and resent when verification token missing`() {
        val email = "magiclogin-resent@example.com"
        testApplication {
            setupApp()
            val userId = registerUnverifiedUser(email)
            transaction { EmailVerificationTokens.deleteWhere { EmailVerificationTokens.userId eq userId } }
            val token = insertMagicLinkToken(userId)

            val response = client.config { followRedirects = false }.get("/api/auth/magic-link-login?token=$token")
            assertEquals(HttpStatusCode.Found, response.status)
            val location = response.headers[HttpHeaders.Location] ?: ""
            assertTrue(location.contains("resent=true"))
        }
    }

    @Test
    fun `GET magic-link-login redirects with banned for banned verified user`() {
        val email = "magiclogin-banned@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            transaction { Users.update({ Users.id eq userId }) { it[Users.isBanned] = true; it[Users.banReason] = "test ban" } }
            val token = insertMagicLinkToken(userId)

            val response = client.config { followRedirects = false }.get("/api/auth/magic-link-login?token=$token")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/login?error=banned", response.headers[HttpHeaders.Location])
        }
    }

    @Test
    fun `GET magic-link-login succeeds and redirects to profile for verified non-banned user`() {
        val email = "magiclogin-success@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            val token = insertMagicLinkToken(userId)

            val response = client.config { followRedirects = false }.get("/api/auth/magic-link-login?token=$token")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/profile", response.headers[HttpHeaders.Location])
            assertNotNull(response.headers[HttpHeaders.SetCookie])
        }
    }

    private fun insertMagicLinkToken(userId: Int, expiresInMs: Long = 5 * 60 * 1000L): String {
        val token = "magic-token-$userId-${clock.now().toEpochMilliseconds()}"
        transaction {
            MagicLinkTokens.insert {
                it[MagicLinkTokens.userId] = userId
                it[MagicLinkTokens.token] = token
                it[MagicLinkTokens.expiresAt] = clock.now().toEpochMilliseconds() + expiresInMs
                it[MagicLinkTokens.createdAt] = clock.now().toEpochMilliseconds()
                it[MagicLinkTokens.usedAt] = null
            }
        }
        return token
    }

    // ==================== POST /api/auth/login-with-password ====================

    @Test
    fun `POST login-with-password returns 400 for invalid body`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody("""{"foo":"bar"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST login-with-password returns invalid credentials for unknown user`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest("unknown@example.com", encryptValue("whatever"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Invalid credentials", body.error)
        }
    }

    @Test
    fun `POST login-with-password returns invalid credentials for wrong password`() {
        val email = "wrongpass@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)

            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue("WrongPass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Invalid credentials", body.error)
        }
    }

    @Test
    fun `POST login-with-password returns not-verified message when token still valid`() {
        val email = "loginnotverified@example.com"
        testApplication {
            setupApp()
            registerUnverifiedUser(email)

            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue("SecurePass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Please verify your email before logging in", body.error)
        }
    }

    @Test
    fun `POST login-with-password resends verification when token missing or expired`() {
        val email = "loginresend@example.com"
        testApplication {
            setupApp()
            val userId = registerUnverifiedUser(email)
            transaction { EmailVerificationTokens.deleteWhere { EmailVerificationTokens.userId eq userId } }

            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue("SecurePass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertEquals("Verification email was expired. A new verification email has been sent.", body.error)
        }
    }

    @Test
    fun `POST login-with-password reports rate limit when daily verification emails exhausted`() {
        val email = "loginratelimited@example.com"
        testApplication {
            setupApp()
            val userId = registerUnverifiedUser(email)
            transaction {
                EmailVerificationTokens.deleteWhere { EmailVerificationTokens.userId eq userId }
                repeat(3) {
                    EmailVerificationAttempts.insert {
                        it[EmailVerificationAttempts.userId] = userId
                        it[EmailVerificationAttempts.createdAt] = clock.now().toEpochMilliseconds()
                    }
                }
            }

            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue("SecurePass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertTrue(body.error!!.contains("daily limit"))
        }
    }

    @Test
    fun `POST login-with-password returns banned message for banned verified user`() {
        val email = "loginbanned@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            transaction { Users.update({ Users.id eq userId }) { it[Users.isBanned] = true; it[Users.banReason] = "violated rules" } }

            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue("SecurePass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertTrue(body.error!!.contains("violated rules"))
        }
    }

    @Test
    fun `POST login-with-password succeeds for verified non-banned user and sets session`() {
        val email = "loginsuccess@example.com"
        testApplication {
            setupApp()
            registerVerifiedUser(email)

            val response = client.post("/api/auth/login-with-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(PasswordLoginRequest(email, encryptValue("SecurePass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
            assertNotNull(response.headers[HttpHeaders.SetCookie])
        }
    }

    // ==================== POST /api/auth/forgot-password ====================

    @Test
    fun `POST forgot-password returns 400 for invalid body`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody("""{"foo":"bar"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST forgot-password returns 400 when decryption fails`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest("not-encrypted")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST forgot-password succeeds for unknown email`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("unknownforgot@example.com"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    @Test
    fun `POST forgot-password returns failure when daily reset limit reached`() {
        val email = "forgotratelimited@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            transaction {
                repeat(3) { i ->
                    PasswordResetTokens.insert {
                        it[PasswordResetTokens.userId] = userId
                        it[PasswordResetTokens.token] = "preexisting-token-$i"
                        it[PasswordResetTokens.expiresAt] = clock.now().toEpochMilliseconds() + 900000
                        it[PasswordResetTokens.createdAt] = clock.now().toEpochMilliseconds()
                    }
                }
            }

            val response = client.post("/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue(email))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
            assertTrue(body.error!!.contains("Maximum password reset"))
        }
    }

    // ==================== POST /api/auth/reset-password ====================

    @Test
    fun `POST reset-password returns 400 when token missing`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("NewPass123!"))))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST reset-password returns 400 for invalid body`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/reset-password?token=sometoken") {
                contentType(ContentType.Application.Json)
                setBody("""{"foo":"bar"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST reset-password returns failure for invalid token`() {
        testApplication {
            setupApp()
            val response = client.post("/api/auth/reset-password?token=invalid-token") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("NewPass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `POST reset-password returns failure when new password is weak`() {
        val email = "resetweak@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            val token = insertPasswordResetToken(userId)

            val response = client.post("/api/auth/reset-password?token=$token") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("weak"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessWithErrorResponse>(response.bodyAsText())
            assertFalse(body.success)
        }
    }

    @Test
    fun `POST reset-password succeeds for valid token and strong password`() {
        val email = "resetsuccess@example.com"
        testApplication {
            setupApp()
            val userId = registerVerifiedUser(email)
            val token = insertPasswordResetToken(userId)

            val response = client.post("/api/auth/reset-password?token=$token") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(EncryptedLoginRequest(encryptValue("NewSecurePass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    private fun insertPasswordResetToken(userId: Int): String {
        val token = "reset-token-$userId-${clock.now().toEpochMilliseconds()}"
        transaction {
            PasswordResetTokens.insert {
                it[PasswordResetTokens.userId] = userId
                it[PasswordResetTokens.token] = token
                it[PasswordResetTokens.expiresAt] = clock.now().toEpochMilliseconds() + 900000
                it[PasswordResetTokens.createdAt] = clock.now().toEpochMilliseconds()
            }
        }
        return token
    }

    // ==================== GET /api/auth/encryption-key ====================

    @Test
    fun `GET encryption-key returns public key`() {
        testApplication {
            setupApp()
            val response = client.get("/api/auth/encryption-key")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("publicKey"))
        }
    }
}
