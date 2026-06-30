package com.adoptu.routes

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.UserShelters
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.UserShelterRepository
import com.adoptu.dto.input.CreateUserShelterRequest
import com.adoptu.dto.input.UpdateUserShelterRequest
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.ports.UserShelterRepositoryPort
import com.adoptu.services.UserShelterService
import com.adoptu.services.auth.SessionUser
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
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UserShelterRoutesE2ETest {

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
                Users.insert {
                    it[Users.id] = 1
                    it[Users.username] = "rescuer@test.com"
                    it[Users.displayName] = "Test Rescuer"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 1
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) { }

            try {
                Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "other@test.com"
                    it[Users.displayName] = "Other Rescuer"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 2
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) { }

            try {
                Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "search@test.com"
                    it[Users.displayName] = "Search Owner"
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 3
                    it[UserActiveRoles.role] = "RESCUER"
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
            single<kotlin.time.Clock> { kotlin.time.Clock.System }
            single<UserShelterRepositoryPort> { UserShelterRepository(get()) }
            single { UserShelterService(get()) }
        }

        environment {
            this.config = config
        }

        application {
            install(Koin) {
                modules(testModules)
            }
            configureSerialization()
            configureSessions()
            routing {
                userShelterRoutes()
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

    private fun createShelterInDb(userId: Int, country: String = "United States", state: String? = "CA", city: String = "LA") {
        transaction {
            val now = clock.now().toEpochMilliseconds()
            UserShelters.insert {
                it[UserShelters.userId] = userId
                it[UserShelters.name] = "Shelter $userId"
                it[UserShelters.country] = com.adoptu.common.Country.fromDisplayName(country)!!
                it[UserShelters.state] = state
                it[UserShelters.city] = city
                it[UserShelters.address] = "123 Main St"
                it[UserShelters.currency] = "USD"
                it[UserShelters.createdAt] = now
                it[UserShelters.updatedAt] = now
            }
        }
    }

    // ==================== POST /api/users/shelter ====================

    @Test
    fun `POST users shelter returns 401 when no session`() {
        testApplication {
            setupApp()

            val request = CreateUserShelterRequest(
                name = "My Shelter",
                country = "United States",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/users/shelter") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateUserShelterRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(response.bodyAsText().contains("Unauthorized"))
        }
    }

    @Test
    fun `POST users shelter creates shelter when authenticated`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val request = CreateUserShelterRequest(
                name = "My Shelter",
                country = "United States",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateUserShelterRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("My Shelter"))
            assertTrue(body.contains("\"userId\": 1"))
        }
    }

    @Test
    fun `POST users shelter twice updates existing shelter instead of failing`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val firstRequest = CreateUserShelterRequest(
                name = "First Name",
                country = "United States",
                city = "LA",
                address = "123 Main St"
            )
            client.post("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateUserShelterRequest.serializer(), firstRequest))
            }

            val secondRequest = CreateUserShelterRequest(
                name = "Second Name",
                country = "United States",
                city = "LA",
                address = "456 Other St"
            )
            val response = client.post("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateUserShelterRequest.serializer(), secondRequest))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Second Name"))
        }
    }

    @Test
    fun `POST users shelter returns 400 for blank name`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val request = CreateUserShelterRequest(
                name = "",
                country = "United States",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateUserShelterRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Name is required"))
        }
    }

    // ==================== GET /api/users/shelter ====================

    @Test
    fun `GET users shelter returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.get("/api/users/shelter")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET users shelter returns 404 when not found`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.get("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("Shelter profile not found"))
        }
    }

    @Test
    fun `GET users shelter returns shelter when it exists`() {
        createShelterInDb(1)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.get("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Shelter 1"))
        }
    }

    // ==================== PUT /api/users/shelter ====================

    @Test
    fun `PUT users shelter returns 401 when no session`() {
        testApplication {
            setupApp()

            val response = client.put("/api/users/shelter") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateUserShelterRequest.serializer(), UpdateUserShelterRequest(name = "New")))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT users shelter returns 404 when shelter does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateUserShelterRequest.serializer(), UpdateUserShelterRequest(name = "New")))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("Not found"))
        }
    }

    @Test
    fun `PUT users shelter updates shelter when it exists`() {
        createShelterInDb(1)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateUserShelterRequest.serializer(), UpdateUserShelterRequest(name = "Updated Shelter")))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Updated Shelter"))
        }
    }

    // ==================== DELETE /api/users/shelter ====================

    @Test
    fun `DELETE users shelter returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.delete("/api/users/shelter")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `DELETE users shelter returns 404 when shelter does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE users shelter deletes shelter when it exists`() {
        createShelterInDb(1)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val followUp = client.get("/api/users/shelter") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, followUp.status)
        }
    }

    // ==================== GET /api/user-shelters ====================

    @Test
    fun `GET user-shelters returns 400 when country is missing`() {
        testApplication {
            setupApp()
            val response = client.get("/api/user-shelters")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Country is required"))
        }
    }

    @Test
    fun `GET user-shelters returns 400 when country is blank`() {
        testApplication {
            setupApp()
            val response = client.get("/api/user-shelters?country=")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `GET user-shelters returns matching shelters`() {
        createShelterInDb(3, country = "United States", state = "CA", city = "LA")

        testApplication {
            setupApp()
            val response = client.get("/api/user-shelters?country=United%20States&state=CA&city=LA")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Shelter 3"))
        }
    }

    @Test
    fun `GET user-shelters returns empty list for non-matching country`() {
        createShelterInDb(3, country = "United States")

        testApplication {
            setupApp()
            val response = client.get("/api/user-shelters?country=Canada")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }
}
