package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.BlockedRescuers
import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.adapters.db.Pets
import com.adoptu.adapters.db.TemporalHomeRequests
import com.adoptu.adapters.db.TemporalHomes
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.dto.CreateTemporalHomeRequest
import com.adoptu.dto.TemporalHomeSearchParams
import com.adoptu.dto.UpdateTemporalHomeRequest
import com.adoptu.dto.UserRole
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.postgresql.Driver
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporalHomeRepositoryIT {

    private var postgresContainer: PostgreSQLContainer<*>? = null
    private lateinit var temporalHomeRepository: TemporalHomeRepositoryImpl
    private lateinit var userRepository: UserRepository
    private lateinit var petRepository: PetRepositoryImpl
    private var dbCounter = 0

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
        val dbName = "temporal_repo_test_${++dbCounter}"
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
        petRepository = PetRepositoryImpl()
        userRepository = UserRepository()
        temporalHomeRepository = TemporalHomeRepositoryImpl(petRepository, userRepository)
    }

    @Test
    fun `getTemporalHome returns null when no temporal home exists`() {
        val userId = createTestUserWithRole("user@example.com", "Test User", UserRole.ADOPTER)

        val result = temporalHomeRepository.getTemporalHome(userId)

        assertNull(result)
    }

    @Test
    fun `getTemporalHome returns temporal home by userId`() {
        val userId = createTestUserWithRole("user@example.com", "Test User", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId, "My Home", "USA", "CA", "Los Angeles", "90001", "Downtown")

        val result = temporalHomeRepository.getTemporalHome(userId)

        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertEquals("My Home", result.alias)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
        assertEquals("Los Angeles", result.city)
    }

    @Test
    fun `createTemporalHome creates new temporal home`() {
        val userId = createTestUserWithRole("user@example.com", "Test User", UserRole.TEMPORAL_HOME)

        val result = temporalHomeRepository.createTemporalHome(userId, CreateTemporalHomeRequest(
            alias = "My Home",
            country = "USA",
            state = "CA",
            city = "Los Angeles",
            zip = "90001",
            neighborhood = "Downtown"
        ))

        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("My Home", result.alias)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
        assertEquals("Los Angeles", result.city)
        assertEquals("90001", result.zip)
        assertEquals("Downtown", result.neighborhood)
    }

    @Test
    fun `updateTemporalHome updates existing temporal home`() {
        val userId = createTestUserWithRole("user@example.com", "Test User", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId, "Old Name", "USA", "CA", "LA", "90001", "Old")

        val result = temporalHomeRepository.updateTemporalHome(userId, UpdateTemporalHomeRequest(
            alias = "New Name",
            city = "Los Angeles"
        ))

        assertNotNull(result)
        assertEquals("New Name", result!!.alias)
        assertEquals("Los Angeles", result.city)
        assertEquals("CA", result.state)
    }

    @Test
    fun `updateTemporalHome returns null for non-existent temporal home`() {
        val result = temporalHomeRepository.updateTemporalHome(999, UpdateTemporalHomeRequest(
            alias = "New Name"
        ))
        assertNull(result)
    }

    @Test
    fun `searchTemporalHomes returns all temporal homes when no filters`() {
        val userId1 = createTestUserWithRole("user1@example.com", "User 1", UserRole.TEMPORAL_HOME)
        val userId2 = createTestUserWithRole("user2@example.com", "User 2", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId1, "Home 1", "USA", "CA", "LA", "90001", "Downtown")
        createTemporalHome(userId2, "Home 2", "USA", "NY", "NYC", "10001", "Manhattan")

        val result = temporalHomeRepository.searchTemporalHomes(TemporalHomeSearchParams())

        assertEquals(2, result.size)
    }

    @Test
    fun `searchTemporalHomes filters by country`() {
        val userId1 = createTestUserWithRole("user1@example.com", "User 1", UserRole.TEMPORAL_HOME)
        val userId2 = createTestUserWithRole("user2@example.com", "User 2", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId1, "Home 1", "USA", "CA", "LA", "90001", "Downtown")
        createTemporalHome(userId2, "Home 2", "Canada", "ON", "Toronto", "M5V", "Downtown")

        val result = temporalHomeRepository.searchTemporalHomes(TemporalHomeSearchParams(country = "USA"))

        assertEquals(1, result.size)
        assertEquals("USA", result.first().country)
    }

    @Test
    fun `searchTemporalHomes filters by state`() {
        val userId1 = createTestUserWithRole("user1@example.com", "User 1", UserRole.TEMPORAL_HOME)
        val userId2 = createTestUserWithRole("user2@example.com", "User 2", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId1, "Home 1", "USA", "CA", "LA", "90001", "Downtown")
        createTemporalHome(userId2, "Home 2", "USA", "NY", "NYC", "10001", "Manhattan")

        val result = temporalHomeRepository.searchTemporalHomes(TemporalHomeSearchParams(state = "CA"))

        assertEquals(1, result.size)
        assertEquals("CA", result.first().state)
    }

    @Test
    fun `searchTemporalHomes filters by city`() {
        val userId1 = createTestUserWithRole("user1@example.com", "User 1", UserRole.TEMPORAL_HOME)
        val userId2 = createTestUserWithRole("user2@example.com", "User 2", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId1, "Home 1", "USA", "CA", "Los Angeles", "90001", "Downtown")
        createTemporalHome(userId2, "Home 2", "USA", "CA", "San Francisco", "94102", "SOMA")

        val result = temporalHomeRepository.searchTemporalHomes(TemporalHomeSearchParams(city = "Los Angeles"))

        assertEquals(1, result.size)
        assertEquals("Los Angeles", result.first().city)
    }

    @Test
    fun `searchTemporalHomes filters by zip`() {
        val userId1 = createTestUserWithRole("user1@example.com", "User 1", UserRole.TEMPORAL_HOME)
        val userId2 = createTestUserWithRole("user2@example.com", "User 2", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId1, "Home 1", "USA", "CA", "LA", "90001", "Downtown")
        createTemporalHome(userId2, "Home 2", "USA", "CA", "LA", "90210", "Beverly Hills")

        val result = temporalHomeRepository.searchTemporalHomes(TemporalHomeSearchParams(zip = "90001"))

        assertEquals(1, result.size)
        assertEquals("90001", result.first().zip)
    }

    @Test
    fun `searchTemporalHomes filters by neighborhood`() {
        val userId1 = createTestUserWithRole("user1@example.com", "User 1", UserRole.TEMPORAL_HOME)
        val userId2 = createTestUserWithRole("user2@example.com", "User 2", UserRole.TEMPORAL_HOME)
        createTemporalHome(userId1, "Home 1", "USA", "CA", "LA", "90001", "Downtown")
        createTemporalHome(userId2, "Home 2", "USA", "CA", "LA", "90210", "Beverly Hills")

        val result = temporalHomeRepository.searchTemporalHomes(TemporalHomeSearchParams(neighborhood = "Downtown"))

        assertEquals(1, result.size)
        assertEquals("Downtown", result.first().neighborhood)
    }

    @Test
    fun `createTemporalHomeRequest creates request`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
        createTemporalHome(temporalHomeId, "My Home", "USA", "CA", "LA", "90001", "Downtown")

        val result = temporalHomeRepository.createTemporalHomeRequest(
            temporalHomeId = temporalHomeId,
            rescuerId = rescuerId,
            petId = null,
            message = "Need help!"
        )

        assertTrue(result > 0)
    }

    @Test
    fun `isBlocked returns false when not blocked`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)

        val result = temporalHomeRepository.isBlocked(temporalHomeId, rescuerId)

        assertTrue(!result)
    }

    @Test
    fun `isBlocked returns true when blocked`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
        temporalHomeRepository.blockRescuer(temporalHomeId, rescuerId)

        val result = temporalHomeRepository.isBlocked(temporalHomeId, rescuerId)

        assertTrue(result)
    }

    @Test
    fun `blockRescuer blocks rescuer`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)

        val result = temporalHomeRepository.blockRescuer(temporalHomeId, rescuerId)

        assertTrue(result)
        assertTrue(temporalHomeRepository.isBlocked(temporalHomeId, rescuerId))
    }

    @Test
    fun `blockRescuer returns false when already blocked`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
        temporalHomeRepository.blockRescuer(temporalHomeId, rescuerId)

        val result = temporalHomeRepository.blockRescuer(temporalHomeId, rescuerId)

        assertTrue(!result)
    }

    @Test
    fun `getMyRequests returns requests for temporal home owner`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
        createTemporalHome(temporalHomeId, "My Home", "USA", "CA", "LA", "90001", "Downtown")
        temporalHomeRepository.createTemporalHomeRequest(temporalHomeId, rescuerId, null, "Help needed!")

        val result = temporalHomeRepository.getMyRequests(temporalHomeId)

        assertEquals(1, result.size)
        assertEquals(rescuerId, result.first().rescuerId)
        assertEquals("Help needed!", result.first().message)
        assertEquals("SENT", result.first().status)
    }

    @Test
    fun `getMyRequests includes rescuer name`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
        createTemporalHome(temporalHomeId, "My Home", "USA", "CA", "LA", "90001", "Downtown")
        temporalHomeRepository.createTemporalHomeRequest(temporalHomeId, rescuerId, null, "Help needed!")

        val result = temporalHomeRepository.getMyRequests(temporalHomeId)

        assertEquals(1, result.size)
        assertEquals("Test Rescuer", result.first().rescuerName)
    }

    @Test
    fun `getMyRequests returns empty list when no requests`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        createTemporalHome(temporalHomeId, "My Home", "USA", "CA", "LA", "90001", "Downtown")

        val result = temporalHomeRepository.getMyRequests(temporalHomeId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMyRequests includes pet info when petId provided`() {
        val temporalHomeId = createTestUserWithRole("home@example.com", "Test Home", UserRole.TEMPORAL_HOME)
        val rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
        createTemporalHome(temporalHomeId, "My Home", "USA", "CA", "LA", "90001", "Downtown")
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        temporalHomeRepository.createTemporalHomeRequest(temporalHomeId, rescuerId, petId, "Help needed!")

        val result = temporalHomeRepository.getMyRequests(temporalHomeId)

        assertEquals(1, result.size)
        assertEquals(petId, result.first().petId)
        assertEquals("Buddy", result.first().petName)
    }

    private fun createTestUserWithRole(username: String, displayName: String, role: UserRole): Int {
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = System.currentTimeMillis()
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

    private fun createTemporalHome(
        userId: Int,
        alias: String,
        country: String,
        state: String,
        city: String,
        zip: String,
        neighborhood: String
    ) {
        transaction {
            TemporalHomes.insert {
                it[TemporalHomes.userId] = userId
                it[TemporalHomes.alias] = alias
                it[TemporalHomes.country] = country
                it[TemporalHomes.state] = state
                it[TemporalHomes.city] = city
                it[TemporalHomes.zip] = zip
                it[TemporalHomes.neighborhood] = neighborhood
                it[TemporalHomes.createdAt] = System.currentTimeMillis()
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
                it[Pets.createdAt] = System.currentTimeMillis()
            } get Pets.id
        }!!
    }
}
