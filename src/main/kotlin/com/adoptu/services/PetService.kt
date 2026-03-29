package com.adoptu.services

import com.adoptu.dto.input.AdoptionRequestDto
import com.adoptu.dto.input.CreatePetRequest
import com.adoptu.dto.input.PetDto
import com.adoptu.dto.input.PetImageDto
import com.adoptu.dto.input.Status
import com.adoptu.dto.input.UpdatePetRequest
import com.adoptu.dto.input.UserRole
import com.adoptu.ports.ImageStoragePort
import com.adoptu.ports.NotificationPort
import com.adoptu.ports.PetRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetService(
    private val petRepository: PetRepositoryPort,
    private val imageStorage: ImageStoragePort,
    private val notificationPort: NotificationPort,
    private val userService: UserService
) {

    fun getAll(type: String? = null, showPromotedOnly: Boolean = false): List<PetDto> = petRepository.getAll(type, showPromotedOnly)

    fun getById(id: Int): PetDto? = petRepository.getById(id)

    fun create(rescuerId: Int, request: CreatePetRequest): PetDto {
        require(request.weight >= 0) { "Weight must be zero or positive" }
        require(request.ageYears >= 0) { "Age (years) must be zero or positive" }
        require(request.ageMonths >= 0) { "Age (months) must be zero or positive" }
        require(request.ageMonths < 12) { "Age (months) must be less than 12" }
        require(request.adoptionFee >= 0) { "Adoption fee must be zero or positive" }
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
            currency = request.currency,
            isUrgent = request.isUrgent,
            isPromoted = request.isPromoted
        )
    }

    fun update(id: Int, userId: Int, userRoles: Set<String>, body: UpdatePetRequest): ServiceResult<PetDto> {
        body.weight?.let { require(it >= 0) { "Weight must be zero or positive" } }
        body.ageYears?.let { require(it >= 0) { "Age (years) must be zero or positive" } }
        body.ageMonths?.let { require(it >= 0) { "Age (months) must be zero or positive" } }
        body.ageMonths?.let { require(it < 12) { "Age (months) must be less than 12" } }
        body.adoptionFee?.let { require(it >= 0) { "Adoption fee must be zero or positive" } }
        val existing = petRepository.getById(id) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val pet = petRepository.update(id, body)
        return if (pet != null) ServiceResult.Success(pet) else ServiceResult.NotFound
    }

    fun delete(id: Int, userId: Int, userRoles: Set<String>): ServiceResult<Unit> {
        val existing = petRepository.getById(id) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        petRepository.delete(id)
        return ServiceResult.Success(Unit)
    }

    fun addImage(petId: Int, userId: Int, userRoles: Set<String>, imageUrl: String, isPrimary: Boolean): ServiceResult<PetImageDto> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val image = petRepository.addImage(petId, imageUrl, isPrimary)
        return ServiceResult.Success(image)
    }

    suspend fun uploadAndAddImage(
        petId: Int,
        userId: Int,
        userRoles: Set<String>,
        imageName: String,
        contentType: String,
        imageData: ByteArray,
        isPrimary: Boolean
    ): ServiceResult<PetImageDto> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }

        val format = if (contentType.contains("png")) "png" else "jpg"
        val compressedStream = ImageCompressor.compress(imageData.inputStream(), format)
        val imageUrl = imageStorage.uploadImage(petId, imageName, contentType, compressedStream.toByteArray().inputStream())

        val image = petRepository.addImage(petId, imageUrl, isPrimary)
        return ServiceResult.Success(image)
    }

    suspend fun removeImage(petId: Int, imageId: Int, userId: Int, userRoles: Set<String>): ServiceResult<Unit> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        val images = petRepository.getImages(petId)
        val image = images.find { it.id == imageId } ?: return ServiceResult.NotFound
        imageStorage.deleteImage(image.imageUrl)
        petRepository.removeImage(petId, imageId)
        return ServiceResult.Success(Unit)
    }

    suspend fun updatePetImages(petId: Int, userId: Int, userRoles: Set<String>, imageIds: List<Int>): ServiceResult<List<PetImageDto>> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
            return ServiceResult.Forbidden
        }

        val existingImages = petRepository.getImages(petId)
        val existingImageIds = existingImages.map { it.id }.toSet()
        val incomingImageIds = imageIds.toSet()
        val imageIdsToDelete = existingImageIds - incomingImageIds

        imageIdsToDelete.forEach { imageId ->
            val image = existingImages.find { it.id == imageId }
            if (image != null) {
                imageStorage.deleteImage(image.imageUrl)
                petRepository.removeImage(petId, imageId)
            }
        }

        val remainingImages = petRepository.getImages(petId)
        return ServiceResult.Success(remainingImages)
    }

    fun setPrimaryImage(petId: Int, imageId: Int, userId: Int, userRoles: Set<String>): ServiceResult<Unit> {
        val existing = petRepository.getById(petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && existing.rescuerId != userId) {
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
            val rescuer = userService.getById(pet.rescuerId)
            val adopter = userService.getById(adopterId)
            if (rescuer?.username != null && rescuer.activeRoles.contains(UserRole.RESCUER) && adopter != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    notificationPort.sendAdoptionRequestNotification(
                        rescuerEmail = rescuer.username,
                        petName = pet.name,
                        adopterName = adopter.displayName,
                        message = message
                    )
                }
            }
        }
        
        return request
    }

    fun getAdoptionRequestsForPet(petId: Int, userId: Int, userRoles: Set<String>): ServiceResult<List<AdoptionRequestDto>> {
        val pet = petRepository.getById(petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && pet.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        return ServiceResult.Success(petRepository.getAdoptionRequestsForPet(petId))
    }

    fun getMyAdoptionRequests(userId: Int): List<AdoptionRequestDto> {
        return petRepository.getAdoptionRequestsForUser(userId)
    }

    fun updateAdoptionRequest(requestId: Int, status: String, userId: Int, userRoles: Set<String>): ServiceResult<AdoptionRequestDto> {
        val request = petRepository.getAdoptionRequestById(requestId) ?: return ServiceResult.NotFound
        val pet = petRepository.getById(request.petId) ?: return ServiceResult.NotFound
        val isAdmin = userRoles.contains("ADMIN")
        if (!isAdmin && pet.rescuerId != userId) {
            return ServiceResult.Forbidden
        }
        if (!listOf("APPROVED", "REJECTED").contains(status)) {
            return ServiceResult.Forbidden
        }
        petRepository.updateAdoptionRequestStatus(requestId, status)
        
        if (status == "APPROVED") {
            petRepository.update(request.petId, UpdatePetRequest(status = Status.ADOPTED))
            petRepository.getAdoptionRequestsForPet(request.petId)
                .filter { it.id != requestId && it.status == "PENDING" }
                .forEach { petRepository.updateAdoptionRequestStatus(it.id, "REJECTED") }
        }
        
        val updatedRequest = petRepository.getAdoptionRequestById(requestId)
        return if (updatedRequest != null) ServiceResult.Success(updatedRequest) else ServiceResult.NotFound
    }
}
