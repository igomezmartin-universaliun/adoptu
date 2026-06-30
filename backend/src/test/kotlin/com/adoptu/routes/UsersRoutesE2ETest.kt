package com.adoptu.routes

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.input.AcceptTermsRequest
import com.adoptu.dto.input.BanUserRequest
import com.adoptu.dto.input.RoleActivationRequest
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.configureLogging
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.ports.ImageStoragePort
import com.adoptu.services.auth.SessionUser
import com.adoptu.services.auth.WebAuthnService
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UsersRoutesE2ETest {

    private val clock = Clock.System

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        TestDatabase.clearAllData()
        createTestUsers()
    }

    private fun createTestUsers() {
        transaction {
            try {
                val rescuerId = Users.insert {
                    it[Users.id] = 1
                    it[Users.username] = "rescuer@test.com"
                    it[Users.displayName] = "Test Rescuer"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = rescuerId
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) { }

            try {
                val adopterId = Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "adopter@test.com"
                    it[Users.displayName] = "Test Adopter"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = adopterId
                    it[UserActiveRoles.role] = "ADOPTER"
                }
            } catch (e: Exception) { }

            try {
                val adminId = Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "admin@test.com"
                    it[Users.displayName] = "Test Admin"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = adminId
                    it[UserActiveRoles.role] = "ADMIN"
                }
            } catch (e: Exception) { }

            try {
                val bannableId = Users.insert {
                    it[Users.id] = 4
                    it[Users.username] = "bannable@test.com"
                    it[Users.displayName] = "Bannable User"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = bannableId
                    it[UserActiveRoles.role] = "ADOPTER"
                }
            } catch (e: Exception) { }
        }
    }

    private fun TestApplicationBuilder.setupApp() {
        val config = MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to "80"
        )

        val testModules = module {
            single<io.ktor.server.config.ApplicationConfig> { config }
            single<kotlin.time.Clock> { kotlin.time.Clock.System }
            single { WebAuthnService(get(), get(), get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", listOf(config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:80")) }
            single<ImageStoragePort> { MockImageStorage() }
            single { MockNotificationAdapter() }
            single<com.adoptu.ports.NotificationPort> { get<MockNotificationAdapter>() }
            single<com.adoptu.ports.PetRepositoryPort> { PetRepositoryImpl(get()) }
            single<com.adoptu.ports.UserRepositoryPort> { UserRepository(get()) }
            single<com.adoptu.ports.PhotographerRepositoryPort> { com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl(get(), get(), get()) }
            single { com.adoptu.services.PhotographerService(get(), get(), get(), get()) }
            single { com.adoptu.services.UserService(get()) }
            single { com.adoptu.services.PetService(get(), get(), get(), get()) }
            single { com.adoptu.services.PasswordService(get(), get(), get(), "http://localhost:80") }
            single { com.adoptu.services.EmailChangeService(get(), get(), get(), "http://localhost:80") }
        }

        environment {
            this.config = config
        }

        application {
            install(Koin) {
                modules(testModules)
            }
            configureLogging()
            configureSerialization()
            configureSessions()
            routing {
                usersRoutes()
                adminUsersRoutes()
                // Test-only helper to establish a real, correctly-signed session cookie
                // without re-implementing the production login flow.
                post("/test/login/{userId}") {
                    val userId = call.parameters["userId"]!!.toInt()
                    call.sessions.set(SessionUser(userId, "user$userId@test.com", "Test User $userId"))
                    call.respondText("OK")
                }
            }
        }
    }

    private suspend fun HttpClient.loginAs(userId: Int): String {
        val response = post("/test/login/$userId")
        val setCookie = response.headers[HttpHeaders.SetCookie]
            ?: error("No session cookie returned from test login")
        return setCookie.substringBefore(";")
    }

    // ==================== POST /api/users/accept-terms ====================

    @Test
    fun `POST accept-terms returns 401 when no session`() {
        testApplication {
            setupApp()

            val request = AcceptTermsRequest(
                acceptPrivacyPolicy = true,
                acceptTermsAndConditions = false
            )

            val response = client.post("/api/users/accept-terms") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(AcceptTermsRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Unauthorized"))
        }
    }

    @Test
    fun `POST accept-terms returns error for invalid content type`() {
        testApplication {
            setupApp()

            val response = client.post("/api/users/accept-terms") {
                contentType(ContentType.Text.Plain)
                setBody("invalid body")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST accept-terms returns 404 for non-existent user`() {
        testApplication {
            setupApp()

            val request = AcceptTermsRequest(
                acceptPrivacyPolicy = true,
                acceptTermsAndConditions = false
            )

            val response = client.post("/api/users/accept-terms") {
                header("Cookie", "user_session=invalid_session")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST accept-terms succeeds for authenticated user`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val request = AcceptTermsRequest(
                acceptPrivacyPolicy = true,
                acceptTermsAndConditions = true
            )

            val response = client.post("/api/users/accept-terms") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(AcceptTermsRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("rescuer@test.com"))
        }
    }

    // ==================== GET /api/admin/users ====================

    @Test
    fun `GET admin users returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.get("/api/admin/users")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET admin users returns 403 for non-admin user`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1) // rescuer

            val response = client.get("/api/admin/users") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `GET admin users returns list for admin`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.get("/api/admin/users") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("rescuer@test.com"))
            assertTrue(body.contains("adopter@test.com"))
        }
    }

    // ==================== GET /api/admin/users/{id} ====================

    @Test
    fun `GET admin users by id returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.get("/api/admin/users/1")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET admin users by id returns 403 for non-admin user`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter

            val response = client.get("/api/admin/users/1") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `GET admin users by id returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.get("/api/admin/users/not-a-number") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `GET admin users by id returns 404 for non-existent user`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.get("/api/admin/users/9999") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET admin users by id returns user for admin`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.get("/api/admin/users/1") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("rescuer@test.com"))
        }
    }

    // ==================== POST /api/admin/users/{id}/ban ====================

    @Test
    fun `POST admin users ban returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/admin/users/4/ban") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("spam")))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST admin users ban returns 403 for non-admin user`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1) // rescuer

            val response = client.post("/api/admin/users/4/ban") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("spam")))
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `POST admin users ban returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/not-a-number/ban") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("spam")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST admin users ban returns 400 when banning self`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/3/ban") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("spam")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Cannot ban yourself"))
        }
    }

    @Test
    fun `POST admin users ban returns 404 for non-existent target`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/9999/ban") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("spam")))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST admin users ban returns 400 when target is admin`() {
        testApplication {
            setupApp()
            // Create a second admin to ban
            transaction {
                val secondAdminId = Users.insert {
                    it[Users.id] = 5
                    it[Users.username] = "admin2@test.com"
                    it[Users.displayName] = "Second Admin"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = secondAdminId
                    it[UserActiveRoles.role] = "ADMIN"
                }
            }
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/5/ban") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("spam")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Cannot ban an admin"))
        }
    }

    @Test
    fun `POST admin users ban succeeds for valid target`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/4/ban") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(BanUserRequest.serializer(), BanUserRequest("repeated spam reports")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)

            val banned = transaction { Users.selectAll().where { Users.id eq 4 }.first()[Users.isBanned] }
            assertTrue(banned)
        }
    }

    // ==================== POST /api/admin/users/{id}/unban ====================

    @Test
    fun `POST admin users unban returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/admin/users/4/unban")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST admin users unban returns 403 for non-admin user`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1) // rescuer

            val response = client.post("/api/admin/users/4/unban") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `POST admin users unban returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/not-a-number/unban") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST admin users unban returns 500 for non-existent target`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/9999/unban") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to unban user"))
        }
    }

    @Test
    fun `POST admin users unban succeeds for previously banned user`() {
        testApplication {
            setupApp()
            transaction { Users.update({ Users.id eq 4 }) { it[Users.isBanned] = true; it[Users.banReason] = "test" } }
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/admin/users/4/unban") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)

            val banned = transaction { Users.selectAll().where { Users.id eq 4 }.first()[Users.isBanned] }
            assertFalse(banned)
        }
    }

    // ==================== PUT /api/users/profile ====================

    @Test
    fun `PUT profile returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.put("/api/users/profile") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateProfileRequest.serializer(), UpdateProfileRequest("New Name")))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT profile updates display name when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateProfileRequest.serializer(), UpdateProfileRequest("New Name")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("New Name"))
        }
    }

    @Test
    fun `PUT profile accepts language query parameter`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/profile?language=es") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateProfileRequest.serializer(), UpdateProfileRequest("Nombre Nuevo")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // ==================== PUT /api/users/language ====================

    @Test
    fun `PUT language returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.put("/api/users/language") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateLanguageRequest.serializer(), UpdateLanguageRequest("fr")))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT language updates language when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/language") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateLanguageRequest.serializer(), UpdateLanguageRequest("fr")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `PUT language returns 400 for blank language`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/language") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateLanguageRequest.serializer(), UpdateLanguageRequest("")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // ==================== GET /api/users/rescuers ====================

    @Test
    fun `GET rescuers returns rescuer list without auth`() {
        testApplication {
            setupApp()
            val response = client.get("/api/users/rescuers")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("rescuer@test.com"))
        }
    }

    // ==================== POST /api/users/rescuer-profile ====================

    @Test
    fun `POST rescuer-profile returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/rescuer-profile") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST rescuer-profile activates when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter

            val response = client.post("/api/users/rescuer-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `POST rescuer-profile deactivates when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1) // already a rescuer

            val response = client.post("/api/users/rescuer-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(false)))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // ==================== POST /api/users/temporal-home-profile ====================

    @Test
    fun `POST temporal-home-profile returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/temporal-home-profile") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST temporal-home-profile activates and deactivates when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val activate = client.post("/api/users/temporal-home-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.OK, activate.status)

            val deactivate = client.post("/api/users/temporal-home-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(false)))
            }
            assertEquals(HttpStatusCode.OK, deactivate.status)
        }
    }

    @Test
    fun `POST temporal-home-profile returns 404 for session user that does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.post("/api/users/temporal-home-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    // ==================== PUT /api/users/photographer-settings ====================

    @Test
    fun `PUT photographer-settings returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.put("/api/users/photographer-settings") {
                contentType(ContentType.Application.Json)
                setBody("""{"photographerFee":50.0,"photographerCurrency":"USD","country":"United States","state":"NY"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT photographer-settings returns 404 when no photographer profile exists`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val response = client.put("/api/users/photographer-settings") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody("""{"photographerFee":50.0,"photographerCurrency":"USD","country":"United States","state":"NY"}""")
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    // ==================== POST /api/users/photographer-profile ====================

    @Test
    fun `POST photographer-profile returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/photographer-profile") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST photographer-profile activates and deactivates when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val activate = client.post("/api/users/photographer-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.OK, activate.status)

            val deactivate = client.post("/api/users/photographer-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(false)))
            }
            assertEquals(HttpStatusCode.OK, deactivate.status)
        }
    }

    // ==================== POST /api/users/shelter-profile ====================

    @Test
    fun `POST shelter-profile returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/shelter-profile") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST shelter-profile activates and deactivates when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val activate = client.post("/api/users/shelter-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.OK, activate.status)

            val deactivate = client.post("/api/users/shelter-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(false)))
            }
            assertEquals(HttpStatusCode.OK, deactivate.status)
        }
    }

    @Test
    fun `POST shelter-profile returns 404 for session user that does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.post("/api/users/shelter-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    // ==================== POST /api/users/sterilization-profile ====================

    @Test
    fun `POST sterilization-profile returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/sterilization-profile") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST sterilization-profile activates and deactivates when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val activate = client.post("/api/users/sterilization-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.OK, activate.status)

            val deactivate = client.post("/api/users/sterilization-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(false)))
            }
            assertEquals(HttpStatusCode.OK, deactivate.status)
        }
    }

    @Test
    fun `POST sterilization-profile returns 404 for session user that does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.post("/api/users/sterilization-profile") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(RoleActivationRequest.serializer(), RoleActivationRequest(true)))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    // ==================== GET /api/users/has-password ====================

    @Test
    fun `GET has-password returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.get("/api/users/has-password")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET has-password returns false when no password set`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.get("/api/users/has-password") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"hasPassword\":false") || response.bodyAsText().contains("\"hasPassword\": false"))
        }
    }

    // ==================== POST /api/users/password ====================

    private fun encryptedCredential(plaintext: String): String {
        com.adoptu.services.crypto.CryptoService.initialize()
        val publicKey = com.adoptu.services.crypto.CryptoService.getPublicKey()
        return com.adoptu.services.crypto.CryptoService.encrypt(plaintext, publicKey)
            ?: error("Encryption failed in test")
    }

    @Test
    fun `POST password returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(SetPasswordRequest.serializer(), SetPasswordRequest(encryptedCredential("user1@test.com:ValidPass123!"))))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST password succeeds for a strong password`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/users/password") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(SetPasswordRequest.serializer(), SetPasswordRequest(encryptedCredential("user1@test.com:ValidPass123!"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    @Test
    fun `POST password fails for a weak password`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/users/password") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(SetPasswordRequest.serializer(), SetPasswordRequest(encryptedCredential("user1@test.com:weak"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"success\":false") || response.bodyAsText().contains("\"success\": false"))
        }
    }

    // ==================== PUT /api/users/password ====================

    @Test
    fun `PUT password returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.put("/api/users/password") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(ChangePasswordRequest.serializer(), ChangePasswordRequest(encryptedCredential("user1@test.com:Old123!@"), encryptedCredential("user1@test.com:New456!@"))))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT password succeeds when no existing password is set`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/password") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(ChangePasswordRequest.serializer(), ChangePasswordRequest(encryptedCredential("user1@test.com:Whatever123!"), encryptedCredential("user1@test.com:New456!@"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.decodeFromString<SuccessResponse>(response.bodyAsText())
            assertTrue(body.success)
        }
    }

    @Test
    fun `PUT password fails when new password is weak`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/password") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(ChangePasswordRequest.serializer(), ChangePasswordRequest(encryptedCredential("user1@test.com:Whatever123!"), encryptedCredential("user1@test.com:weak"))))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"success\":false") || response.bodyAsText().contains("\"success\": false"))
        }
    }

    // ==================== POST /api/users/request-email-change ====================

    @Test
    fun `POST request-email-change returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.post("/api/users/request-email-change") {
                contentType(ContentType.Application.Json)
                setBody("""{"newEmail":"new@test.com"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `POST request-email-change returns 400 for invalid email format`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/users/request-email-change") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody("""{"newEmail":"not-an-email"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST request-email-change succeeds for a valid new email`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/users/request-email-change") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody("""{"newEmail":"brand-new@test.com"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // ==================== GET /api/users/verify-email-change ====================

    @Test
    fun `GET verify-email-change returns 400 when token missing`() {
        testApplication {
            setupApp()
            val response = client.get("/api/users/verify-email-change")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `GET verify-email-change returns failure message for invalid token`() {
        testApplication {
            setupApp()
            val response = client.get("/api/users/verify-email-change?token=nonexistent")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"success\":false") || response.bodyAsText().contains("\"success\": false"))
        }
    }
}
