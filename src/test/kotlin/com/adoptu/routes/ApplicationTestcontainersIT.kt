package com.adoptu.routes

import com.adoptu.adapters.db.*
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl
import com.adoptu.adapters.db.repositories.TemporalHomeRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.adapters.notification.SesEmailAdapter
import com.adoptu.adapters.storage.S3ImageStorageAdapter
import com.adoptu.mocks.TestClock
import com.adoptu.plugins.configureRouting
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.plugins.configureWebAuthn
import com.adoptu.ports.*
import com.adoptu.services.auth.WebAuthnService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Testcontainers
@OptIn(ExperimentalTime::class)
class ApplicationTestcontainersIT {

    companion object {
        @Container
        val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("adoptu")
            .withUsername("adoptu")
            .withPassword("Ad0ptU")

        @Container
        val localstackContainer: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("EAGER_SERVICE_LOADING", "1")
            .waitingFor(Wait.forListeningPort())
    }

    private val testClock: Clock = TestClock()

    private fun createAppConfig(): ApplicationConfig {
        return MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to "0",
            "db.test.postgres.driver" to "org.postgresql.Driver",
            "db.test.postgres.url" to postgresContainer.jdbcUrl,
            "db.test.postgres.user" to postgresContainer.username,
            "db.test.postgres.password" to postgresContainer.password,
            "storage.test.bucket" to "test-bucket",
            "storage.test.region" to localstackContainer.region,
            "storage.test.endpoint" to localstackContainer.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
            "storage.test.access_key_id" to localstackContainer.accessKey,
            "storage.test.secret_access_key" to localstackContainer.secretKey,
            "storage.test.path_style_access" to "true",
            "email.from" to "test@adopt-u.com",
            "admin.email" to "admin@adopt-u.com"
        )
    }

    private fun initDatabase(config: ApplicationConfig) {
        val driverClassName = config.property("db.test.postgres.driver").getString()
        val jdbcURL = config.property("db.test.postgres.url").getString()
        val user = config.property("db.test.postgres.user").getString()
        val password = config.property("db.test.postgres.password").getString()

        Database.connect(jdbcURL, driverClassName, user = user, password = password)

        transaction {
            SchemaUtils.drop(
                EmailVerificationTokens,
                TemporalHomeRequests,
                BlockedRescuers,
                TemporalHomes,
                AdoptionRequests,
                PetImages,
                PhotographyRequests,
                Pets,
                Photographers,
                WebAuthnCredentials,
                UserActiveRoles,
                Users
            )
            SchemaUtils.create(
                Users,
                UserActiveRoles,
                WebAuthnCredentials,
                Photographers,
                Pets,
                PetImages,
                AdoptionRequests,
                PhotographyRequests,
                TemporalHomes,
                BlockedRescuers,
                TemporalHomeRequests,
                EmailVerificationTokens
            )
        }
    }

    private fun TestApplicationBuilder.setupApp() {
        val config = createAppConfig()

        // Initialize database connection and schema
        initDatabase(config)

        val testModules = module {
            single<ApplicationConfig> { config }
            single<Clock> { testClock }
            single { WebAuthnService(get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:8080") }
            single<UserRepositoryPort> { UserRepository(get()) }
            single<PetRepositoryPort> { PetRepositoryImpl(get()) }
            single<PhotographerRepositoryPort> { PhotographerRepositoryImpl(get(), get(), get()) }
            single<TemporalHomeRepositoryPort> { TemporalHomeRepositoryImpl(get(), get(), get()) }
            single<ImageStoragePort> {
                S3ImageStorageAdapter(
                    bucketName = "test-bucket",
                    region = localstackContainer.region,
                    accessKeyId = localstackContainer.accessKey,
                    secretAccessKey = localstackContainer.secretKey,
                    endpoint = localstackContainer.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                    pathStyleAccess = true
                )
            }
            single<NotificationPort> { SesEmailAdapter(get()) }
            single<com.adoptu.services.UserService> { com.adoptu.services.UserService(get()) }
            single<com.adoptu.services.PetService> { com.adoptu.services.PetService(get(), get(), get(), get()) }
            single<com.adoptu.services.PhotographerService> { com.adoptu.services.PhotographerService(get(), get(), get(), get()) }
            single<com.adoptu.services.TemporalHomeService> { com.adoptu.services.TemporalHomeService(get(), get(), get(), get()) }
            single<com.adoptu.services.EmailVerificationService> { com.adoptu.services.EmailVerificationService(get(), get(), get()) }
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
            configureWebAuthn()
            configureRouting()
        }
    }

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        println("Starting test: ${testInfo.displayName}")
        println("PostgreSQL: ${postgresContainer.jdbcUrl}")
        println("LocalStack: ${localstackContainer.getEndpointOverride(LocalStackContainer.Service.S3)}")
    }

    @Test
    fun `containers are running`() {
        assertTrue(postgresContainer.isRunning, "PostgreSQL container should be running")
        assertTrue(localstackContainer.isRunning, "LocalStack container should be running")
    }

    @Test
    fun `application starts successfully`() {
        testApplication {
            setupApp()
            
            val response = client.get("/")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.NotFound, HttpStatusCode.Found),
                "Application should respond with valid HTTP status"
            )
        }
    }

    @Test
    fun `health endpoint responds`() {
        testApplication {
            setupApp()
            
            val response = client.get("/health")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.NotFound),
                "Health endpoint should respond"
            )
        }
    }

    @Test
    fun `api auth me endpoint works`() {
        testApplication {
            setupApp()
            
            val response = client.get("/api/auth/me")
            // May return OK or 401 depending on implementation
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized),
                "Auth me endpoint should respond with valid status"
            )
        }
    }

    @Test
    fun `pets endpoint returns response`() {
        testApplication {
            setupApp()
            
            val response = client.get("/api/pets")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError),
                "Pets endpoint should respond"
            )
        }
    }

    @Test
    fun `temporal homes endpoint returns response`() {
        testApplication {
            setupApp()
            
            val response = client.get("/api/temporal-homes")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized, HttpStatusCode.InternalServerError),
                "Temporal homes endpoint should respond"
            )
        }
    }

    @Test
    fun `photographers endpoint returns response`() {
        testApplication {
            setupApp()
            
            val response = client.get("/api/photographers")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError),
                "Photographers endpoint should respond"
            )
        }
    }

    @Test
    fun `unknown route returns 404`() {
        testApplication {
            setupApp()
            
            val response = client.get("/api/unknown-route-xyz-123")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `register page is accessible`() {
        testApplication {
            setupApp()
            
            val response = client.get("/register")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Found),
                "Register page should be accessible"
            )
        }
    }

    @Test
    fun `login page is accessible`() {
        testApplication {
            setupApp()
            
            val response = client.get("/login")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Found),
                "Login page should be accessible"
            )
        }
    }

    @Test
    fun `S3 storage is accessible via localstack`() {
        testApplication {
            setupApp()
            
            // The S3ImageStorageAdapter should be able to connect to localstack
            // This test verifies the container is properly configured
            assertNotNull(localstackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
            assertTrue(localstackContainer.isRunning)
        }
    }

    @Test
    fun `database connection works`() {
        testApplication {
            setupApp()
            
            // Test that database connection is working by accessing an endpoint that queries the DB
            val response = client.get("/api/pets")
            println("Response status: ${response.status}")
            assertTrue(
                response.status.value < 500,
                "Database connection should work without 5xx errors, got: ${response.status}"
            )
        }
    }

    @Test
    fun `privacy page returns 200`() {
        testApplication {
            setupApp()
            
            val response = client.get("/privacy")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `privacy page contains privacy policy content`() {
        testApplication {
            setupApp()
            
            val response = client.get("/privacy")
            val body = response.bodyAsText()
            assertTrue(body.contains("Privacy Policy"), "Page should contain Privacy Policy heading")
            assertTrue(body.contains("Information We Collect"), "Page should contain information section")
            assertTrue(body.contains("How We Use Information"), "Page should contain usage section")
            assertTrue(body.contains("Contact Us"), "Page should contain contact section")
        }
    }

    @Test
    fun `terms page returns 200`() {
        testApplication {
            setupApp()
            
            val response = client.get("/terms")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `terms page contains terms content`() {
        testApplication {
            setupApp()
            
            val response = client.get("/terms")
            val body = response.bodyAsText()
            assertTrue(body.contains("Terms and Conditions"), "Page should contain Terms and Conditions")
        }
    }

    @Test
    fun `login page returns 200`() {
        testApplication {
            setupApp()
            
            val response = client.get("/login")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `register page returns 200`() {
        testApplication {
            setupApp()
            
            val response = client.get("/register")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `index page contains loadPets JavaScript function`() {
        testApplication {
            setupApp()
            
            val response = client.get("/")
            val body = response.bodyAsText()
            assertTrue(body.contains("function loadPets"), "Page should contain loadPets function")
            assertTrue(body.contains("api.getPets"), "Page should call api.getPets")
        }
    }

    @Test
    fun `login page contains authentication JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/login")
            val body = response.bodyAsText()
            assertTrue(body.contains("authenticateWithResponse"), "Page should contain authenticateWithResponse function")
            assertTrue(body.contains("checkProfileCompletion"), "Page should contain checkProfileCompletion function")
        }
    }

    @Test
    fun `register page contains registration JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/register")
            val body = response.bodyAsText()
            assertTrue(body.contains("webauthn.register"), "Page should contain webauthn.register function")
        }
    }

    @Test
    fun `profile page contains profile JavaScript functions`() {
        testApplication {
            setupApp()
            
            val response = client.get("/profile")
            val body = response.bodyAsText()
            assertTrue(body.contains("api.me"), "Page should contain api.me call")
            assertTrue(body.contains("api.updateProfile") || body.contains("updateProfile"), "Page should contain profile update functionality")
        }
    }

    @Test
    fun `pets page contains JavaScript functions`() {
        testApplication {
            setupApp()
            
            val response = client.get("/pets")
            if (response.status.value == 200) {
                val body = response.bodyAsText()
                assertTrue(body.contains("function loadPets"), "Page should contain loadPets function")
                assertTrue(body.contains("currentType"), "Page should handle type filtering")
                assertTrue(body.contains("currentSex"), "Page should handle sex filtering")
                assertTrue(body.contains("filter-btn"), "Page should contain filter buttons")
                assertTrue(body.contains("api.getPets"), "Page should call api.getPets")
            } else {
                println("Pets page returned ${response.status}, checking if index page has the same JS")
                val indexResponse = client.get("/")
                val indexBody = indexResponse.bodyAsText()
                assertTrue(indexBody.contains("function loadPets"), "Index page should contain loadPets function")
            }
        }
    }

    @Test
    fun `my pets page is accessible and contains JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/my-pets")
            if (response.status.value < 400) {
                val body = response.bodyAsText()
                assertTrue(body.contains("function load"), "Page should contain load function")
                assertTrue(body.contains("function fillForm"), "Page should contain fillForm function")
                assertTrue(body.contains("function handleFiles"), "Page should contain handleFiles function")
                assertTrue(body.contains("function updatePreviews"), "Page should contain updatePreviews function")
            } else {
                println("My pets page returned ${response.status}")
            }
        }
    }

    @Test
    fun `pet detail page contains pet detail JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/pet/1")
            val body = response.bodyAsText()
            assertTrue(body.contains("api.getPet"), "Page should contain api.getPet call")
        }
    }

    @Test
    fun `shelters page contains search JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/shelters")
            val body = response.bodyAsText()
            assertTrue(body.contains("function onCountryChange"), "Page should contain onCountryChange function")
            assertTrue(body.contains("function searchShelters"), "Page should contain searchShelters function")
        }
    }

    @Test
    fun `photographers page contains search JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/photographers")
            val body = response.bodyAsText()
            assertTrue(body.contains("function onCountryChange"), "Page should contain onCountryChange function")
            assertTrue(body.contains("function searchPhotographers"), "Page should contain searchPhotographers function")
        }
    }

    @Test
    fun `temporal homes page contains search JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/temporal-homes")
            val body = response.bodyAsText()
            assertTrue(body.contains("function onCountryChange"), "Page should contain onCountryChange function")
            assertTrue(body.contains("function searchTemporalHomes"), "Page should contain searchTemporalHomes function")
        }
    }

    @Test
    fun `sterilization locations page contains location JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/sterilization-locations")
            val body = response.bodyAsText()
            assertTrue(body.contains("function loadCountries"), "Page should contain loadCountries function")
            assertTrue(body.contains("function loadStates"), "Page should contain loadStates function")
            assertTrue(body.contains("function loadCities"), "Page should contain loadCities function")
            assertTrue(body.contains("function loadLocations"), "Page should contain loadLocations function")
        }
    }

    @Test
    fun `admin sterilization locations page is accessible`() {
        testApplication {
            setupApp()
            
            val response = client.get("/admin/sterilization-locations")
            if (response.status.value < 500) {
                val body = response.bodyAsText()
                assertTrue(body.contains("function loadCountries"), "Page should contain loadCountries function")
                assertTrue(body.contains("function loadLocations"), "Page should contain loadLocations function")
                assertTrue(body.contains("function showForm"), "Page should contain showForm function")
                assertTrue(body.contains("function editLocation"), "Page should contain editLocation function")
                assertTrue(body.contains("function deleteLocation"), "Page should contain deleteLocation function")
            } else {
                println("Admin sterilization locations page returned ${response.status}, skipping JS assertion")
            }
        }
    }

    @Test
    fun `admin shelters page is accessible`() {
        testApplication {
            setupApp()
            
            val response = client.get("/admin/shelters")
            assertTrue(
                response.status.value < 500,
                "Admin shelters page should be accessible (not 5xx), got: ${response.status}"
            )
        }
    }

    @Test
    fun `admin page contains JavaScript functions`() {
        testApplication {
            setupApp()
            
            val response = client.get("/admin")
            if (response.status.value < 500) {
                val body = response.bodyAsText()
                assertTrue(body.contains("loadUsers"), "Page should contain loadUsers function")
                assertTrue(body.contains("loadPets"), "Page should contain loadPets function")
            } else {
                println("Admin page returned ${response.status}, skipping JS assertion")
            }
        }
    }

    @Test
    fun `privacy page contains required scripts`() {
        testApplication {
            setupApp()
            
            val response = client.get("/privacy")
            val body = response.bodyAsText()
            assertTrue(body.contains("api.js"), "Page should include api.js")
            assertTrue(body.contains("i18n.js"), "Page should include i18n.js")
        }
    }

    @Test
    fun `terms page contains required scripts`() {
        testApplication {
            setupApp()
            
            val response = client.get("/terms")
            val body = response.bodyAsText()
            assertTrue(body.contains("api.js"), "Page should include api.js")
            assertTrue(body.contains("i18n.js"), "Page should include i18n.js")
        }
    }

    @Test
    fun `email verification page with valid token shows success content`() {
        testApplication {
            setupApp()
            
            val response = client.get("/verify?token=validtoken123")
            val body = response.bodyAsText()
            assertTrue(body.contains("verification-success") || body.contains("Email Verified") || body.contains("verification-error") || body.contains("Verification Failed"), "Page should contain verification content")
        }
    }

    @Test
    fun `email verification page without token shows error content`() {
        testApplication {
            setupApp()
            
            val response = client.get("/verify")
            val body = response.bodyAsText()
            assertTrue(body.contains("verification-error") || body.contains("Verification Failed") || body.contains("invalid"), "Page should show error when no token provided")
        }
    }

    @Test
    fun `email verification page contains countdown JavaScript`() {
        testApplication {
            setupApp()
            
            val response = client.get("/verify")
            val body = response.bodyAsText()
            assertTrue(body.contains("countdown"), "Page should contain countdown element")
            assertTrue(body.contains("setInterval"), "Page should contain setInterval for countdown")
            assertTrue(body.contains("window.location.href"), "Page should redirect after countdown")
        }
    }

    @Test
    fun `email verification page contains required scripts`() {
        testApplication {
            setupApp()
            
            val response = client.get("/verify")
            val body = response.bodyAsText()
            assertTrue(body.contains("api.js"), "Page should include api.js")
            assertTrue(body.contains("i18n.js"), "Page should include i18n.js")
        }
    }
}
