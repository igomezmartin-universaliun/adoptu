package com.adoptu.ports

import com.adoptu.dto.input.AdoptionRequestDto
import com.adoptu.dto.input.Currency
import com.adoptu.dto.input.Gender
import com.adoptu.dto.input.PetDto
import com.adoptu.dto.input.PetImageDto
import com.adoptu.dto.input.UpdatePetRequest

interface PetRepositoryPort {
    fun getAll(type: String? = null, showPromotedOnly: Boolean = false): List<PetDto>
    fun getById(id: Int): PetDto?
    fun create(
        rescuerId: Int,
        name: String,
        type: String,
        description: String,
        weight: Double,
        ageYears: Int,
        ageMonths: Int,
        sex: Gender,
        breed: String? = null,
        color: String? = null,
        size: String? = null,
        temperament: String? = null,
        isSterilized: Boolean = false,
        isMicrochipped: Boolean = false,
        microchipId: String? = null,
        vaccinations: String? = null,
        isGoodWithKids: Boolean = true,
        isGoodWithDogs: Boolean = true,
        isGoodWithCats: Boolean = true,
        isHouseTrained: Boolean = false,
        energyLevel: String? = null,
        rescueDate: Long? = null,
        rescueLocation: String? = null,
        specialNeeds: String? = null,
        adoptionFee: Double = 0.0,
        currency: Currency = Currency.USD,
        isUrgent: Boolean = false,
        isPromoted: Boolean = false,
        status: String = "AVAILABLE"
    ): PetDto
    fun update(id: Int, body: UpdatePetRequest): PetDto?
    fun delete(petId: Int)
    fun createAdoptionRequest(petId: Int, adopterId: Int, message: String): AdoptionRequestDto
    fun getAdoptionRequestsForPet(petId: Int): List<AdoptionRequestDto>
    fun getAdoptionRequestsForUser(userId: Int): List<AdoptionRequestDto>
    fun updateAdoptionRequestStatus(requestId: Int, status: String): Boolean
    fun getAdoptionRequestById(requestId: Int): AdoptionRequestDto?
    fun addImage(petId: Int, imageUrl: String, isPrimary: Boolean = false, sortOrder: Int = 0): PetImageDto
    fun removeImage(petId: Int, imageId: Int): Boolean
    fun setPrimaryImage(petId: Int, imageId: Int): Boolean
    fun getImages(petId: Int): List<PetImageDto>
}
