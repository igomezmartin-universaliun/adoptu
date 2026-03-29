package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.dto.input.AcceptTermsRequest
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.UserRole
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.postgresql.Driver
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryIT {

    private var postgresContainer: PostgreSQLContainer<*>? = null
    private lateinit var userRepository: UserRepository
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
        val dbName = "repo_test_${++dbCounter}"
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

    private fun setupRepository() {
        userRepository = UserRepository(clock)
    }

    @BeforeEach
    fun setUp() {
        TransactionManager.defaultDatabase = null
        val dbName = createDatabase()
        initDatabase(dbName)
        setupRepository()
    }

    @Test
    fun `getById returns null for non-existent user`() {
        val result = userRepository.getById(999)
        assertNull(result)
    }

    @Test
    fun `getById returns user by id`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = userRepository.getById(userId)

        assertNotNull(result)
        assertEquals("test@example.com", result.username)
        assertEquals("Test User", result.displayName)
    }

    @Test
    fun `getById returns user with roles`() {
        val userId = createTestUser("test@example.com", "Test User")
        addRoleToUser(userId, UserRole.RESCUER)

        val result = userRepository.getById(userId)

        assertNotNull(result)
        assertTrue(result.activeRoles.contains(UserRole.RESCUER))
    }

    @Test
    fun `isRoleActive returns true for active role`() {
        val userId = createTestUser("test@example.com", "Test User")
        addRoleToUser(userId, UserRole.RESCUER)

        val result = userRepository.isRoleActive(userId, UserRole.RESCUER)

        assertTrue(result)
    }

    @Test
    fun `isRoleActive returns false for inactive role`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = userRepository.isRoleActive(userId, UserRole.RESCUER)

        assertTrue(!result)
    }

    @Test
    fun `activateRescuerProfile adds rescuer role`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = userRepository.activateRescuerProfile(userId)

        assertNotNull(result)
        assertTrue(result.activeRoles.contains(UserRole.RESCUER))
    }

    @Test
    fun `deactivateRescuerProfile removes rescuer role`() {
        val userId = createTestUser("test@example.com", "Test User")
        userRepository.activateRescuerProfile(userId)

        val result = userRepository.deactivateRescuerProfile(userId)

        assertNotNull(result)
        assertTrue(!result.activeRoles.contains(UserRole.RESCUER))
    }

    @Test
    fun `activateTemporalHomeProfile adds temporal home role`() {
        val userId = createTestUser("test@example.com", "Test Temp Home")

        val result = userRepository.activateTemporalHomeProfile(userId)

        assertNotNull(result)
        assertTrue(result.activeRoles.contains(UserRole.TEMPORAL_HOME))
    }

    @Test
    fun `deactivateTemporalHomeProfile removes temporal home role`() {
        val userId = createTestUser("test@example.com", "Test Temp Home")
        userRepository.activateTemporalHomeProfile(userId)

        val result = userRepository.deactivateTemporalHomeProfile(userId)

        assertNotNull(result)
        assertTrue(!result.activeRoles.contains(UserRole.TEMPORAL_HOME))
    }

    @Test
    fun `updateProfile updates display name`() {
        val userId = createTestUser("test@example.com", "Old Name")

        val result = userRepository.updateProfile(userId, "New Name")

        assertNotNull(result)
        assertEquals("New Name", result.displayName)
    }

    @Test
    fun `updateProfile returns null for non-existent user`() {
        val result = userRepository.updateProfile(999, "New Name")
        assertNull(result)
    }

    @Test
    fun `updateProfile throws for blank name`() {
        val userId = createTestUser("test@example.com", "Old Name")

        try {
            userRepository.updateProfile(userId, "")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("Display name cannot be empty", e.message)
        }
    }

    @Test
    fun `updateLanguage updates user language`() {
        val userId = createTestUser("test@example.com", "Test User")

        val result = userRepository.updateLanguage(userId, "es")

        assertNotNull(result)
        assertEquals("es", result.language)
    }

    @Test
    fun `acceptTerms updates privacy policy timestamp`() {
        val userId = createTestUser("test@example.com", "Test User")
        val beforeTime = clock.now().toEpochMilliseconds()

        val result = userRepository.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = false
        ))

        assertNotNull(result)
        assertNotNull(result.lastAcceptedPrivacyPolicy)
        assertTrue(result.lastAcceptedPrivacyPolicy!! >= beforeTime)
    }

    @Test
    fun `acceptTerms updates terms timestamp`() {
        val userId = createTestUser("test@example.com", "Test User")
        val beforeTime = clock.now().toEpochMilliseconds()

        val result = userRepository.acceptTerms(userId, AcceptTermsRequest(
            acceptPrivacyPolicy = false,
            acceptTermsAndConditions = true
        ))

        assertNotNull(result)
        assertNotNull(result.lastAcceptedTermsAndConditions)
        assertTrue(result.lastAcceptedTermsAndConditions!! >= beforeTime)
    }

    @Test
    fun `acceptTerms returns null for non-existent user`() {
        val result = userRepository.acceptTerms(999, AcceptTermsRequest(
            acceptPrivacyPolicy = true,
            acceptTermsAndConditions = true
        ))
        assertNull(result)
    }

    @Test
    fun `getRescuers returns only users with rescuer role`() {
        val rescuerId = createTestUser("rescuer@example.com", "Rescuer")
        addRoleToUser(rescuerId, UserRole.RESCUER)
        createTestUser("user@example.com", "Regular User")

        val result = userRepository.getRescuers()

        assertEquals(1, result.size)
        assertEquals("rescuer@example.com", result.first().username)
    }

    @Test
    fun `updatePhotographerSettings creates photographer record`() {
        val userId = createTestUser("photo@example.com", "Photographer")

        val result = userRepository.updatePhotographerSettings(userId, PhotographerSettingsRequest(
            photographerFee = 50.0,
            photographerCurrency = "USD",
            country = "USA",
            state = "CA"
        ))

        assertNotNull(result)
        assertEquals(50.0, result.photographerFee)
        assertEquals("USD", result.photographerCurrency)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
    }

    @Test
    fun `updatePhotographerSettings throws for negative fee`() {
        val userId = createTestUser("photo@example.com", "Photographer")

        try {
            userRepository.updatePhotographerSettings(userId, PhotographerSettingsRequest(
                photographerFee = -10.0,
                photographerCurrency = "USD"
            ))
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("Photographer fee must be zero or positive", e.message)
        }
    }

    @Test
    fun `updatePhotographerSettings returns null for non-existent user`() {
        val result = userRepository.updatePhotographerSettings(999, PhotographerSettingsRequest(
            photographerFee = 50.0,
            photographerCurrency = "USD"
        ))
        assertNull(result)
    }

    @Test
    fun `getPhotographers returns only users with photographer role`() {
        val photographerId = createTestUser("photo1@example.com", "Photo 1")
        addRoleToUser(photographerId, UserRole.PHOTOGRAPHER)
        createTestUser("user@example.com", "Regular User")

        val result = userRepository.getPhotographers()

        assertEquals(1, result.size)
        assertEquals("photo1@example.com", result.first().username)
    }

    private fun createTestUser(username: String, displayName: String): Int {
        return transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = displayName
                it[Users.createdAt] = clock.now().toEpochMilliseconds()
            } get Users.id
        }!!
    }

    private fun addRoleToUser(userId: Int, role: UserRole) {
        transaction {
            UserActiveRoles.insert {
                it[UserActiveRoles.userId] = userId
                it[UserActiveRoles.role] = role.name
            }
        }
    }
}
