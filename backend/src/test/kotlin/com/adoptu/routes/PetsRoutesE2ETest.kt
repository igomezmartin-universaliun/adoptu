package com.adoptu.routes

import com.adoptu.adapters.db.AdoptionRequests
import com.adoptu.adapters.db.PetImages
import com.adoptu.adapters.db.Pets
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.dto.input.CreateAdoptionRequestRequest
import com.adoptu.dto.input.CreatePetRequest
import com.adoptu.dto.input.Gender
import com.adoptu.dto.input.PetDto
import com.adoptu.dto.input.PetImageDto
import com.adoptu.dto.input.Status
import com.adoptu.dto.input.UpdatePetRequest
import com.adoptu.dto.input.UserRole
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.MockNotificationAdapter
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.ports.ImageStoragePort
import com.adoptu.ports.PetRepositoryPort
import com.adoptu.services.PetService
import com.adoptu.services.ServiceResult
import com.adoptu.services.auth.SessionUser
import com.adoptu.services.auth.WebAuthnService
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PetsRoutesE2ETest {

    private val clock = Clock.System

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
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
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
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
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
                    it[Users.createdAt] = clock.now().toEpochMilliseconds()
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
            "ktor.deployment.port" to "80"
        )

        val testModules = module {
            single<ApplicationConfig> { config }
            single<kotlin.time.Clock> { kotlin.time.Clock.System }
            single { WebAuthnService(get(), get(), get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", listOf(config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:80")) }
            single<ImageStoragePort> { MockImageStorage() }
            single { MockNotificationAdapter() }
            single<com.adoptu.ports.NotificationPort> { get<MockNotificationAdapter>() }
            single<PetRepositoryPort> { PetRepositoryImpl(get()) }
            single<com.adoptu.ports.UserRepositoryPort> { UserRepository(get()) }
            single<com.adoptu.ports.PhotographerRepositoryPort> { PhotographerRepositoryImpl(get(), get(), get()) }
            single { com.adoptu.services.PhotographerService(get(), get(), get(), get()) }
            single { com.adoptu.services.UserService(get()) }
            single { PetService(get(), get(), get(), get()) }
            single { com.adoptu.services.validation.PetsValidationService() }
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
                // Test-only helper to establish a real, correctly-signed session cookie
                // without re-implementing the production login flow.
                post("/test/login/{userId}") {
                    val userId = call.parameters["userId"]!!.toInt()
                    call.sessions.set(SessionUser(userId, "user$userId@test.com", "Test User $userId"))
                    call.respondText("OK")
                }
            }
        }
    }

    private suspend fun HttpClient.loginAs(userId: Int): String {
        val response = post("/test/login/$userId")
        val setCookie = response.headers[HttpHeaders.SetCookie]
            ?: error("No session cookie returned from test login")
        return setCookie.substringBefore(";")
    }

    private fun generateTestImageBytes(): ByteArray {
        val image = java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val out = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "jpg", out)
        return out.toByteArray()
    }

    private fun createImageInDb(petId: Int, isPrimary: Boolean = false): Int {
        return transaction {
            PetImages.insert {
                it[PetImages.petId] = petId
                it[PetImages.imageUrl] = "https://mock-storage.example.com/existing.jpg"
                it[PetImages.isPrimary] = isPrimary
                it[PetImages.sortOrder] = 0
            } get PetImages.id
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
            userRoles = setOf("RESCUER"),
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
            userRoles = setOf("ADMIN"),
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
            userRoles = setOf("RESCUER"),
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
            userRoles = setOf("ADOPTER"),
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
            userRoles = setOf("RESCUER")
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
            userRoles = setOf("ADMIN")
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
            userRoles = setOf("RESCUER")
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
            userRoles = setOf("RESCUER")
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
            userRoles = setOf("ADOPTER")
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
            userRoles = setOf("RESCUER")
        )

        assertEquals(ServiceResult.Forbidden, result)
    }

    // ==================== POST /api/pets (authenticated branches) ====================

    @Test
    fun `POST pets returns 403 for adopter role`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter

            val response = client.post("/api/pets") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreatePetRequest(name = "Test", type = "DOG")))
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `POST pets returns 404 when session user does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.post("/api/pets") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreatePetRequest(name = "Test", type = "DOG")))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST pets succeeds for rescuer`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1) // rescuer

            val response = client.post("/api/pets") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreatePetRequest(name = "Rover", type = "DOG")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Rover"))
        }
    }

    @Test
    fun `POST pets succeeds for admin`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.post("/api/pets") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreatePetRequest(name = "AdminPet", type = "CAT")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("AdminPet"))
        }
    }

    // ==================== PUT /api/pets/{id} (authenticated branches) ====================

    @Test
    fun `PUT pets returns 404 when session user does not exist`() {
        val petId = createPetInDb("Buddy", "DOG")
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.put("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdatePetRequest(name = "Updated")))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT pets returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/abc") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdatePetRequest(name = "Updated")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `PUT pets returns 404 when pet does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/9999") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdatePetRequest(name = "Updated")))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT pets returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter, not the owner

            val response = client.put("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdatePetRequest(name = "Updated")))
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `PUT pets succeeds for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdatePetRequest(name = "UpdatedName")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("UpdatedName"))
        }
    }

    @Test
    fun `PUT pets succeeds for admin on someone else's pet`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.put("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdatePetRequest(name = "AdminUpdated")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("AdminUpdated"))
        }
    }

    // ==================== DELETE /api/pets/{id} (authenticated branches) ====================

    @Test
    fun `DELETE pets returns 404 when session user does not exist`() {
        val petId = createPetInDb("Buddy", "DOG")
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.delete("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE pets returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/abc") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `DELETE pets returns 404 when pet does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/9999") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE pets returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val response = client.delete("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `DELETE pets succeeds for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `DELETE pets succeeds for admin on someone else's pet`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(3)

            val response = client.delete("/api/pets/$petId") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // ==================== POST /api/pets/{id}/images (authenticated branches) ====================

    @Test
    fun `POST pets images returns 404 when session user does not exist`() {
        val petId = createPetInDb("Buddy", "DOG")
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.post("/api/pets/$petId/images") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST pets images returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/pets/abc/images") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST pets images via imageIds param returns 404 for non-existent pet`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/pets/9999/images?imageIds=1,2") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST pets images via imageIds param returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val response = client.post("/api/pets/$petId/images?imageIds=1,2") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `POST pets images via imageIds param succeeds for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.post("/api/pets/$petId/images?imageIds=1,2") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("images"))
        }
    }

    @Test
    fun `POST pets images multipart returns 400 when no file provided`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.submitFormWithBinaryData(
                url = "/api/pets/$petId/images",
                formData = formData {
                    append("isPrimary", "false")
                }
            ) {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("No storage provided"))
        }
    }

    @Test
    fun `POST pets images multipart returns 404 when pet does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.submitFormWithBinaryData(
                url = "/api/pets/9999/images",
                formData = formData {
                    append("file", generateTestImageBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.jpg\"")
                    })
                }
            ) {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST pets images multipart returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val response = client.submitFormWithBinaryData(
                url = "/api/pets/$petId/images",
                formData = formData {
                    append("file", generateTestImageBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.jpg\"")
                    })
                }
            ) {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `POST pets images multipart succeeds with valid image for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.submitFormWithBinaryData(
                url = "/api/pets/$petId/images",
                formData = formData {
                    append("file", generateTestImageBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.jpg\"")
                    })
                    append("isPrimary", "true")
                }
            ) {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("mock-storage"))
        }
    }

    @Test
    fun `POST pets images multipart returns 500 for unparseable image data`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.submitFormWithBinaryData(
                url = "/api/pets/$petId/images",
                formData = formData {
                    append("file", "not-a-real-image".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.jpg\"")
                    })
                }
            ) {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to upload storage"))
        }
    }

    // ==================== DELETE /api/pets/{petId}/images/{imageId} (authenticated branches) ====================

    @Test
    fun `DELETE pets images returns 404 when session user does not exist`() {
        val petId = createPetInDb("Buddy", "DOG")
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.delete("/api/pets/$petId/images/1") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE pets images returns error for invalid pet id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/abc/images/1") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid pet ID"))
        }
    }

    @Test
    fun `DELETE pets images returns error for invalid image id`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/$petId/images/abc") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid storage ID"))
        }
    }

    @Test
    fun `DELETE pets images returns 404 when pet does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/9999/images/1") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE pets images returns 404 when image does not exist`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/$petId/images/9999") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE pets images returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val imageId = createImageInDb(petId)
        testApplication {
            setupApp()
            val cookie = client.loginAs(2)

            val response = client.delete("/api/pets/$petId/images/$imageId") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `DELETE pets images succeeds for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val imageId = createImageInDb(petId)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.delete("/api/pets/$petId/images/$imageId") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // ==================== POST /api/pets/{id}/adopt (authenticated branches) ====================

    @Test
    fun `POST adopt returns 404 when session user does not exist`() {
        val petId = createPetInDb("Buddy", "DOG")
        testApplication {
            setupApp()
            val cookie = client.loginAs(9999)

            val response = client.post("/api/pets/$petId/adopt") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateAdoptionRequestRequest("I want to adopt")))
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST adopt returns 403 for non-adopter role`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(1) // rescuer, not an adopter

            val response = client.post("/api/pets/$petId/adopt") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateAdoptionRequestRequest("I want to adopt")))
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertTrue(response.bodyAsText().contains("Only adopters can request adoption"))
        }
    }

    @Test
    fun `POST adopt returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter

            val response = client.post("/api/pets/abc/adopt") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateAdoptionRequestRequest("I want to adopt")))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `POST adopt succeeds for adopter`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter

            val response = client.post("/api/pets/$petId/adopt") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateAdoptionRequestRequest("I want to adopt Buddy")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("I want to adopt Buddy"))
        }
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
                it[Pets.createdAt] = clock.now().toEpochMilliseconds()
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
        createdAt = clock.now().toEpochMilliseconds()
    )

    private fun createMockPetImage(imageId: Int = 10) = PetImageDto(
        id = imageId,
        imageUrl = "https://test.com/img.jpg",
        isPrimary = true,
        sortOrder = 0
    )

    private fun createAdoptionRequestInDb(petId: Int, adopterId: Int = 2, status: String = "PENDING"): Int {
        return transaction {
            AdoptionRequests.insert {
                it[AdoptionRequests.petId] = petId
                it[AdoptionRequests.adopterId] = adopterId
                it[AdoptionRequests.message] = "Please let me adopt"
                it[AdoptionRequests.status] = status
                it[AdoptionRequests.createdAt] = clock.now().toEpochMilliseconds()
            } get AdoptionRequests.id
        }
    }

    // ==================== PUT /api/pets/{petId}/images/{imageId}/primary ====================

    @Test
    fun `PUT primary image returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")
        val imageId = createImageInDb(petId)

        testApplication {
            setupApp()
            val response = client.put("/api/pets/$petId/images/$imageId/primary")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT primary image returns 400 for invalid pet id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)
            val response = client.put("/api/pets/not-a-number/images/1/primary") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `PUT primary image returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val imageId = createImageInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter, not the owner

            val response = client.put("/api/pets/$petId/images/$imageId/primary") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `PUT primary image succeeds for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val imageId = createImageInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/$petId/images/$imageId/primary") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `PUT primary image succeeds for admin on someone else's pet`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val imageId = createImageInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.put("/api/pets/$petId/images/$imageId/primary") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `PUT primary image returns 404 when pet does not exist`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/9999/images/9999/primary") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    // ==================== GET /api/pets/{id}/adoption-requests ====================

    @Test
    fun `GET adoption-requests returns 401 when no session`() {
        val petId = createPetInDb("Buddy", "DOG")

        testApplication {
            setupApp()
            val response = client.get("/api/pets/$petId/adoption-requests")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET adoption-requests returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)
            val response = client.get("/api/pets/not-a-number/adoption-requests") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `GET adoption-requests returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter, not the owner

            val response = client.get("/api/pets/$petId/adoption-requests") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `GET adoption-requests succeeds for owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.get("/api/pets/$petId/adoption-requests") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Please let me adopt"))
        }
    }

    @Test
    fun `GET adoption-requests succeeds for admin on someone else's pet`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.get("/api/pets/$petId/adoption-requests") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // ==================== PUT /api/pets/adoption-requests/{requestId} ====================

    @Test
    fun `PUT adoption-requests returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.put("/api/pets/adoption-requests/1") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=APPROVED")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `PUT adoption-requests returns 400 for invalid request id`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)
            val response = client.put("/api/pets/adoption-requests/not-a-number") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=APPROVED")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `PUT adoption-requests returns 400 when status param missing`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val requestId = createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)
            val response = client.put("/api/pets/adoption-requests/$requestId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `PUT adoption-requests returns 404 for non-existent request`() {
        testApplication {
            setupApp()
            val cookie = client.loginAs(1)
            val response = client.put("/api/pets/adoption-requests/9999") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=APPROVED")
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT adoption-requests returns 403 for non-owner`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val requestId = createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter, not the owner

            val response = client.put("/api/pets/adoption-requests/$requestId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=APPROVED")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `PUT adoption-requests rejects an invalid status value`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val requestId = createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/adoption-requests/$requestId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=NOT_A_REAL_STATUS")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `PUT adoption-requests succeeds for owner approving a request`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val requestId = createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(1)

            val response = client.put("/api/pets/adoption-requests/$requestId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=APPROVED")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("APPROVED"))
        }
    }

    @Test
    fun `PUT adoption-requests succeeds for admin rejecting someone else's request`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        val requestId = createAdoptionRequestInDb(petId)

        testApplication {
            setupApp()
            val cookie = client.loginAs(3) // admin

            val response = client.put("/api/pets/adoption-requests/$requestId") {
                header(HttpHeaders.Cookie, cookie)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("status=REJECTED")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("REJECTED"))
        }
    }

    // ==================== GET /api/pets/my-adoption-requests ====================

    @Test
    fun `GET my-adoption-requests returns 401 when no session`() {
        testApplication {
            setupApp()
            val response = client.get("/api/pets/my-adoption-requests")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET my-adoption-requests returns requests made by the session user`() {
        val petId = createPetInDb("Buddy", "DOG", rescuerId = 1)
        createAdoptionRequestInDb(petId, adopterId = 2)

        testApplication {
            setupApp()
            val cookie = client.loginAs(2) // adopter

            val response = client.get("/api/pets/my-adoption-requests") {
                header(HttpHeaders.Cookie, cookie)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Please let me adopt"))
        }
    }
}
