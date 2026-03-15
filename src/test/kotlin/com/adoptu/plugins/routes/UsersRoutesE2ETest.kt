package com.adoptu.plugins.routes

import com.adoptu.di.appModule
import com.adoptu.dto.AcceptTermsRequest
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

class UsersRoutesE2ETest {

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
                usersRoutes()
            }
        }
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
}
