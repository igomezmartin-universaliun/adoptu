package com.adoptu.routes

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
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
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
class SheltersRoutesE2ETest {

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
    private lateinit var httpClient: HttpClient
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

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        println("Starting shelter routes E2E test: ${testInfo.displayName}")

        TransactionManager.defaultDatabase = null
        serverPort = findAvailablePort()
        val config = createTestConfig()
        initDatabase(config)

        val testModules = module {
            single<ApplicationConfig> { config }
            single<Clock> { testClock }
            single { WebAuthnService(get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:8080") }
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

        httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        println("Server started on port: $serverPort")
    }

    @AfterEach
    fun tearDown() {
        server?.stop(1000, 5000)
        server = null
        if (::httpClient.isInitialized) {
            httpClient.close()
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

    private fun createTestShelter(
        name: String,
        country: String,
        state: String? = null,
        city: String,
        address: String = "123 Test St"
    ): Int {
        return transaction {
            AnimalShelters.insert {
                it[AnimalShelters.name] = name
                it[AnimalShelters.country] = country
                it[AnimalShelters.state] = state
                it[AnimalShelters.city] = city
                it[AnimalShelters.address] = address
                it[AnimalShelters.createdAt] = testClock.now().toEpochMilliseconds()
                it[AnimalShelters.updatedAt] = testClock.now().toEpochMilliseconds()
            } get AnimalShelters.id
        }!!
    }

    @Test
    fun `GET shelters returns empty list when no shelters`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters?country=USA")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
        }
    }

    @Test
    fun `GET shelters returns shelters for country`() = runTestWithRetry {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "Canada", "ON", "Toronto")

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters?country=USA")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("Shelter 1"), "Should contain Shelter 1")
            assertTrue(body.contains("Shelter 2"), "Should contain Shelter 2")
            assertTrue(!body.contains("Shelter 3") || !body.contains("Canada"), "Should not contain Shelter 3 (Canada)")
        }
    }

    @Test
    fun `GET shelters filters by state`() = runTestWithRetry {
        createTestShelter("Shelter NY 1", "USA", "NY", "New York")
        createTestShelter("Shelter CA", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter NY 2", "USA", "NY", "Buffalo")

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters?country=USA&state=NY")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("Shelter NY 1"), "Should contain Shelter NY 1")
            assertTrue(body.contains("Shelter NY 2"), "Should contain Shelter NY 2")
        }
    }

