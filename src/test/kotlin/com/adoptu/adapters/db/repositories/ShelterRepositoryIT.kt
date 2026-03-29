package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.UpdateShelterRequest
import io.ktor.server.config.*
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
class ShelterRepositoryIT {

    private var postgresContainer: PostgreSQLContainer<*>? = null
    private lateinit var shelterRepository: ShelterRepository
    private var dbCounter = 0
    private val clock: Clock = Clock.System

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
        val dbName = "shelter_repo_test_${++dbCounter}"
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
        val dbName = createDatabase()
        initDatabase(dbName)
        shelterRepository = ShelterRepository(clock)
    }

    @Test
    fun `getAll returns empty list when no shelters exist`() {
        val result = shelterRepository.getAll("USA")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll returns shelters for given country`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "Canada", "ON", "Toronto")

        val result = shelterRepository.getAll("USA")

        assertEquals(2, result.size)
        assertTrue(result.all { it.country == "USA" })
    }

    @Test
    fun `getAll filters by state when provided`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "USA", "NY", "Buffalo")

        val result = shelterRepository.getAll("USA", "NY")

        assertEquals(2, result.size)
        assertTrue(result.all { it.state == "NY" })
    }

    @Test
    fun `getAll treats empty state as null`() {
        createTestShelter("Shelter 1", "USA", null, "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")

        val result = shelterRepository.getAll("USA", "")

        assertEquals(2, result.size)
    }

    @Test
    fun `getAll returns empty list for non-existent country`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")

        val result = shelterRepository.getAll("France")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getById returns shelter by id`() {
        val shelterId = createTestShelter("Test Shelter", "USA", "NY", "New York")

        val result = shelterRepository.getById(shelterId)

        assertNotNull(result)
        assertEquals(shelterId, result.id)
        assertEquals("Test Shelter", result.name)
    }

    @Test
    fun `getById returns null for non-existent id`() {
        val result = shelterRepository.getById(999)
        assertNull(result)
    }

    @Test
    fun `create creates new shelter with all fields`() {
        val request = CreateShelterRequest(
            name = "New Shelter",
            country = "USA",
            state = "CA",
            city = "Los Angeles",
            address = "123 Main St",
            zip = "90001",
            phone = "555-1234",
            email = "shelter@test.com",
            website = "https://test.com",
            fiscalId = "12-3456789",
            bankName = "Test Bank",
            accountHolderName = "Shelter Account",
            accountNumber = "123456789",
            iban = "US123456789",
            swiftBic = "TESTBIC",
            currency = "USD",
            description = "A test shelter"
        )

        val result = shelterRepository.create(request)

        assertNotNull(result)
        assertEquals("New Shelter", result.name)
        assertEquals("USA", result.country)
        assertEquals("CA", result.state)
        assertEquals("Los Angeles", result.city)
        assertEquals("123 Main St", result.address)
        assertEquals("90001", result.zip)
        assertEquals("555-1234", result.phone)
        assertEquals("shelter@test.com", result.email)
        assertEquals("https://test.com", result.website)
        assertEquals("12-3456789", result.fiscalId)
        assertEquals("Test Bank", result.bankName)
        assertEquals("Shelter Account", result.accountHolderName)
        assertEquals("123456789", result.accountNumber)
        assertEquals("US123456789", result.iban)
        assertEquals("TESTBIC", result.swiftBic)
        assertEquals("USD", result.currency)
        assertEquals("A test shelter", result.description)
        assertTrue(result.id > 0)
    }

    @Test
    fun `create creates shelter with minimal fields`() {
        val request = CreateShelterRequest(
            name = "Minimal Shelter",
            country = "USA",
            city = "NYC",
            address = "456 Oak Ave"
        )

        val result = shelterRepository.create(request)

        assertNotNull(result)
        assertEquals("Minimal Shelter", result.name)
        assertEquals("USA", result.country)
        assertNull(result.state)
        assertEquals("NYC", result.city)
        assertNull(result.zip)
        assertNull(result.phone)
        assertNull(result.email)
    }

    @Test
    fun `update updates shelter successfully`() {
        val shelterId = createTestShelter("Old Name", "USA", "NY", "New York")

        val result = shelterRepository.update(shelterId, UpdateShelterRequest(
            name = "Updated Name",
            city = "Brooklyn"
        ))

        assertNotNull(result)
        assertEquals("Updated Name", result.name)
        assertEquals("Brooklyn", result.city)
        assertEquals("NY", result.state)
    }

    @Test
    fun `update returns null for non-existent id`() {
        val result = shelterRepository.update(999, UpdateShelterRequest(name = "New Name"))
        assertNull(result)
    }

    @Test
    fun `update only modifies specified fields`() {
        val shelterId = createTestShelter(
            "Original", "USA", "NY", "New York",
            phone = "555-0000", email = "old@test.com",
            bankName = "Old Bank", accountNumber = "000000"
        )

        val result = shelterRepository.update(shelterId, UpdateShelterRequest(
            name = "Updated",
            phone = "555-1111"
        ))

        assertNotNull(result)
        assertEquals("Updated", result.name)
        assertEquals("USA", result.country)
        assertEquals("NY", result.state)
        assertEquals("New York", result.city)
        assertEquals("555-1111", result.phone)
        assertEquals("old@test.com", result.email)
        assertEquals("Old Bank", result.bankName)
        assertEquals("000000", result.accountNumber)
    }

    @Test
    fun `update updates bank details`() {
        val shelterId = createTestShelter("Test", "USA", "NY", "New York")

        val result = shelterRepository.update(shelterId, UpdateShelterRequest(
            bankName = "New Bank",
            accountNumber = "987654321",
            iban = "US999999999",
            swiftBic = "NEWBIC",
            fiscalId = "99-9999999"
        ))

        assertNotNull(result)
        assertEquals("New Bank", result.bankName)
        assertEquals("987654321", result.accountNumber)
        assertEquals("US999999999", result.iban)
        assertEquals("NEWBIC", result.swiftBic)
        assertEquals("99-9999999", result.fiscalId)
    }

    @Test
    fun `delete removes shelter successfully`() {
        val shelterId = createTestShelter("To Delete", "USA", "NY", "New York")

        val result = shelterRepository.delete(shelterId)

        assertTrue(result)
        assertNull(shelterRepository.getById(shelterId))
    }

    @Test
    fun `delete returns false for non-existent id`() {
        val result = shelterRepository.delete(999)
        assertTrue(!result)
    }

    @Test
    fun `delete returns false when shelter already deleted`() {
        val shelterId = createTestShelter("To Delete", "USA", "NY", "New York")
        shelterRepository.delete(shelterId)

        val result = shelterRepository.delete(shelterId)
        assertTrue(!result)
    }

    @Test
    fun `getCountries returns distinct countries`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "Canada", "ON", "Toronto")
        createTestShelter("Shelter 4", "USA", "TX", "Houston")

        val result = shelterRepository.getCountries()

        assertEquals(2, result.size)
        assertTrue(result.contains("USA"))
        assertTrue(result.contains("Canada"))
    }

    @Test
    fun `getCountries returns sorted list`() {
        createTestShelter("Shelter 1", "Zimbabwe", "Harare", "Harare")
        createTestShelter("Shelter 2", "Argentina", "BA", "Buenos Aires")
        createTestShelter("Shelter 3", "Mexico", "DF", "Mexico City")

        val result = shelterRepository.getCountries()

        assertEquals(3, result.size)
        assertEquals("Argentina", result[0])
        assertEquals("Mexico", result[1])
        assertEquals("Zimbabwe", result[2])
    }

    @Test
    fun `getStatesByCountry returns states for country`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", "CA", "Los Angeles")
        createTestShelter("Shelter 3", "USA", "NY", "Buffalo")
        createTestShelter("Shelter 4", "Canada", "ON", "Toronto")

        val result = shelterRepository.getStatesByCountry("USA")

        assertEquals(2, result.size)
        assertTrue(result.contains("NY"))
        assertTrue(result.contains("CA"))
    }

    @Test
    fun `getStatesByCountry excludes null states`() {
        createTestShelter("Shelter 1", "USA", "NY", "New York")
        createTestShelter("Shelter 2", "USA", null, "Unknown City")
        createTestShelter("Shelter 3", "USA", "CA", "Los Angeles")

        val result = shelterRepository.getStatesByCountry("USA")

        assertEquals(2, result.size)
        assertTrue(result.contains("NY"))
        assertTrue(result.contains("CA"))
    }

    @Test
    fun `getStatesByCountry returns empty for non-existent country`() {
        val result = shelterRepository.getStatesByCountry("France")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStatesByCountry returns sorted list`() {
        createTestShelter("Shelter 1", "USA", "Texas", "Houston")
        createTestShelter("Shelter 2", "USA", "Alaska", "Anchorage")
        createTestShelter("Shelter 3", "USA", "Florida", "Miami")

        val result = shelterRepository.getStatesByCountry("USA")

        assertEquals(3, result.size)
        assertEquals("Alaska", result[0])
        assertEquals("Florida", result[1])
        assertEquals("Texas", result[2])
    }

    @Test
    fun `created shelter has correct timestamps`() {
        val request = CreateShelterRequest(
            name = "Timestamp Test",
            country = "USA",
            city = "NYC",
            address = "123 Test"
        )

        val result = shelterRepository.create(request)

        assertTrue(result.createdAt > 0)
        assertTrue(result.updatedAt > 0)
        assertEquals(result.createdAt, result.updatedAt)
    }

    @Test
    fun `updated shelter has updated timestamp`() {
        val shelterId = createTestShelter("Original", "USA", "NY", "New York")
        val original = shelterRepository.getById(shelterId)!!

        Thread.sleep(10)

        val result = shelterRepository.update(shelterId, UpdateShelterRequest(name = "Updated"))

        assertNotNull(result)
        assertTrue(result.updatedAt > original.updatedAt)
        assertEquals(original.createdAt, result.createdAt)
    }

    private fun createTestShelter(
        name: String,
        country: String,
        state: String?,
        city: String,
        address: String = "123 Test St",
        zip: String? = null,
        phone: String? = null,
        email: String? = null,
        website: String? = null,
        fiscalId: String? = null,
        bankName: String? = null,
        accountHolderName: String? = null,
        accountNumber: String? = null,
        iban: String? = null,
        swiftBic: String? = null,
        currency: String = "USD",
        description: String? = null
    ): Int {
        val request = CreateShelterRequest(
            name = name,
            country = country,
            state = state,
            city = city,
            address = address,
            zip = zip,
            phone = phone,
            email = email,
            website = website,
            fiscalId = fiscalId,
            bankName = bankName,
            accountHolderName = accountHolderName,
            accountNumber = accountNumber,
            iban = iban,
            swiftBic = swiftBic,
            currency = currency,
            description = description
        )
        return shelterRepository.create(request).id
    }
}
