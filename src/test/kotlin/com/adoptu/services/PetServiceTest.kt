package com.adoptu.services

import com.adoptu.dto.CreatePetRequest
import com.adoptu.dto.Gender
import com.adoptu.dto.UpdatePetRequest
import com.adoptu.dto.UserRole
import com.adoptu.models.Users
import com.adoptu.models.UserActiveRoles
import com.adoptu.repositories.PetRepository
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
    private lateinit var mockImageStorage: MockImageStorage
    private lateinit var mockNotificationAdapter: MockNotificationAdapter

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        
        // Always create mocks user - use INSERT OR IGNORE to avoid duplicates
        transaction {
            try {
                Users.insert {
                    it[Users.id] = 1
                    it[Users.username] = "mocks@mocks.com"
                    it[Users.displayName] = "Test User"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 1
                    it[UserActiveRoles.role] = "RESCUER"
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
        petService = PetService(PetRepository, mockImageStorage, mockNotificationAdapter)
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

    private fun createTestPet(rescuerId: Int, name: String, type: String) =
        PetRepository.create(
            rescuerId = rescuerId,
            name = name,
            type = type,
            description = "Test description",
            weight = 10.0,
            ageYears = 2,
            ageMonths = 0,
            sex = Gender.MALE,
            breed = "Test breed"
        )
}