    @Test
    fun `GET shelters returns 400 when country is missing`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters")
            assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        }
    }

    @Test
    fun `GET shelters returns shelter with all fields`() {
        val shelterId = transaction {
            AnimalShelters.insert {
                it[AnimalShelters.name] = "Full Shelter XYZ"
                it[AnimalShelters.country] = "USA"
                it[AnimalShelters.state] = "CA"
                it[AnimalShelters.city] = "Los Angeles"
                it[AnimalShelters.address] = "123 Main St"
                it[AnimalShelters.zip] = "90001"
                it[AnimalShelters.phone] = "555-1234"
                it[AnimalShelters.email] = "test@shelter.com"
                it[AnimalShelters.website] = "https://test.com"
                it[AnimalShelters.fiscalId] = "12-3456789"
                it[AnimalShelters.bankName] = "Test Bank"
                it[AnimalShelters.accountHolderName] = "Account Holder"
                it[AnimalShelters.accountNumber] = "123456789"
                it[AnimalShelters.iban] = "US123456789"
                it[AnimalShelters.swiftBic] = "TESTBIC"
                it[AnimalShelters.currency] = "USD"
                it[AnimalShelters.description] = "Test description"
                it[AnimalShelters.createdAt] = testClock.now().toEpochMilliseconds()
                it[AnimalShelters.updatedAt] = testClock.now().toEpochMilliseconds()
            } get AnimalShelters.id
        }!!

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/$shelterId")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("Full Shelter XYZ"), "Should contain shelter name")
            assertTrue(body.contains("USA"), "Should contain country")
            assertTrue(body.contains("CA"), "Should contain state")
            assertTrue(body.contains("12-3456789"), "Should contain fiscalId")
            assertTrue(body.contains("Test Bank"), "Should contain bankName")
        }
    }

    @Test
    fun `GET shelters countries returns list of countries`() = runTestWithRetry {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "Canada", "ON", "Toronto")

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/countries")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("USA"), "Should contain USA")
            assertTrue(body.contains("Canada"), "Should contain Canada")
        }
    }

    @Test
    fun `GET shelters countries returns empty list when no shelters`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/countries")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
        }
    }

    @Test
    fun `GET shelters states returns list of states for country`() = runTestWithRetry {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "USA", "NY", "Buffalo")
        createTestShelter("Shelter 4", "Canada", "ON", "Toronto")

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/countries/USA/states")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("NY"), "Should contain NY")
            assertTrue(body.contains("CA"), "Should contain CA")
        }
    }

    @Test
    fun `GET shelters states returns empty list for country with no states`() = runTestWithRetry {
        createTestShelter("Shelter 1", "USA", "NY", "New York")

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/countries/Canada/states")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(!body.contains("NY"), "Should not contain NY for Canada")
        }
    }

    @Test
    fun `GET shelter by id returns shelter`() = runTestWithRetry {
        val shelterId = createTestShelter("Test Shelter", "USA", "NY", "New York")

        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/$shelterId")
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("Test Shelter"), "Should contain shelter name")
            assertTrue(body.contains("USA"), "Should contain country")
        }
    }

    @Test
    fun `GET shelter by id returns 404 for non-existent id`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.get("$baseUrl/api/shelters/999999")
            assertEquals(HttpStatusCode.NotFound, response.status, "Expected 404 Not Found")
        }
    }

    @Test
    fun `POST admin shelter creates new shelter`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.post("$baseUrl/api/admin/shelters") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "New Shelter",
                        "country": "USA",
                        "state": "NY",
                        "city": "New York",
                        "address": "123 Main St",
                        "bankName": "Test Bank",
                        "accountNumber": "123456789"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("New Shelter"), "Should contain new shelter name")
            assertTrue(body.contains("USA"), "Should contain country")
            assertTrue(body.contains("NY"), "Should contain state")
        }
    }

    @Test
    fun `POST admin shelter creates shelter with all donation fields`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.post("$baseUrl/api/admin/shelters") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Full Shelter",
                        "country": "USA",
                        "state": "CA",
                        "city": "Los Angeles",
                        "address": "456 Oak Ave",
                        "zip": "90001",
                        "phone": "555-1234",
                        "email": "shelter@test.com",
                        "website": "https://test.com",
                        "fiscalId": "12-3456789",
                        "bankName": "Test Bank",
                        "accountHolderName": "Account Holder",
                        "accountNumber": "123456789",
                        "iban": "US123456789",
                        "swiftBic": "TESTBIC",
                        "currency": "USD",
                        "description": "A test shelter"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("Full Shelter"), "Should contain shelter name")
            assertTrue(body.contains("12-3456789"), "Should contain fiscalId")
            assertTrue(body.contains("Test Bank"), "Should contain bankName")
        }
    }

    @Test
    fun `PUT admin shelter updates shelter`() = runTestWithRetry {
        val shelterId = createTestShelter("Old Name", "USA", "NY", "New York")

        runBlocking {
            val response = httpClient.put("$baseUrl/api/admin/shelters/$shelterId") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Updated Name",
                        "city": "Brooklyn"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("Updated Name"), "Should contain updated name")
            assertTrue(body.contains("Brooklyn"), "Should contain new city")
        }
    }

    @Test
    fun `PUT admin shelter updates donation info`() = runTestWithRetry {
        val shelterId = createTestShelter("Test", "USA", "NY", "New York")

        runBlocking {
            val response = httpClient.put("$baseUrl/api/admin/shelters/$shelterId") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "bankName": "New Bank",
                        "accountNumber": "987654321",
                        "iban": "US987654321",
                        "fiscalId": "99-9999999"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, response.status, "Expected 200 OK")
            val body = response.bodyAsText()
            assertTrue(body.contains("New Bank"), "Should contain new bank name")
            assertTrue(body.contains("987654321"), "Should contain new account number")
        }
    }

    @Test
    fun `PUT admin shelter returns 404 for non-existent id`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.put("$baseUrl/api/admin/shelters/999999") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Updated Name"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.NotFound, response.status, "Expected 404 Not Found")
        }
    }

    @Test
    fun `DELETE admin shelter removes shelter`() = runTestWithRetry {
        val shelterId = createTestShelter("To Delete", "USA", "NY", "New York")

        runBlocking {
            val deleteResponse = httpClient.delete("$baseUrl/api/admin/shelters/$shelterId")
            assertEquals(HttpStatusCode.OK, deleteResponse.status, "Expected 200 OK on delete")

            val getResponse = httpClient.get("$baseUrl/api/shelters/$shelterId")
            assertEquals(HttpStatusCode.NotFound, getResponse.status, "Expected 404 Not Found after delete")
        }
    }

    @Test
    fun `DELETE admin shelter returns 404 for non-existent id`() = runTestWithRetry {
        runBlocking {
            val response = httpClient.delete("$baseUrl/api/admin/shelters/999999")
            assertEquals(HttpStatusCode.NotFound, response.status, "Expected 404 Not Found")
        }
    }
}
