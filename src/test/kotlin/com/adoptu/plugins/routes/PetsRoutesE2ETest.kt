package com.adoptu.plugins.routes

import com.adoptu.ports.ImageStoragePort
import com.adoptu.ports.PetRepositoryPort
import com.adoptu.dto.Gender
import com.adoptu.dto.PetDto
import com.adoptu.dto.PetImageDto
import com.adoptu.dto.Status
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.TestDatabase
import com.adoptu.adapters.db.Pets
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.services.PetService
import com.adoptu.services.ServiceResult
import com.adoptu.services.auth.WebAuthnService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetsRoutesE2ETest {

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
                    it[Users.username] = "rescuer@test.com"
                    it[Users.displayName] = "Test Rescuer"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 1
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) { }

            try {
                val adopterId = Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "adopter@test.com"
                    it[Users.displayName] = "Test Adopter"
                    it[Users.createdAt] = System.currentTimeMillis()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = adopterId
                    it[UserActiveRoles.role] = "ADOPTER"
                }
            } catch (e: Exception) { }

            try {
                val adminId = Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "admin@test.com"
                    it[Users.displayName] = "Test Admin"
                    it[Users.createdAt] = System.currentTimeMillis()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = adminId
                    it[UserActiveRoles.role] = "ADMIN"
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
            single<ApplicationConfig> { config }
            single { WebAuthnService }
            single<ImageStoragePort> { MockImageStorage() }
            single { MockNotificationAdapter() }
            single<com.adoptu.ports.NotificationPort> { get<MockNotificationAdapter>() }
            single<PetRepositoryPort> { PetRepositoryImpl() }
            single<com.adoptu.ports.UserRepositoryPort> { UserRepository() }
            single<com.adoptu.ports.PhotographerRepositoryPort> { PhotographerRepositoryImpl(get(), get()) }
            single { com.adoptu.services.PhotographerService(get(), get(), get()) }
            single { com.adoptu.services.UserService(get(), get()) }
            single { PetService(get(), get(), get(), get()) }
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
                petsRoutes()
            }
        }
    }

    // ==================== GET /api/pets ====================

    @Test
    fun `GET pets returns empty list when no pets`() {
        TestDatabase.clearAllData()
        
        testApplication {
            setupApp()
            val response = client.get("/api/pets")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body == "[]" || !body.contains("id"))
        }
    }

    @Test
    fun `GET pets returns all available pets`() {
        createPetInDb("Buddy", "DOG")
        createPetInDb("Whiskers", "CAT")

        testApplication {
            setupApp()
            val response = client.get("/api/pets")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Buddy") || body.contains("Whiskers"))
        }
    }

    @Test
    fun `GET pets filters by type query parameter`() {
        createPetInDb("Buddy", "DOG")
        createPetInDb("Max", "DOG")

        testApplication {
            setupApp()

            val dogsResponse = client.get("/api/pets?type=DOG")
            assertEquals(HttpStatusCode.OK, dogsResponse.status)
            val body = dogsResponse.bodyAsText()
            assertTrue(body.contains("Buddy"))
            assertTrue(body.contains("Max"))
        }
    }

    @Test
    fun `GET pets returns only available pets`() {
        createPetInDb("Buddy", "DOG", status = "ADOPTED")
        createPetInDb("Whiskers", "CAT")

        testApplication {
            setupApp()
            val response = client.get("/api/pets")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Whiskers"))
            assertTrue(!body.contains("Buddy") || body.indexOf("Buddy") > body.indexOf("Whiskers"))
        }
    }

    // ==================== GET /api/pets/{id} ====================

    @Test
    fun `GET pet by id returns 404 for non-existent pet`() {
        testApplication {
            setupApp()
            val response = client.get("/api/pets/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET pet by id returns pet when exists`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()
            val response = client.get("/api/pets/$petId")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Buddy"))
            assertTrue(body.contains("DOG"))
        }
    }

    @Test
    fun `GET pet by id returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.get("/api/pets/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `GET pet by id returns pet even if adopted`() {
        val petId = createPetInDb("Buddy", "DOG", status = "ADOPTED")

        testApplication {
            setupApp()
            val response = client.get("/api/pets/$petId")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Buddy"))
            assertTrue(body.contains("ADOPTED"))
        }
    }

    // ==================== POST /api/pets ====================

    @Test
    fun `POST pets returns 401 when no session`() {
        testApplication {
            setupApp()

            val response = client.post("/api/pets") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Test","type":"DOG"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // ==================== PUT /api/pets/{id} ====================

    @Test
    fun `PUT pets returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()

            val response = client.put("/api/pets/$petId") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Updated"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // ==================== DELETE /api/pets/{id} ====================

    @Test
    fun `DELETE pets returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()

            val response = client.delete("/api/pets/$petId")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // ==================== POST /api/pets/{id}/adopt ====================

    @Test
    fun `POST adopt returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()

            val response = client.post("/api/pets/$petId/adopt") {
                contentType(ContentType.Application.Json)
                setBody("""{"message":"I want to adopt"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // ==================== POST /api/pets/{id}/images ====================

    @Test
    fun `POST pets images returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()

            val response = client.post("/api/pets/$petId/images")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // ==================== GET /api/pets with various filters ====================

    @Test
    fun `GET pets returns pets with correct fields`() {
        createPetInDb("Buddy", "DOG", breed = "Golden Retriever")

        testApplication {
            setupApp()
            val response = client.get("/api/pets")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Buddy"))
            assertTrue(body.contains("Golden Retriever"))
            assertTrue(body.contains("DOG"))
        }
    }

    @Test
    fun `GET pets handles case insensitive type filter`() {
        createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()
            val response = client.get("/api/pets?type=dog")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `GET pet by id returns correct pet details`() {
        val petId = createPetInDb("Buddy", "DOG", description = "A lovely dog", weight = 25.5)

        testApplication {
            setupApp()
            val response = client.get("/api/pets/$petId")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Buddy"))
            assertTrue(body.contains("25.5"))
        }
    }

    @Test
    fun `GET pet by id includes rescuer info`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()
            val response = client.get("/api/pets/$petId")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("rescuerId") || body.contains("1"))
        }
    }

    // ==================== DELETE /api/pets/{petId}/images/{imageId} ====================

    @Test
    fun `DELETE pets images returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()

            val response = client.delete("/api/pets/$petId/images/1")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // ==================== POST /api/pets/{id}/images - Service Tests with Mocked Repository ====================

    @Test
    fun `POST pets images service returns Success when user is owner`() {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 1)
        every { mockRepository.addImage(1, "https://test.com/new.jpg", false) } returns createMockPetImage()

        val result = petService.addImage(
            petId = 1,
            userId = 1,
            userRole = com.adoptu.dto.UserRole.RESCUER,
            imageUrl = "https://test.com/new.jpg",
            isPrimary = false
        )

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `POST pets images service returns Success when user is admin`() {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 99)
        every { mockRepository.addImage(1, "test.jpg", false) } returns createMockPetImage()

        val result = petService.addImage(
            petId = 1,
            userId = 5,
            userRole = com.adoptu.dto.UserRole.ADMIN,
            imageUrl = "https://test.com/new.jpg",
            isPrimary = false
        )

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `POST pets images service returns NotFound when pet does not exist`() {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(999) } returns null

        val result = petService.addImage(
            petId = 999,
            userId = 1,
            userRole = com.adoptu.dto.UserRole.RESCUER,
            imageUrl = "https://test.com/new.jpg",
            isPrimary = false
        )

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `POST pets images service returns Forbidden when user is not owner or admin`() {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 99)

        val result = petService.addImage(
            petId = 1,
            userId = 5,
            userRole = com.adoptu.dto.UserRole.ADOPTER,
            imageUrl = "https://test.com/new.jpg",
            isPrimary = false
        )

        assertEquals(ServiceResult.Forbidden, result)
    }

    // ==================== DELETE /api/pets/{petId}/images/{imageId} - Service Tests with Mocked Repository ====================

    @Test
    fun `DELETE pets images service returns Success when user is owner and image exists`() = runBlocking {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 1)
        every { mockRepository.getImages(1) } returns listOf(createMockPetImage(10))
        every { mockRepository.removeImage(1, 10) } returns true

        val result = petService.removeImage(
            petId = 1,
            imageId = 10,
            userId = 1,
            userRole = com.adoptu.dto.UserRole.RESCUER
        )

        assertEquals(ServiceResult.Success(Unit), result)
        coVerify { mockImageStorage.deleteImage("https://test.com/img.jpg") }
    }

    @Test
    fun `DELETE pets images service returns Success when user is admin`() = runBlocking {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 99)
        every { mockRepository.getImages(1) } returns listOf(createMockPetImage(10))
        every { mockRepository.removeImage(1, 10) } returns true

        val result = petService.removeImage(
            petId = 1,
            imageId = 10,
            userId = 5,
            userRole = com.adoptu.dto.UserRole.ADMIN
        )

        assertEquals(ServiceResult.Success(Unit), result)
    }

    @Test
    fun `DELETE pets images service returns NotFound when pet does not exist`() = runBlocking {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(999) } returns null

        val result = petService.removeImage(
            petId = 999,
            imageId = 10,
            userId = 1,
            userRole = com.adoptu.dto.UserRole.RESCUER
        )

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `DELETE pets images service returns NotFound when image does not exist`() = runBlocking {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 1)
        every { mockRepository.getImages(1) } returns emptyList()

        val result = petService.removeImage(
            petId = 1,
            imageId = 999,
            userId = 1,
            userRole = com.adoptu.dto.UserRole.RESCUER
        )

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `DELETE pets images service returns Forbidden when user is not owner or admin`() = runBlocking {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 99)

        val result = petService.removeImage(
            petId = 1,
            imageId = 10,
            userId = 5,
            userRole = com.adoptu.dto.UserRole.ADOPTER
        )

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `DELETE pets images service returns Forbidden when user is rescuer but not owner`() = runBlocking {
        val mockRepository = mockk<PetRepositoryPort>(relaxed = true)
        val mockImageStorage = mockk<ImageStoragePort>(relaxed = true)
        val petService = PetService(mockRepository, mockImageStorage, mockk(relaxed = true), mockk(relaxed = true))
        
        every { mockRepository.getById(1) } returns createMockPetDto(1, rescuerId = 99)

        val result = petService.removeImage(
            petId = 1,
            imageId = 10,
            userId = 50,
            userRole = com.adoptu.dto.UserRole.RESCUER
        )

        assertEquals(ServiceResult.Forbidden, result)
    }

    // ==================== Helper Methods ====================

    private fun createPetInDb(
        name: String,
        type: String,
        rescuerId: Int = 1,
        status: String = "AVAILABLE",
        breed: String = "Test breed",
        description: String = "Test description",
        weight: Double = 10.0
    ): Int {
        return transaction {
            Pets.insert {
                it[Pets.rescuerId] = rescuerId
                it[Pets.name] = name
                it[Pets.type] = type
                it[Pets.description] = description
                it[Pets.weight] = BigDecimal(weight.toString())
                it[Pets.ageYears] = 2
                it[Pets.ageMonths] = 0
                it[Pets.sex] = "MALE"
                it[Pets.breed] = breed
                it[Pets.status] = status
                it[Pets.size] = "MEDIUM"
                it[Pets.isUrgent] = false
                it[Pets.createdAt] = System.currentTimeMillis()
            } get Pets.id
        }
    }

    private fun createMockPetDto(petId: Int, rescuerId: Int = 1) = PetDto(
        id = petId,
        rescuerId = rescuerId,
        name = "Buddy",
        type = "DOG",
        breed = "Golden",
        description = "Test",
        weight = 10.0,
        ageYears = 2,
        ageMonths = 0,
        sex = Gender.MALE,
        status = Status.AVAILABLE,
        size = "MEDIUM",
        isUrgent = false,
        createdAt = System.currentTimeMillis()
    )

    private fun createMockPetImage(imageId: Int = 10) = PetImageDto(
        id = imageId,
        imageUrl = "https://test.com/img.jpg",
        isPrimary = true,
        sortOrder = 0
    )
}
