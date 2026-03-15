package com.adoptu.plugins.routes

import com.adoptu.di.appModule
import com.adoptu.mocks.MockEmailService
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.TestDatabase
import com.adoptu.models.Users
import com.adoptu.plugins.configureRouting
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
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

class AuthRoutesE2ETest {

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
                    it[Users.username] = "rescuer"
                    it[Users.displayName] = "Test Rescuer"
                    it[Users.email] = "rescuer@test.com"
                    it[Users.role] = "RESCUER"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
            } catch (e: Exception) { }

            try {
                Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "adopter"
                    it[Users.displayName] = "Test Adopter"
                    it[Users.email] = "adopter@test.com"
                    it[Users.role] = "ADOPTER"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
            } catch (e: Exception) { }

            try {
                Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "admin"
                    it[Users.displayName] = "Test Admin"
                    it[Users.email] = "admin@test.com"
                    it[Users.role] = "ADMIN"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
            } catch (e: Exception) { }
        }
    }

    private fun TestApplicationBuilder.setupApp() {
        val config = MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to "8080"
        )

        val testModules = module {
            single<io.ktor.server.config.ApplicationConfig> { config }
            single { com.adoptu.auth.WebAuthnService }
            single { MockImageStorage() }
            single { MockEmailService() }
            single<com.adoptu.domains.image.ImageStoragePort> { get<MockImageStorage>() }
            single { com.adoptu.services.EmailService(get()) }
            single { com.adoptu.repositories.PetRepository }
            single { com.adoptu.services.UserService }
            single { com.adoptu.services.PetService(get(), get(), get()) }
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
                authRoutes()
            }
        }
    }

    // ==================== POST /api/auth/registration-options ====================

    @Test
    fun `POST registration-options returns options with username and displayName`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/registration-options") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("username=newuser&displayName=New+User")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("challenge"))
            assertTrue(body.contains("newuser"))
        }
    }

    // ==================== GET /api/auth/assertion-options ====================

    @Test
    fun `GET assertion-options returns options`() {
        testApplication {
            setupApp()

            val response = client.get("/api/auth/assertion-options")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("challenge"))
        }
    }

    // ==================== POST /api/auth/logout ====================

    @Test
    fun `POST logout returns success`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/logout")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("success"))
        }
    }

    // ==================== GET /api/auth/me ====================

    @Test
    fun `GET me returns unauthenticated when no session`() {
        testApplication {
            setupApp()

            val response = client.get("/api/auth/me")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("authenticated"))
            assertTrue(body.contains("false"))
        }
    }

    // ==================== POST /api/auth/register ====================

    // Note: WebAuthn registration requires browser interaction, so we only test basic endpoints

    // ==================== POST /api/auth/authenticate ====================

    @Test
    fun `POST authenticate returns error when body blank`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/authenticate") {
                contentType(ContentType.Text.Plain)
                setBody("")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("No credential"))
        }
    }

    @Test
    fun `POST authenticate returns error when authentication fails`() {
        testApplication {
            setupApp()

            val response = client.post("/api/auth/authenticate") {
                contentType(ContentType.Text.Plain)
                setBody("invalid_credential")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Authentication failed") || body.contains("success\":false"))
        }
    }
}
