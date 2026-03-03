package com.adoptu.services

import com.adoptu.domains.image.ImageStoragePort
import com.adoptu.dto.*
import com.adoptu.repositories.PetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetService(
    private val petRepository: PetRepository,
    private val imageStorage: ImageStoragePort,
    private val emailService: EmailService
) {

    fun getAll(type: String? = null): List<PetDto> = petRepository.getAll(type)

    fun getById(id: Int): PetDto? = petRepository.getById(id)

    fun create(rescuerId: Int, request: CreatePetRequest): PetDto {
        require(request.weight >= 0) { "Weight must be zero or positive" }
        require(request.ageYears >= 0) { "Age (years) must be zero or positive" }
        require(request.ageMonths >= 0) { "Age (months) must be zero or positive" }
        require(request.ageMonths < 12) { "Age (months) must be less than 12" }
        return petRepository.create(
            rescuerId = rescuerId,
            name = request.name,
            type = request.type,
            breed = request.breed,
            description = request.description,
            weight = request.weight,
            ageYears = request.ageYears,
            ageMonths = request.ageMonths,
            sex = request.sex,
            color = request.color,
            size = request.size,
            temperament = request.temperament,
            isSterilized = request.isSterilized,
            isMicrochipped = request.isMicrochipped,
            microchipId = request.microchipId,
            vaccinations = request.vaccinations,
            isGoodWithKids = request.isGoodWithKids,
            isGoodWithDogs = request.isGoodWithDogs,
            isGoodWithCats = request.isGoodWithCats,
            isHouseTrained = request.isHouseTrained,
            energyLevel = request.energyLevel,
            rescueDate = request.rescueDate,
            rescueLocation = request.rescueLocation,
            specialNeeds = request.specialNeeds,
            adoptionFee = request.adoptionFee,
            isUrgent = request.isUrgent
        )
    }

    fun update(id: Int, userId: Int, userRole: UserRole, body: UpdatePetRequest): ServiceResult<PetDto> {
        body.weight?.let { require(it >= 0) { "Weight must be zero or positive" } }
        body.ageYears?.let { require(it >= 0) { "Age (years) must be zero or positive" } }
        body.ageMonths?.let { require(it >= 0) { "Age (months) must be zero or positive" } }
        body.ageMonths?.let { require(it < 12) { "Age (months) must be less than 12" } }
        val existing = petRepository.getById(id) ?: return ServiceResult.NotFound
        if (userRole != UserRole.ADMIN && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val pet = petRepository.update(id, body)
        return if (pet != null) ServiceResult.Success(pet) else ServiceResult.NotFound
    }

    fun delete(id: Int, userId: Int, userRole: UserRole): ServiceResult<Unit> {
        val existing = petRepository.getById(id) ?: return ServiceResult.NotFound
        if (userRole != UserRole.ADMIN && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        petRepository.delete(id)
        return ServiceResult.Success(Unit)
    }

    fun addImage(petId: Int, userId: Int, userRole: UserRole, imageUrl: String, isPrimary: Boolean): ServiceResult<PetImageDto> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        if (userRole != UserRole.ADMIN && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val image = petRepository.addImage(petId, imageUrl, isPrimary)
        return ServiceResult.Success(image)
    }

    suspend fun removeImage(petId: Int, imageId: Int, userId: Int, userRole: UserRole): ServiceResult<Unit> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        if (userRole != UserRole.ADMIN && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val images = petRepository.getImages(petId)
        val image = images.find { it.id == imageId } ?: return ServiceResult.NotFound
        imageStorage.deleteImage(image.imageUrl)
        petRepository.removeImage(petId, imageId)
        return ServiceResult.Success(Unit)
    }

    fun setPrimaryImage(petId: Int, imageId: Int, userId: Int, userRole: UserRole): ServiceResult<Unit> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        if (userRole != UserRole.ADMIN && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val success = petRepository.setPrimaryImage(petId, imageId)
        return if (success) ServiceResult.Success(Unit) else ServiceResult.NotFound
    }

    fun getImages(petId: Int): List<PetImageDto> = petRepository.getImages(petId)

    fun createAdoptionRequest(petId: Int, adopterId: Int, message: String): AdoptionRequestDto {
        val request = petRepository.createAdoptionRequest(petId, adopterId, message)
        
        val pet = petRepository.getById(petId)
        if (pet != null) {
            val rescuer = UserService.getById(pet.rescuerId)
            val adopter = UserService.getById(adopterId)
            if (rescuer?.email != null && adopter != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    emailService.sendAdoptionRequestNotification(
                        rescuerEmail = rescuer.email,
                        petName = pet.name,
                        adopterName = adopter.displayName,
                        message = message
                    )
                }
            }
        }
        
        return request
    }
}
