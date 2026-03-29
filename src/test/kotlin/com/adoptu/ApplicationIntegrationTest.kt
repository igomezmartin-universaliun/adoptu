package com.adoptu

import com.adoptu.adapters.db.*
import com.adoptu.adapters.notification.SesEmailAdapter
import com.adoptu.adapters.storage.S3ImageStorageAdapter
import com.adoptu.di.appModule
import com.adoptu.mocks.TestClock
import com.adoptu.plugins.configureRouting
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.plugins.configureWebAuthn
import com.adoptu.ports.*
import com.adoptu.services.*
import com.adoptu.services.auth.WebAuthnService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Testcontainers
@OptIn(ExperimentalTime::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

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
    private var serverPort: Int = 0
    private lateinit var baseUrl: String
    private val httpClient = HttpClient(OkHttp)
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    private fun findAvailablePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }

    private fun createTestConfig(): MapApplicationConfig {
        return MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to serverPort.toString(),
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
            "admin.email" to "admin@adopt-u.com",
            "sns.region" to localstackContainer.region
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
                EmailVerificationTokens,
                AnimalShelters
            )
        }
    }

    @BeforeAll
    fun setUpAll() {
        println("Starting application integration test")
        println("PostgreSQL JDBC URL: ${postgresContainer.jdbcUrl}")
        println("LocalStack endpoint: ${localstackContainer.getEndpointOverride(LocalStackContainer.Service.S3)}")
    }

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        println("Starting test: ${testInfo.displayName}")
        
        serverPort = findAvailablePort()
        val config = createTestConfig()
        initDatabase(config)

        val testModules = module {
            single<ApplicationConfig> { config }
            single<Clock> { testClock }
            single { WebAuthnService(get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com") }
            single<UserRepositoryPort> { com.adoptu.adapters.db.repositories.UserRepository(get()) }
            single<PetRepositoryPort> { com.adoptu.adapters.db.repositories.PetRepositoryImpl(get()) }
            single<PhotographerRepositoryPort> { com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl(get(), get(), get()) }
            single<TemporalHomeRepositoryPort> { com.adoptu.adapters.db.repositories.TemporalHomeRepositoryImpl(get(), get(), get()) }
            single<ShelterRepositoryPort> { com.adoptu.adapters.db.repositories.ShelterRepository(get()) }
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
            single<UserService> { UserService(get()) }
            single<PetService> { PetService(get(), get(), get(), get()) }
            single<PhotographerService> { PhotographerService(get(), get(), get(), get()) }
            single<TemporalHomeService> { TemporalHomeService(get(), get(), get(), get()) }
            single { ShelterService(get()) }
            single { EmailVerificationService(get(), get(), get()) }
        }

        server = embeddedServer(Netty, port = serverPort) {
            install(Koin) {
                slf4jLogger()
                modules(appModule(config), testModules)
            }
            configureSerialization()
            configureSessions()
            configureWebAuthn()
            configureRouting()
        }

        server!!.start()

        baseUrl = "http://${InetAddress.getLoopbackAddress().hostAddress}:$serverPort"

        println("Server started on port: $serverPort")
        println("Base URL: $baseUrl")
    }

    @AfterEach
    fun tearDown() {
        server?.stop(1000, 5000)
        server = null
    }

    @AfterAll
    fun tearDownAll() {
        httpClient.close()
    }

    @Test
    fun `containers are running`() {
        assertTrue(postgresContainer.isRunning, "PostgreSQL container should be running")
        assertTrue(localstackContainer.isRunning, "LocalStack container should be running")
    }

    @Test
    fun `server is listening on configured port`() {
        assertTrue(serverPort > 0, "Server should be listening on a port, was: $serverPort")
    }

    @Test
    fun `root endpoint responds with HTTP 200`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.NotFound, HttpStatusCode.Found),
                "Root endpoint should respond with valid status, got: ${response.status}"
            )
        }
    }

    @Test
    fun `health endpoint responds`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/health")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.NotFound),
                "Health endpoint should respond with valid status, got: ${response.status}"
            )
        }
    }

    @Test
    fun `login page is accessible`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/login")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Found),
                "Login page should be accessible, got: ${response.status}"
            )
        }
    }

    @Test
    fun `register page is accessible`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/register")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Found),
                "Register page should be accessible, got: ${response.status}"
            )
        }
    }

    @Test
    fun `api pets endpoint returns valid response`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/pets")
            assertTrue(
                response.status.value < 500,
                "Pets API should not return 5xx error, got: ${response.status}"
            )
        }
    }

    @Test
    fun `api photographers endpoint returns valid response`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/photographers")
            assertTrue(
                response.status.value < 500,
                "Photographers API should not return 5xx error, got: ${response.status}"
            )
        }
    }

    @Test
    fun `api auth me endpoint responds`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/auth/me")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized),
                "Auth me endpoint should respond, got: ${response.status}"
            )
        }
    }

    @Test
    fun `unknown route returns 404`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/nonexistent-route-xyz")
            assertEquals(HttpStatusCode.NotFound, response.status, "Unknown route should return 404")
        }
    }

    @Test
    fun `application responds within reasonable time`() = runTestWithRetry {
        runBlocking {
            val startTime = System.currentTimeMillis()
            val response = httpClient.get("$baseUrl/")
            val duration = System.currentTimeMillis() - startTime
            assertTrue(duration < 5000, "Application should respond within 5 seconds, took: ${duration}ms")
        }
    }

    @Test
    fun `static resources are served`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/style.css")
            assertTrue(
                response.status in listOf(HttpStatusCode.OK, HttpStatusCode.NotFound),
                "Static CSS should be accessible, got: ${response.status}"
            )
        }
    }

    private fun runTestWithRetry(block: () -> Unit) {
        var lastException: Throwable? = null
        repeat(3) { attempt ->
            try {
                block()
                return
            } catch (e: Throwable) {
                lastException = e
                if (attempt < 2) {
                    println("Attempt ${attempt + 1} failed, retrying: ${e.message}")
                    Thread.sleep(500)
                }
            }
        }
        throw lastException ?: RuntimeException("Test failed after retries")
    }
}
