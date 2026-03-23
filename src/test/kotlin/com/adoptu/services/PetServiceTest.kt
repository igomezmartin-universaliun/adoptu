package com.adoptu.services

import com.adoptu.dto.CreatePetRequest
import com.adoptu.dto.Gender
import com.adoptu.dto.UpdatePetRequest
import com.adoptu.dto.UserRole
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.mocks.MockNotificationAdapter
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

class PetServiceTest {

    private lateinit var petService: PetService
    private lateinit var petRepository: PetRepositoryImpl
    private lateinit var mockImageStorage: MockImageStorage
    private lateinit var mockNotificationAdapter: MockNotificationAdapter

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        
        // Always create mocks users - use INSERT OR IGNORE to avoid duplicates
        transaction {
            try {
                Users.insert {
                    it[Users.id] = 1
                    it[Users.username] = "rescuer@mocks.com"
                    it[Users.displayName] = "Test Rescuer"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 1
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) {
                // User already exists, ignore
            }
            try {
                Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "adopter@mocks.com"
                    it[Users.displayName] = "Test Adopter"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 2
                    it[UserActiveRoles.role] = "ADOPTER"
                }
            } catch (e: Exception) {
                // User already exists, ignore
            }
            try {
                Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "other@mocks.com"
                    it[Users.displayName] = "Other User"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 3
                    it[UserActiveRoles.role] = "ADOPTER"
                }
            } catch (e: Exception) {
                // User already exists, ignore
            }
        }
        
        // Clear pets
        transaction {
            exec("DELETE FROM adoption_requests")
            exec("DELETE FROM pet_images")
            exec("DELETE FROM pets")
        }
        
        mockImageStorage = MockImageStorage()
        mockNotificationAdapter = MockNotificationAdapter()
        val userRepository = UserRepository()
        petRepository = PetRepositoryImpl()
        val photographerRepository = PhotographerRepositoryImpl(petRepository, userRepository)
        val photographerService = PhotographerService(photographerRepository, null, userRepository)
        val userService = UserService(userRepository, photographerService)
        petService = PetService(petRepository, mockImageStorage, mockNotificationAdapter, userService)
    }

    @Test
    fun `getAll returns empty list when no pets exist`() {
        val result = petService.getAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAll returns pets filtered by type`() {
        createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        createTestPet(rescuerId = 1, name = "Whiskers", type = "CAT")
        createTestPet(rescuerId = 1, name = "Max", type = "DOG")

        val dogs = petService.getAll("DOG")
        assertEquals(2, dogs.size)
        assertTrue(dogs.all { it.type == "DOG" })
    }

    @Test
    fun `getById returns null for non-existent pet`() {
        val result = petService.getById(999)
        assertNull(result)
    }

    @Test
    fun `getById returns pet by id`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.getById(created.id)
        assertNotNull(result)
        assertEquals("Buddy", result.name)
    }

    @Test
    fun `create throws exception for negative weight`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = -5.0,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE
        )

        val exception = assertThrows<IllegalArgumentException> {
            petService.create(1, request)
        }
        assertEquals("Weight must be zero or positive", exception.message)
    }

    @Test
    fun `create throws exception for invalid age months`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 15,
            sex = Gender.MALE
        )

        val exception = assertThrows<IllegalArgumentException> {
            petService.create(1, request)
        }
        assertEquals("Age (months) must be less than 12", exception.message)
    }

    @Test
    fun `update returns NotFound for non-existent pet`() {
        val result = petService.update(999, 1, UserRole.RESCUER, UpdatePetRequest(name = "Test"))
        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `update returns Forbidden when user is not owner`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.update(created.id, 2, UserRole.RESCUER, UpdatePetRequest(name = "Hacked"))

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `delete returns NotFound for non-existent pet`() {
        val result = petService.delete(999, 1, UserRole.ADMIN)
        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `delete returns Forbidden when user is not owner`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.delete(created.id, 2, UserRole.RESCUER)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `addImage returns NotFound for non-existent pet`() {
        val result = petService.addImage(999, 1, UserRole.ADMIN, "https://example.com/image.jpg", false)
        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `addImage returns Forbidden when user is not owner`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.addImage(created.id, 2, UserRole.RESCUER, "https://example.com/image.jpg", false)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `addImage adds image successfully`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image.jpg", true)

        assertTrue(result is ServiceResult.Success)
        assertTrue((result as ServiceResult.Success).data.isPrimary)
    }

    @Test
    fun `addImage allows admin to add image`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.addImage(created.id, 999, UserRole.ADMIN, "https://example.com/admin-image.jpg", false)

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `getAll returns only promoted pets when showPromotedOnly is true`() {
        createTestPet(rescuerId = 1, name = "Buddy", type = "DOG", isPromoted = true)
        createTestPet(rescuerId = 1, name = "Max", type = "DOG", isPromoted = false)
        createTestPet(rescuerId = 1, name = "Rocky", type = "DOG", isPromoted = true)

        val result = petService.getAll("DOG", showPromotedOnly = true)

        assertEquals(2, result.size)
        assertTrue(result.all { it.isPromoted })
    }

    @Test
    fun `getAll returns all available pets without filters`() {
        createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        createTestPet(rescuerId = 1, name = "Whiskers", type = "CAT")

        val result = petService.getAll()

        assertTrue(result.size >= 2)
    }

    @Test
    fun `create creates pet successfully`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE,
            breed = "Golden Retriever"
        )

        val result = petService.create(1, request)

        assertEquals("Buddy", result.name)
        assertEquals("DOG", result.type)
        assertEquals(25.0, result.weight)
        assertEquals(3, result.ageYears)
        assertEquals(6, result.ageMonths)
        assertEquals(Gender.MALE, result.sex)
        assertEquals("Golden Retriever", result.breed)
    }

    @Test
    fun `create throws exception for negative ageYears`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            weight = 25.0,
            ageYears = -1,
            ageMonths = 0,
            sex = Gender.MALE
        )

        val exception = assertThrows<IllegalArgumentException> {
            petService.create(1, request)
        }
        assertEquals("Age (years) must be zero or positive", exception.message)
    }

    @Test
    fun `create throws exception for negative adoptionFee`() {
        val request = CreatePetRequest(
            name = "Buddy",
            type = "DOG",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 0,
            sex = Gender.MALE,
            adoptionFee = -50.0
        )

        val exception = assertThrows<IllegalArgumentException> {
            petService.create(1, request)
        }
        assertEquals("Adoption fee must be zero or positive", exception.message)
    }

    @Test
    fun `update updates pet successfully`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.update(created.id, 1, UserRole.RESCUER, UpdatePetRequest(name = "Max", weight = 30.0))

        assertTrue(result is ServiceResult.Success)
        val pet = (result as ServiceResult.Success).data
        assertEquals("Max", pet.name)
        assertEquals(30.0, pet.weight)
    }

    @Test
    fun `update allows admin to update any pet`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.update(created.id, 999, UserRole.ADMIN, UpdatePetRequest(name = "Admin Buddy"))

        assertTrue(result is ServiceResult.Success)
        assertEquals("Admin Buddy", (result as ServiceResult.Success).data.name)
    }

    @Test
    fun `delete deletes pet successfully`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val petId = created.id

        val result = petService.delete(petId, 1, UserRole.RESCUER)

        assertTrue(result is ServiceResult.Success)
        assertNull(petService.getById(petId))
    }

    @Test
    fun `delete allows admin to delete any pet`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val petId = created.id

        val result = petService.delete(petId, 999, UserRole.ADMIN)

        assertTrue(result is ServiceResult.Success)
        assertNull(petService.getById(petId))
    }

    @Test
    fun `getImages returns empty list when no images`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.getImages(created.id)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getImages returns pet images`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image1.jpg", true)
        petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image2.jpg", false)

        val result = petService.getImages(created.id)

        assertEquals(2, result.size)
        assertTrue(result.any { it.imageUrl.contains("image1.jpg") && it.isPrimary })
    }

    @Test
    fun `setPrimaryImage sets primary image successfully`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val img1Result = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image1.jpg", true)
        val img2Result = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image2.jpg", false)
        val img1 = (img1Result as ServiceResult.Success).data
        val img2 = (img2Result as ServiceResult.Success).data

        val result = petService.setPrimaryImage(created.id, img2.id, 1, UserRole.RESCUER)

        assertTrue(result is ServiceResult.Success)
        val images = petService.getImages(created.id)
        val newPrimary = images.find { it.id == img2.id }
        val oldPrimary = images.find { it.id == img1.id }
        assertTrue(newPrimary?.isPrimary == true)
        assertTrue(oldPrimary?.isPrimary == false)
    }

    @Test
    fun `setPrimaryImage returns NotFound for non-existent image`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.setPrimaryImage(created.id, 999, 1, UserRole.RESCUER)

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `setPrimaryImage returns Forbidden when user is not owner`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val imgResult = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image.jpg", false)
        val img = (imgResult as ServiceResult.Success).data

        val result = petService.setPrimaryImage(created.id, img.id, 2, UserRole.RESCUER)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `setPrimaryImage allows admin to set primary image`() {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image1.jpg", true)
        val img2Result = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image2.jpg", false)
        val img2 = (img2Result as ServiceResult.Success).data

        val result = petService.setPrimaryImage(created.id, img2.id, 999, UserRole.ADMIN)

        assertTrue(result is ServiceResult.Success)
    }

    @Test
    fun `removeImage removes image successfully`() = kotlinx.coroutines.runBlocking {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val imgResult = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image.jpg", false)
        val img = (imgResult as ServiceResult.Success).data

        val result = petService.removeImage(created.id, img.id, 1, UserRole.RESCUER)

        assertTrue(result is ServiceResult.Success)
        assertTrue(petService.getImages(created.id).isEmpty())
    }

    @Test
    fun `removeImage returns NotFound for non-existent image`() = kotlinx.coroutines.runBlocking {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.removeImage(created.id, 999, 1, UserRole.RESCUER)

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `removeImage returns Forbidden when user is not owner`() = kotlinx.coroutines.runBlocking {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val imgResult = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image.jpg", false)
        val img = (imgResult as ServiceResult.Success).data

        val result = petService.removeImage(created.id, img.id, 2, UserRole.RESCUER)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `updatePetImages updates images successfully`() = kotlinx.coroutines.runBlocking {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val img1Result = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image1.jpg", false)
        val img2Result = petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image2.jpg", false)
        val img2 = (img2Result as ServiceResult.Success).data

        val result = petService.updatePetImages(created.id, 1, UserRole.RESCUER, listOf(img2.id))

        assertTrue(result is ServiceResult.Success)
        val images = petService.getImages(created.id)
        assertEquals(1, images.size)
        assertEquals(img2.id, images[0].id)
    }

    @Test
    fun `updatePetImages removes deleted images from storage`() = kotlinx.coroutines.runBlocking {
        val created = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image1.jpg", false)
        petService.addImage(created.id, 1, UserRole.RESCUER, "https://example.com/image2.jpg", false)

        petService.updatePetImages(created.id, 1, UserRole.RESCUER, emptyList())

        assertEquals(0, mockImageStorage.getStoredImages().size)
    }

    @Test
    fun `createAdoptionRequest creates adoption request successfully`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.createAdoptionRequest(pet.id, 2, "I would love to adopt this pet!")

        assertEquals(pet.id, result.petId)
        assertEquals(2, result.adopterId)
        assertEquals("I would love to adopt this pet!", result.message)
        assertEquals("PENDING", result.status)
    }

    @Test
    fun `getAdoptionRequestsForPet returns requests for owner`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        petService.createAdoptionRequest(pet.id, 2, "Request 1")
        petService.createAdoptionRequest(pet.id, 3, "Request 2")

        val result = petService.getAdoptionRequestsForPet(pet.id, 1, UserRole.RESCUER)

        assertTrue(result is ServiceResult.Success)
        assertEquals(2, (result as ServiceResult.Success).data.size)
    }

    @Test
    fun `getAdoptionRequestsForPet returns Forbidden when user is not owner`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")

        val result = petService.getAdoptionRequestsForPet(pet.id, 2, UserRole.RESCUER)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `getAdoptionRequestsForPet allows admin to view requests`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        petService.createAdoptionRequest(pet.id, 2, "Request 1")

        val result = petService.getAdoptionRequestsForPet(pet.id, 999, UserRole.ADMIN)

        assertTrue(result is ServiceResult.Success)
        assertEquals(1, (result as ServiceResult.Success).data.size)
    }

    @Test
    fun `getMyAdoptionRequests returns user's adoption requests`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        petService.createAdoptionRequest(pet.id, 2, "My request 1")
        petService.createAdoptionRequest(pet.id, 2, "My request 2")
        petService.createAdoptionRequest(pet.id, 3, "Other request")

        val result = petService.getMyAdoptionRequests(2)

        assertEquals(2, result.size)
        assertTrue(result.all { it.adopterId == 2 })
    }

    @Test
    fun `updateAdoptionRequest updates request to APPROVED`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val request = petService.createAdoptionRequest(pet.id, 2, "I want to adopt")

        val result = petService.updateAdoptionRequest(request.id, "APPROVED", 1, UserRole.RESCUER)

        assertTrue(result is ServiceResult.Success)
        assertEquals("APPROVED", (result as ServiceResult.Success).data.status)
    }

    @Test
    fun `updateAdoptionRequest updates request to REJECTED`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val request = petService.createAdoptionRequest(pet.id, 2, "I want to adopt")

        val result = petService.updateAdoptionRequest(request.id, "REJECTED", 1, UserRole.RESCUER)

        assertTrue(result is ServiceResult.Success)
        assertEquals("REJECTED", (result as ServiceResult.Success).data.status)
    }

    @Test
    fun `updateAdoptionRequest returns NotFound for non-existent request`() {
        val result = petService.updateAdoptionRequest(999, "APPROVED", 1, UserRole.RESCUER)

        assertEquals(ServiceResult.NotFound, result)
    }

    @Test
    fun `updateAdoptionRequest returns Forbidden for invalid status`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val request = petService.createAdoptionRequest(pet.id, 2, "I want to adopt")

        val result = petService.updateAdoptionRequest(request.id, "INVALID_STATUS", 1, UserRole.RESCUER)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `updateAdoptionRequest returns Forbidden when user is not owner`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val request = petService.createAdoptionRequest(pet.id, 2, "I want to adopt")

        val result = petService.updateAdoptionRequest(request.id, "APPROVED", 3, UserRole.RESCUER)

        assertEquals(ServiceResult.Forbidden, result)
    }

    @Test
    fun `updateAdoptionRequest allows admin to update request`() {
        val pet = createTestPet(rescuerId = 1, name = "Buddy", type = "DOG")
        val request = petService.createAdoptionRequest(pet.id, 2, "I want to adopt")

        val result = petService.updateAdoptionRequest(request.id, "APPROVED", 999, UserRole.ADMIN)

        assertTrue(result is ServiceResult.Success)
        assertEquals("APPROVED", (result as ServiceResult.Success).data.status)
    }

    private fun createTestPet(rescuerId: Int, name: String, type: String, isPromoted: Boolean = false) =
        petRepository.create(
            rescuerId = rescuerId,
            name = name,
            type = type,
            description = "Test description",
            weight = 10.0,
            ageYears = 2,
            ageMonths = 0,
            sex = Gender.MALE,
            breed = "Test breed",
            isPromoted = isPromoted
        )
}
