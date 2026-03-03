package com.adoptu.plugins.routes

import com.adoptu.dto.*
import com.adoptu.models.Users
import com.adoptu.repositories.PetRepository
import com.adoptu.services.PetService
import com.adoptu.services.ServiceResult
import com.adoptu.mocks.MockEmailService
import com.adoptu.mocks.MockImageStorage
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PetsRoutesIntegrationTest {

    private lateinit var petService: PetService
    private lateinit var petRepository: PetRepository
    private lateinit var mockImageStorage: MockImageStorage
    private lateinit var mockEmailService: MockEmailService

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        
        // Create mocks users
        transaction {
            try {
                Users.insert {
                    it[Users.id] = 1
                    it[Users.username] = "rescuer"
                    it[Users.displayName] = "Test Rescuer"
                    it[Users.email] = "rescuer@mocks.com"
                    it[Users.role] = "RESCUER"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
            } catch (e: Exception) { }
            
            try {
                Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "adopter"
                    it[Users.displayName] = "Test Adopter"
                    it[Users.email] = "adopter@mocks.com"
                    it[Users.role] = "ADOPTER"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
            } catch (e: Exception) { }
            
            try {
                Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "admin"
                    it[Users.displayName] = "Test Admin"
                    it[Users.email] = "admin@mocks.com"
                    it[Users.role] = "ADMIN"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
            } catch (e: Exception) { }
        }
        
        mockImageStorage = MockImageStorage()
        mockEmailService = MockEmailService()
        petRepository = PetRepository
        petService = PetService(petRepository, mockImageStorage, mockEmailService)
    }

    // ==================== GET /api/pets ====================
    
    @Test
    fun `GET all pets returns empty list when no pets`() {
        val result = petService.getAll()
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `GET all pets returns all available pets`() {
        createPetViaRepository("Buddy", "DOG")
        createPetViaRepository("Whiskers", "CAT")
        createPetViaRepository("Max", "DOG")
        
        val result = petService.getAll()
        
        assertEquals(3, result.size)
    }
    
    @Test
    fun `GET pets filters by type`() {
        createPetViaRepository("Buddy", "DOG")
        createPetViaRepository("Whiskers", "CAT")
        createPetViaRepository("Max", "DOG")
        
        val dogs = petService.getAll("DOG")
        val cats = petService.getAll("CAT")
        
        assertEquals(2, dogs.size)
        assertEquals(1, cats.size)
        assertTrue(dogs.all { it.type == "DOG" })
        assertTrue(cats.all { it.type == "CAT" })
    }
    
    @Test
    fun `GET pets only returns AVAILABLE pets`() {
        createPetViaRepository("Buddy", "DOG", status = "ADOPTED")
        createPetViaRepository("Whiskers", "CAT")
        
        val result = petService.getAll()
        
        assertEquals(1, result.size)
        assertEquals("CAT", result[0].type)
    }

    // ==================== GET /api/pets/{id} ====================
    
    @Test
    fun `GET pet by id returns null for non-existent`() {
        val result = petService.getById(999)
        assertNull(result)
    }
    
    @Test
    fun `GET pet by id returns pet when exists`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.getById(created.id)
        
        assertNotNull(result)
        assertEquals("Buddy", result.name)
        assertEquals("DOG", result.type)
    }

    // ==================== POST /api/pets ====================
    
    @Test
    fun `POST creates new pet successfully`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            breed = "Golden Retriever",
            description = "A friendly dog",
            weight = 25.5,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE,
            size = "LARGE",
            isUrgent = true
        )
        
        val result = petService.create(1, request)
        
        assertNotNull(result)
        assertEquals("Buddy", result.name)
        assertEquals("DOG", result.type)
        assertEquals("Golden Retriever", result.breed)
        assertEquals(25.5, result.weight)
        assertEquals(3, result.ageYears)
        assertEquals(6, result.ageMonths)
        assertEquals(Gender.MALE, result.sex)
        assertEquals("LARGE", result.size)
        assertTrue(result.isUrgent)
    }
    
    @Test
    fun `POST rejects negative weight`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = -5.0,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE
        )
        
        assertThrows<IllegalArgumentException> {
            petService.create(1, request)
        }
    }
    
    @Test
    fun `POST rejects invalid age months`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 15,
            sex = Gender.MALE
        )
        
        assertThrows<IllegalArgumentException> {
            petService.create(1, request)
        }
    }

    // ==================== PUT /api/pets/{id} ====================
    
    @Test
    fun `PUT updates pet successfully when owner`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.update(
            id = created.id,
            userId = 1,
            userRole = UserRole.RESCUER,
            body = UpdatePetRequest(name = "Buddy Updated", weight = 30.0)
        )
        
        assertTrue(result is ServiceResult.Success)
        val updated = (result as ServiceResult.Success).data
        assertEquals("Buddy Updated", updated.name)
        assertEquals(30.0, updated.weight)
    }
    
    @Test
    fun `PUT updates pet successfully when admin`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.update(
            id = created.id,
            userId = 999,
            userRole = UserRole.ADMIN,
            body = UpdatePetRequest(name = "Admin Updated")
        )
        
        assertTrue(result is ServiceResult.Success)
        assertEquals("Admin Updated", (result as ServiceResult.Success).data.name)
    }
    
    @Test
    fun `PUT returns NotFound for non-existent pet`() {
        val result = petService.update(
            id = 999,
            userId = 1,
            userRole = UserRole.RESCUER,
            body = UpdatePetRequest(name = "Test")
        )
        
        assertEquals(ServiceResult.NotFound, result)
    }
    
    @Test
    fun `PUT returns Forbidden when not owner or admin`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.update(
            id = created.id,
            userId = 2,
            userRole = UserRole.ADOPTER,
            body = UpdatePetRequest(name = "Hacked")
        )
        
        assertEquals(ServiceResult.Forbidden, result)
    }

    // ==================== DELETE /api/pets/{id} ====================
    
    @Test
    fun `DELETE deletes pet successfully when owner`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.delete(
            id = created.id,
            userId = 1,
            userRole = UserRole.RESCUER
        )
        
        assertEquals(ServiceResult.Success(Unit), result)
        assertNull(petService.getById(created.id))
    }
    
    @Test
    fun `DELETE deletes pet successfully when admin`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.delete(
            id = created.id,
            userId = 999,
            userRole = UserRole.ADMIN
        )
        
        assertEquals(ServiceResult.Success(Unit), result)
    }
    
    @Test
    fun `DELETE returns NotFound for non-existent pet`() {
        val result = petService.delete(
            id = 999,
            userId = 1,
            userRole = UserRole.ADMIN
        )
        
        assertEquals(ServiceResult.NotFound, result)
    }
    
    @Test
    fun `DELETE returns Forbidden when not owner or admin`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.delete(
            id = created.id,
            userId = 2,
            userRole = UserRole.ADOPTER
        )
        
        assertEquals(ServiceResult.Forbidden, result)
    }

    // ==================== POST /api/pets/{id}/adopt ====================
    
    @Test
    fun `POST adopt creates adoption request`() {
        val pet = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.createAdoptionRequest(
            petId = pet.id,
            adopterId = 2,
            message = "I would love to adopt this pet!"
        )
        
        assertNotNull(result)
        assertEquals(pet.id, result.petId)
        assertEquals(2, result.adopterId)
        assertEquals("I would love to adopt this pet!", result.message)
    }

    // ==================== Image Tests ====================
    
    @Test
    fun `POST adds image to pet`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.addImage(
            petId = created.id,
            userId = 1,
            userRole = UserRole.RESCUER,
            imageUrl = "https://s3.example.com/images/pet1.jpg",
            isPrimary = true
        )
        
        assertTrue(result is ServiceResult.Success)
        val image = (result as ServiceResult.Success).data
        assertEquals("https://s3.example.com/images/pet1.jpg", image.imageUrl)
        assertTrue(image.isPrimary)
    }
    
    @Test
    fun `POST image returns NotFound for non-existent pet`() {
        val result = petService.addImage(
            petId = 999,
            userId = 1,
            userRole = UserRole.ADMIN,
            imageUrl = "https://s3.example.com/image.jpg",
            isPrimary = false
        )
        
        assertEquals(ServiceResult.NotFound, result)
    }
    
    @Test
    fun `POST image returns Forbidden when not owner`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        val result = petService.addImage(
            petId = created.id,
            userId = 2,
            userRole = UserRole.ADOPTER,
            imageUrl = "https://s3.example.com/image.jpg",
            isPrimary = false
        )
        
        assertEquals(ServiceResult.Forbidden, result)
    }
    
    @Test
    fun `DELETE removes image from pet`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        // Add image first
        petService.addImage(
            petId = created.id,
            userId = 1,
            userRole = UserRole.RESCUER,
            imageUrl = "https://s3.example.com/image.jpg",
            isPrimary = true
        )
        
        val images = petService.getImages(created.id)
        assertEquals(1, images.size)
        
        // Remove image - it's a suspend function
        val removeResult = kotlinx.coroutines.runBlocking {
            petService.removeImage(
                petId = created.id,
                imageId = images[0].id,
                userId = 1,
                userRole = UserRole.RESCUER
            )
        }
        
        assertEquals(ServiceResult.Success(Unit), removeResult)
    }
    
    @Test
    fun `PUT sets primary image`() {
        val created = createPetViaRepository("Buddy", "DOG")
        
        // Add two images
        petService.addImage(created.id, 1, UserRole.RESCUER, "https://s3.example.com/image1.jpg", false)
        val img2 = (petService.addImage(created.id, 1, UserRole.RESCUER, "https://s3.example.com/image2.jpg", false) as ServiceResult.Success).data
        
        // Set second as primary
        val result = petService.setPrimaryImage(
            petId = created.id,
            imageId = img2.id,
            userId = 1,
            userRole = UserRole.RESCUER
        )
        
        assertEquals(ServiceResult.Success(Unit), result)
    }

    // ==================== Helper Methods ====================
    
    private fun createPetViaRepository(
        name: String,
        type: String,
        rescuerId: Int = 1,
        status: String = "AVAILABLE"
    ): PetDto {
        return PetRepository.create(
            rescuerId = rescuerId,
            name = name,
            type = type,
            description = "Test description",
            weight = 10.0,
            ageYears = 2,
            ageMonths = 0,
            sex = Gender.MALE,
            breed = "Test breed",
            status = status
        )
    }
}
