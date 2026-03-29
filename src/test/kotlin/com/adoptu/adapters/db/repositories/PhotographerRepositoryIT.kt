package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.*
import com.adoptu.dto.input.UserRole
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.postgresql.Driver
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotographerRepositoryIT {

    private var postgresContainer: PostgreSQLContainer<*>? = null
    private lateinit var photographerRepository: PhotographerRepositoryImpl
    private lateinit var userRepository: UserRepository
    private lateinit var petRepository: PetRepositoryImpl
    private var dbCounter = 0
    private val clock = Clock.System

    @BeforeAll
    fun startContainer() {
        postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("postgres")
            .withUsername("testuser")
            .withPassword("testpassword")
            .withExposedPorts(5432)

        postgresContainer!!.start()
    }

    @AfterAll
    fun stopContainer() {
        postgresContainer?.stop()
    }

    private fun createConfig(databaseName: String): ApplicationConfig {
        return MapApplicationConfig(
            "env" to "test",
            "db.test.postgres.driver" to "org.postgresql.Driver",
            "db.test.postgres.url" to "jdbc:postgresql://${postgresContainer!!.host}:${postgresContainer!!.firstMappedPort}/$databaseName",
            "db.test.postgres.user" to (postgresContainer?.username ?: "testuser"),
            "db.test.postgres.password" to (postgresContainer?.password ?: "testpassword")
        )
    }

    private fun createDatabase(): String {
        val dbName = "photo_repo_test_${++dbCounter}"
        val host = postgresContainer!!.host
        val port = postgresContainer!!.firstMappedPort

        val driver = Driver()
        val props = Properties().apply {
            setProperty("user", "testuser")
            setProperty("password", "testpassword")
        }

        val conn = driver.connect("jdbc:postgresql://$host:$port/postgres", props)!!
        conn.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute("CREATE DATABASE $dbName")
            }
        }
        return dbName
    }

    private fun initDatabase(dbName: String) {
        val config = createConfig(dbName)
        DatabaseFactory.init(config)
    }

    @BeforeEach
    fun setUp() {
        TransactionManager.defaultDatabase = null
        val dbName = createDatabase()
        initDatabase(dbName)
        petRepository = PetRepositoryImpl(clock)
        userRepository = UserRepository(clock)
        photographerRepository = PhotographerRepositoryImpl(petRepository, userRepository, clock)
    }

    @Test
    fun `canSendMessage returns true when no recent requests`() {
        val userId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)

        val result = photographerRepository.canSendMessage(userId)

        assertTrue(result)
    }

    @Test
    fun `canSendMessage returns false when request sent within week`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")

        createPhotographyRequest(requesterId, photographerId, null, "Test message")

        val result = photographerRepository.canSendMessage(requesterId)

        assertTrue(!result)
    }

    @Test
    fun `canSendMessage returns true when request older than week`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")

        val oneWeekAgo = clock.now().toEpochMilliseconds() - (7 * 24 * 60 * 60 * 1000L + 1000)
        createPhotographyRequestWithTimestamp(requesterId, photographerId, null, "Test message", oneWeekAgo)

        val result = photographerRepository.canSendMessage(requesterId)

        assertTrue(result)
    }

    @Test
    fun `createPhotographyRequest creates request`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")

        val result = photographerRepository.createPhotographyRequest(
            requesterId = requesterId,
            photographerId = photographerId,
            petId = null,
            message = "Please take photos"
        )

        assertTrue(result > 0)
    }

    @Test
    fun `getMyRequests returns requests for user`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")
        photographerRepository.createPhotographyRequest(requesterId, photographerId, null, "Request 1")

        val result = photographerRepository.getMyRequests(requesterId)

        assertEquals(1, result.size)
        assertEquals(photographerId, result.first().photographerId)
        assertEquals("Request 1", result.first().message)
    }

    @Test
    fun `getMyRequests returns empty list when no requests`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)

        val result = photographerRepository.getMyRequests(requesterId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRequestsForPhotographer returns requests for photographer`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")
        photographerRepository.createPhotographyRequest(requesterId, photographerId, null, "Request 1")

        val result = photographerRepository.getRequestsForPhotographer(photographerId)

        assertEquals(1, result.size)
        assertEquals(requesterId, result.first().requesterId)
    }

    @Test
    fun `getRequestsForPhotographer returns empty list when no requests`() {
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")

        val result = photographerRepository.getRequestsForPhotographer(photographerId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPhotographerById returns photographer data for photographer user`() {
        val userId = createTestUserWithRole("photo@example.com", "Photographer", UserRole.PHOTOGRAPHER)
        addPhotographerSettings(userId, 50.0, "USD", "USA", "CA")

        val result = photographerRepository.getPhotographerById(userId)

        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertEquals("Photographer", result.displayName)
        assertEquals(50.0, result.photographerFee)
        assertEquals("USD", result.photographerCurrency)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
    }

    @Test
    fun `getPhotographerById returns null for non-photographer user`() {
        val userId = createTestUserWithRole("user@example.com", "Regular User", UserRole.ADOPTER)

        val result = photographerRepository.getPhotographerById(userId)

        assertNull(result)
    }

    @Test
    fun `getPhotographerById returns null for non-existent user`() {
        val result = photographerRepository.getPhotographerById(999)
        assertNull(result)
    }

    @Test
    fun `getPhotographers returns only photographers`() {
        createTestUserWithRole("user@example.com", "Regular User", UserRole.ADOPTER)
        createTestPhotographer("photo@example.com", "Photo 1")
        createTestPhotographer("photo2@example.com", "Photo 2")

        val result = photographerRepository.getPhotographers()

        assertEquals(2, result.size)
    }

    @Test
    fun `getPhotographers filters by country`() {
        createTestPhotographerWithCountry("photo1@example.com", "Photo 1", "USA", null)
        createTestPhotographerWithCountry("photo2@example.com", "Photo 2", "Canada", null)

        val result = photographerRepository.getPhotographers(country = "USA")

        assertEquals(1, result.size)
        assertEquals("USA", result.first().country)
    }

    @Test
    fun `getPhotographers filters by state`() {
        createTestPhotographerWithCountry("photo1@example.com", "Photo 1", "USA", "CA")
        createTestPhotographerWithCountry("photo2@example.com", "Photo 2", "USA", "NY")

        val result = photographerRepository.getPhotographers(country = "USA", state = "CA")

        assertEquals(1, result.size)
        assertEquals("CA", result.first().state)
    }

    @Test
    fun `getPhotographers returns empty list when no photographers`() {
        createTestUserWithRole("user@example.com", "Regular User", UserRole.ADOPTER)

        val result = photographerRepository.getPhotographers()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRequestsForPhotographer includes pet info when petId provided`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")
        val petId = createTestPet(requesterId, "Buddy", "DOG")

        photographerRepository.createPhotographyRequest(requesterId, photographerId, petId, "Request")

        val result = photographerRepository.getRequestsForPhotographer(photographerId)

        assertEquals(1, result.size)
        assertEquals(petId, result.first().petId)
        assertEquals("Buddy", result.first().petName)
    }

    @Test
    fun `getMyRequests includes photographer name`() {
        val requesterId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)
        val photographerId = createTestPhotographer("photo@example.com", "Photographer")

        photographerRepository.createPhotographyRequest(requesterId, photographerId, null, "Request")

        val result = photographerRepository.getMyRequests(requesterId)

        assertEquals(1, result.size)
        assertEquals("Photographer", result.first().photographerName)
    }

    private fun createTestUserWithRole(username: String, displayName: String, role: UserRole): Int {
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
            } get Users.id
        }!!

        transaction {
            UserActiveRoles.insert {
                it[UserActiveRoles.userId] = userId
                it[UserActiveRoles.role] = role.name
            }
        }

        return userId
    }

    private fun createTestPhotographer(username: String, displayName: String): Int {
        return createTestUserWithRole(username, displayName, UserRole.PHOTOGRAPHER)
    }

    private fun createTestPhotographerWithCountry(username: String, displayName: String, country: String, state: String?): Int {
        val userId = createTestUserWithRole(username, displayName, UserRole.PHOTOGRAPHER)
        addPhotographerSettings(userId, 0.0, "USD", country, state)
        return userId
    }

    private fun addPhotographerSettings(userId: Int, fee: Double, currency: String, country: String?, state: String?) {
        transaction {
            Photographers.insert {
                it[Photographers.userId] = userId
                it[Photographers.photographerFee] = BigDecimal(fee.toString())
                it[Photographers.photographerCurrency] = currency
                it[Photographers.country] = country
                it[Photographers.state] = state
            }
        }
    }

    private fun createPhotographyRequest(requesterId: Int, photographerId: Int, petId: Int?, message: String) {
        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.petId] = petId
                it[PhotographyRequests.message] = message
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = clock.now().toEpochMilliseconds()
            }
        }
    }

    private fun createPhotographyRequestWithTimestamp(requesterId: Int, photographerId: Int, petId: Int?, message: String, createdAt: Long) {
        transaction {
            PhotographyRequests.insert {
                it[PhotographyRequests.requesterId] = requesterId
                it[PhotographyRequests.photographerId] = photographerId
                it[PhotographyRequests.petId] = petId
                it[PhotographyRequests.message] = message
                it[PhotographyRequests.status] = "PENDING"
                it[PhotographyRequests.createdAt] = createdAt
            }
        }
    }

    private fun createTestPet(rescuerId: Int, name: String, type: String): Int {
        return transaction {
            Pets.insert {
                it[Pets.rescuerId] = rescuerId
                it[Pets.name] = name
                it[Pets.type] = type
                it[Pets.description] = "Test description"
                it[Pets.weight] = BigDecimal("10.0")
                it[Pets.ageYears] = 2
                it[Pets.ageMonths] = 0
                it[Pets.sex] = "MALE"
                it[Pets.breed] = "Test breed"
                it[Pets.status] = "AVAILABLE"
                it[Pets.size] = "MEDIUM"
                it[Pets.isUrgent] = false
                it[Pets.isPromoted] = false
                it[Pets.adoptionFee] = BigDecimal("100.0")
                it[Pets.currency] = "USD"
                it[Pets.createdAt] = clock.now().toEpochMilliseconds()
            } get Pets.id
        }!!
    }
}
