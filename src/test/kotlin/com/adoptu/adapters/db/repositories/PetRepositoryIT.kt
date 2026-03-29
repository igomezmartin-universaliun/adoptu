package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.adapters.db.Pets
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.dto.input.Currency
import com.adoptu.dto.input.Gender
import com.adoptu.dto.input.UpdatePetRequest
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
class PetRepositoryIT {

    private var postgresContainer: PostgreSQLContainer<*>? = null
    private lateinit var petRepository: PetRepositoryImpl
    private lateinit var userRepository: UserRepository
    private var rescuerId: Int = 0
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
        val dbName = "pet_repo_test_${++dbCounter}"
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
        rescuerId = createTestUserWithRole("rescuer@example.com", "Test Rescuer", UserRole.RESCUER)
    }

    @Test
    fun `getAll returns empty list when no pets`() {
        val result = petRepository.getAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll returns available pets`() {
        createTestPet(rescuerId, "Buddy", "DOG")

        val result = petRepository.getAll()

        assertEquals(1, result.size)
        assertEquals("Buddy", result.first().name)
    }

    @Test
    fun `getAll filters by type`() {
        createTestPet(rescuerId, "Buddy", "DOG")
        createTestPet(rescuerId, "Whiskers", "CAT")

        val result = petRepository.getAll("DOG")

        assertEquals(1, result.size)
        assertEquals("Buddy", result.first().name)
    }

    @Test
    fun `getAll returns only promoted pets when flag is set`() {
        createTestPet(rescuerId, "Buddy", "DOG", isPromoted = true)
        createTestPet(rescuerId, "Max", "DOG", isPromoted = false)

        val result = petRepository.getAll(showPromotedOnly = true)

        assertEquals(1, result.size)
        assertEquals("Buddy", result.first().name)
        assertTrue(result.first().isPromoted)
    }

    @Test
    fun `getById returns null for non-existent pet`() {
        val result = petRepository.getById(999)
        assertNull(result)
    }

    @Test
    fun `getById returns pet by id`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")

        val result = petRepository.getById(petId)

        assertNotNull(result)
        assertEquals("Buddy", result!!.name)
        assertEquals("DOG", result.type)
    }

    @Test
    fun `create creates a new pet`() {
        val result = petRepository.create(
            rescuerId = rescuerId,
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE,
            breed = "Golden Retriever",
            color = "Golden",
            size = "MEDIUM",
            temperament = "Friendly",
            isSterilized = true,
            isMicrochipped = true,
            microchipId = "123456789",
            vaccinations = "Up to date",
            isGoodWithKids = true,
            isGoodWithDogs = true,
            isGoodWithCats = false,
            isHouseTrained = true,
            energyLevel = "HIGH",
            rescueDate = clock.now().toEpochMilliseconds(),
            rescueLocation = "Shelter",
            specialNeeds = null,
            adoptionFee = 200.0,
            currency = Currency.USD,
            isUrgent = false,
            isPromoted = false,
            status = "AVAILABLE"
        )

        assertNotNull(result)
        assertEquals("Buddy", result.name)
        assertEquals("DOG", result.type)
        assertEquals(25.0, result.weight)
        assertEquals(3, result.ageYears)
        assertEquals(6, result.ageMonths)
        assertEquals(Gender.MALE, result.sex)
        assertEquals("Golden Retriever", result.breed)
    }

    @Test
    fun `update updates pet successfully`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")

        val result = petRepository.update(petId, UpdatePetRequest(name = "Max", weight = 30.0))

        assertNotNull(result)
        assertEquals("Max", result!!.name)
        assertEquals(30.0, result.weight)
    }

    @Test
    fun `update returns null for non-existent pet`() {
        val result = petRepository.update(999, UpdatePetRequest(name = "Test"))
        assertNull(result)
    }

    @Test
    fun `delete removes pet`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")

        petRepository.delete(petId)

        val result = petRepository.getById(petId)
        assertNull(result)
    }

    @Test
    fun `addImage adds image to pet`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")

        val result = petRepository.addImage(petId, "https://example.com/image.jpg", true, 0)

        assertNotNull(result)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertTrue(result.isPrimary)
    }

    @Test
    fun `addImage sets new image as primary and unsets previous`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        petRepository.addImage(petId, "https://example.com/image1.jpg", true, 0)

        val result = petRepository.addImage(petId, "https://example.com/image2.jpg", true, 1)

        assertTrue(result.isPrimary)
        val images = petRepository.getImages(petId)
        val image1 = images.find { it.imageUrl.contains("image1") }
        assertTrue(!image1!!.isPrimary)
    }

    @Test
    fun `removeImage removes image`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val image = petRepository.addImage(petId, "https://example.com/image.jpg", false, 0)

        val result = petRepository.removeImage(petId, image.id)

        assertTrue(result)
        assertTrue(petRepository.getImages(petId).isEmpty())
    }

    @Test
    fun `removeImage returns false for non-existent image`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")

        val result = petRepository.removeImage(petId, 999)

        assertTrue(!result)
    }

    @Test
    fun `setPrimaryImage sets primary image`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val image1 = petRepository.addImage(petId, "https://example.com/image1.jpg", true, 0)
        val image2 = petRepository.addImage(petId, "https://example.com/image2.jpg", false, 1)

        val result = petRepository.setPrimaryImage(petId, image2.id)

        assertTrue(result)
        val images = petRepository.getImages(petId)
        val img1 = images.find { it.id == image1.id }
        val img2 = images.find { it.id == image2.id }
        assertTrue(!img1!!.isPrimary)
        assertTrue(img2!!.isPrimary)
    }

    @Test
    fun `getImages returns all pet images`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        petRepository.addImage(petId, "https://example.com/image1.jpg", false, 0)
        petRepository.addImage(petId, "https://example.com/image2.jpg", false, 1)

        val result = petRepository.getImages(petId)

        assertEquals(2, result.size)
    }

    @Test
    fun `createAdoptionRequest creates adoption request`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val adopterId = createTestUserWithRole("adopter@example.com", "Test Adopter", UserRole.ADOPTER)

        val result = petRepository.createAdoptionRequest(petId, adopterId, "I want to adopt!")

        assertNotNull(result)
        assertEquals(petId, result.petId)
        assertEquals(adopterId, result.adopterId)
        assertEquals("I want to adopt!", result.message)
        assertEquals("PENDING", result.status)
    }

    @Test
    fun `getAdoptionRequestsForPet returns requests for pet`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val adopterId = createTestUserWithRole("adopter@example.com", "Test Adopter", UserRole.ADOPTER)
        petRepository.createAdoptionRequest(petId, adopterId, "Request 1")

        val result = petRepository.getAdoptionRequestsForPet(petId)

        assertEquals(1, result.size)
        assertEquals(adopterId, result.first().adopterId)
    }

    @Test
    fun `getAdoptionRequestsForUser returns user's adoption requests`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val adopterId = createTestUserWithRole("adopter@example.com", "Test Adopter", UserRole.ADOPTER)
        petRepository.createAdoptionRequest(petId, adopterId, "Request 1")

        val result = petRepository.getAdoptionRequestsForUser(adopterId)

        assertEquals(1, result.size)
        assertEquals(petId, result.first().petId)
    }

    @Test
    fun `updateAdoptionRequestStatus updates status`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val adopterId = createTestUserWithRole("adopter@example.com", "Test Adopter", UserRole.ADOPTER)
        val request = petRepository.createAdoptionRequest(petId, adopterId, "Request")

        val result = petRepository.updateAdoptionRequestStatus(request.id, "APPROVED")

        assertTrue(result)
        val updatedRequest = petRepository.getAdoptionRequestById(request.id)
        assertEquals("APPROVED", updatedRequest!!.status)
    }

    @Test
    fun `getAdoptionRequestById returns request by id`() {
        val petId = createTestPet(rescuerId, "Buddy", "DOG")
        val adopterId = createTestUserWithRole("adopter@example.com", "Test Adopter", UserRole.ADOPTER)
        val request = petRepository.createAdoptionRequest(petId, adopterId, "Request")

        val result = petRepository.getAdoptionRequestById(request.id)

        assertNotNull(result)
        assertEquals(petId, result!!.petId)
        assertEquals(adopterId, result.adopterId)
    }

    @Test
    fun `getAdoptionRequestById returns null for non-existent request`() {
        val result = petRepository.getAdoptionRequestById(999)
        assertNull(result)
    }

    private fun createTestPet(rescuerId: Int, name: String, type: String, isPromoted: Boolean = false): Int {
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
                it[Pets.isPromoted] = isPromoted
                it[Pets.adoptionFee] = BigDecimal("100.0")
                it[Pets.currency] = "USD"
                it[Pets.createdAt] = clock.now().toEpochMilliseconds()
            } get Pets.id
        }!!
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
}
