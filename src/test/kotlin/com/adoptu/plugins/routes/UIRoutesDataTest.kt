package com.adoptu.plugins.routes

import com.adoptu.dto.Gender
import com.adoptu.dto.Status
import com.adoptu.dto.Currency
import com.adoptu.models.Users
import com.adoptu.models.UserActiveRoles
import com.adoptu.repositories.PetRepository
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class UIRoutesDataTest {

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        
        transaction {
            try {
                Users.insert {
                    it[Users.id] = 1
                    it[Users.username] = "rescuer1@mocks.com"
                    it[Users.displayName] = "Rescuer One"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 1
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) { }
            
            try {
                Users.insert {
                    it[Users.id] = 2
                    it[Users.username] = "rescuer2@mocks.com"
                    it[Users.displayName] = "Rescuer Two"
                    it[Users.createdAt] = System.currentTimeMillis()
                }
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = 2
                    it[UserActiveRoles.role] = "RESCUER"
                }
            } catch (e: Exception) { }
            
            try {
                val adminId = Users.insert {
                    it[Users.id] = 3
                    it[Users.username] = "admin@mocks.com"
                    it[Users.displayName] = "Admin"
                    it[Users.createdAt] = System.currentTimeMillis()
                } get Users.id
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = adminId
                    it[UserActiveRoles.role] = "ADMIN"
                }
            } catch (e: Exception) { }
        }
    }

    // ==================== Pets List Data ====================

    @Test
    fun `pets list returns all available pets`() {
        // Create pets
        PetRepository.create(
            rescuerId = 1,
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        PetRepository.create(
            rescuerId = 1,
            name = "Luna",
            type = "CAT",
            description = "A sweet cat",
            weight = 4.5,
            ageYears = 2,
            ageMonths = 0,
            sex = Gender.FEMALE,
            status = "AVAILABLE"
        )
        
        val pets = PetRepository.getAll()
        
        assertEquals(2, pets.size)
    }

    @Test
    fun `pets list filters by type DOG`() {
        PetRepository.create(
            rescuerId = 1,
            name = "Dog1",
            type = "DOG",
            description = "Dog",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        PetRepository.create(
            rescuerId = 1,
            name = "Cat1",
            type = "CAT",
            description = "Cat",
            weight = 5.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.FEMALE,
            status = "AVAILABLE"
        )
        
        val dogs = PetRepository.getAll("DOG")
        
        assertEquals(1, dogs.size)
        assertEquals("DOG", dogs[0].type)
    }

    @Test
    fun `pets list filters by type CAT`() {
        PetRepository.create(
            rescuerId = 1,
            name = "Cat1",
            type = "CAT",
            description = "Cat",
            weight = 5.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.FEMALE,
            status = "AVAILABLE"
        )
        
        PetRepository.create(
            rescuerId = 1,
            name = "Dog1",
            type = "DOG",
            description = "Dog",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        val cats = PetRepository.getAll("CAT")
        
        assertEquals(1, cats.size)
        assertEquals("CAT", cats[0].type)
    }

    @Test
    fun `pets list excludes adopted pets`() {
        PetRepository.create(
            rescuerId = 1,
            name = "Available",
            type = "DOG",
            description = "Available",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        PetRepository.create(
            rescuerId = 1,
            name = "Adopted",
            type = "DOG",
            description = "Adopted",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "ADOPTED"
        )
        
        val pets = PetRepository.getAll()
        
        assertEquals(1, pets.size)
        assertEquals(Status.AVAILABLE, pets[0].status)
    }

    // ==================== Pet Detail Data ====================

    @Test
    fun `pet detail returns pet by id`() {
        val created = PetRepository.create(
            rescuerId = 1,
            name = "Buddy",
            type = "DOG",
            description = "A friendly dog",
            weight = 25.0,
            ageYears = 3,
            ageMonths = 6,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        val pet = PetRepository.getById(created.id)
        
        assertNotNull(pet)
        assertEquals("Buddy", pet.name)
    }

    @Test
    fun `pet detail returns null for non-existent id`() {
        val pet = PetRepository.getById(999)
        
        assertNull(pet)
    }

    @Test
    fun `pet detail includes all attributes`() {
        val created = PetRepository.create(
            rescuerId = 1,
            name = "Max",
            type = "DOG",
            breed = "German Shepherd",
            description = "A loyal dog",
            weight = 35.0,
            ageYears = 4,
            ageMonths = 3,
            sex = Gender.MALE,
            color = "Black/Tan",
            size = "LARGE",
            temperament = "Loyal",
            isSterilized = true,
            isMicrochipped = true,
            microchipId = "ABC123456",
            isGoodWithKids = true,
            isGoodWithDogs = true,
            isGoodWithCats = false,
            isHouseTrained = true,
            energyLevel = "HIGH",
            rescueDate = System.currentTimeMillis(),
            rescueLocation = "City Shelter",
            specialNeeds = "None",
            adoptionFee = 250.0,
            currency = Currency.USD,
            isUrgent = false,
            status = "AVAILABLE"
        )
        
        val pet = PetRepository.getById(created.id)
        
        assertNotNull(pet)
        assertEquals("Max", pet.name)
        assertEquals("DOG", pet.type)
        assertEquals("German Shepherd", pet.breed)
        assertEquals("A loyal dog", pet.description)
        assertEquals(35.0, pet.weight)
        assertEquals(4, pet.ageYears)
        assertEquals(3, pet.ageMonths)
        assertEquals(Gender.MALE, pet.sex)
        assertEquals("Black/Tan", pet.color)
        assertEquals("LARGE", pet.size)
        assertEquals("Loyal", pet.temperament)
        assertTrue(pet.isSterilized)
        assertTrue(pet.isMicrochipped)
        assertEquals("ABC123456", pet.microchipId)
        assertTrue(pet.isGoodWithKids)
        assertTrue(pet.isGoodWithDogs)
        assertFalse(pet.isGoodWithCats)
        assertTrue(pet.isHouseTrained)
        assertEquals("HIGH", pet.energyLevel)
        assertEquals("City Shelter", pet.rescueLocation)
        assertEquals("None", pet.specialNeeds)
        assertEquals(250.0, pet.adoptionFee)
        assertEquals(Currency.USD, pet.currency)
        assertFalse(pet.isUrgent)
        assertEquals(Status.AVAILABLE, pet.status)
    }

    // ==================== Pet Images ====================

    @Test
    fun `pet images returns empty list when no images`() {
        val created = PetRepository.create(
            rescuerId = 1,
            name = "NoImages",
            type = "DOG",
            description = "No images",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        val images = PetRepository.getImages(created.id)
        
        assertEquals(0, images.size)
    }

    @Test
    fun `pet images returns added images`() {
        val created = PetRepository.create(
            rescuerId = 1,
            name = "WithImages",
            type = "DOG",
            description = "With images",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        PetRepository.addImage(created.id, "https://s3.example.com/img1.jpg", true, 0)
        PetRepository.addImage(created.id, "https://s3.example.com/img2.jpg", false, 1)
        
        val images = PetRepository.getImages(created.id)
        
        assertEquals(2, images.size)
    }

    @Test
    fun `pet images first added is primary by default`() {
        val created = PetRepository.create(
            rescuerId = 1,
            name = "PrimaryTest",
            type = "DOG",
            description = "Primary mocks",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        PetRepository.addImage(created.id, "https://s3.example.com/first.jpg", false, 0)
        PetRepository.addImage(created.id, "https://s3.example.com/second.jpg", true, 1)
        
        val images = PetRepository.getImages(created.id)
        
        val primary = images.find { it.isPrimary }
        assertNotNull(primary)
        assertEquals("https://s3.example.com/second.jpg", primary.imageUrl)
    }

    // ==================== Urgent Pets ====================

    @Test
    fun `urgent pets show isUrgent flag`() {
        val urgent = PetRepository.create(
            rescuerId = 1,
            name = "UrgentBuddy",
            type = "DOG",
            description = "Urgent",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE",
            isUrgent = true
        )
        
        val pet = PetRepository.getById(urgent.id)
        
        assertNotNull(pet)
        assertTrue(pet.isUrgent)
    }

    @Test
    fun `non-urgent pets show isUrgent false`() {
        val normal = PetRepository.create(
            rescuerId = 1,
            name = "NormalBuddy",
            type = "DOG",
            description = "Normal",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE",
            isUrgent = false
        )
        
        val pet = PetRepository.getById(normal.id)
        
        assertNotNull(pet)
        assertFalse(pet.isUrgent)
    }

    // ==================== Pet Gender Display ====================

    @Test
    fun `pets show MALE gender`() {
        val male = PetRepository.create(
            rescuerId = 1,
            name = "MalePet",
            type = "DOG",
            description = "Male",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        val pet = PetRepository.getById(male.id)
        
        assertNotNull(pet)
        assertEquals(Gender.MALE, pet.sex)
    }

    @Test
    fun `pets show FEMALE gender`() {
        val female = PetRepository.create(
            rescuerId = 1,
            name = "FemalePet",
            type = "CAT",
            description = "Female",
            weight = 5.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.FEMALE,
            status = "AVAILABLE"
        )
        
        val pet = PetRepository.getById(female.id)
        
        assertNotNull(pet)
        assertEquals(Gender.FEMALE, pet.sex)
    }

    // ==================== Pet Size Display ====================

    @Test
    fun `pets show SMALL size`() {
        val pet = PetRepository.create(
            rescuerId = 1,
            name = "SmallPet",
            type = "CAT",
            description = "Small",
            weight = 3.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            size = "SMALL",
            status = "AVAILABLE"
        )
        
        val retrieved = PetRepository.getById(pet.id)
        
        assertNotNull(retrieved)
        assertEquals("SMALL", retrieved.size)
    }

    @Test
    fun `pets show MEDIUM size`() {
        val pet = PetRepository.create(
            rescuerId = 1,
            name = "MediumPet",
            type = "DOG",
            description = "Medium",
            weight = 15.0,
            ageYears = 2,
            ageMonths = 0,
            sex = Gender.FEMALE,
            size = "MEDIUM",
            status = "AVAILABLE"
        )
        
        val retrieved = PetRepository.getById(pet.id)
        
        assertNotNull(retrieved)
        assertEquals("MEDIUM", retrieved.size)
    }

    @Test
    fun `pets show LARGE size`() {
        val pet = PetRepository.create(
            rescuerId = 1,
            name = "LargePet",
            type = "DOG",
            description = "Large",
            weight = 40.0,
            ageYears = 3,
            ageMonths = 0,
            sex = Gender.MALE,
            size = "LARGE",
            status = "AVAILABLE"
        )
        
        val retrieved = PetRepository.getById(pet.id)
        
        assertNotNull(retrieved)
        assertEquals("LARGE", retrieved.size)
    }

    // ==================== Rescue Date ====================

    @Test
    fun `pets show rescue date when present`() {
        val rescueTime = System.currentTimeMillis()
        val pet = PetRepository.create(
            rescuerId = 1,
            name = "RescuedPet",
            type = "DOG",
            description = "Rescued",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            rescueDate = rescueTime,
            status = "AVAILABLE"
        )
        
        val retrieved = PetRepository.getById(pet.id)
        
        assertNotNull(retrieved)
        assertNotNull(retrieved.rescueDate)
    }

    @Test
    fun `pets show null rescue date when not set`() {
        val pet = PetRepository.create(
            rescuerId = 1,
            name = "NoRescueDate",
            type = "DOG",
            description = "No rescue date",
            weight = 10.0,
            ageYears = 1,
            ageMonths = 0,
            sex = Gender.MALE,
            status = "AVAILABLE"
        )
        
        val retrieved = PetRepository.getById(pet.id)
        
        assertNotNull(retrieved)
        assertNull(retrieved.rescueDate)
    }
}
