package com.adoptu.ports

import com.adoptu.dto.input.*

interface DynamoDBPort {
    // User operations
    suspend fun createUser(user: UserDto): UserDto?
    suspend fun getUserById(userId: Int): UserDto?
    suspend fun getUserByEmail(email: String): UserDto?
    suspend fun updateUser(user: UserDto): UserDto?
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun getAllUsers(): List<UserDto>
    suspend fun getUsersByRole(role: UserRole): List<UserDto>
    
    // Pet operations
    suspend fun createPet(pet: PetDto): PetDto?
    suspend fun getPetById(petId: Int): PetDto?
    suspend fun updatePet(pet: PetDto): PetDto?
    suspend fun deletePet(petId: Int): Boolean
    suspend fun getAllPets(): List<PetDto>
    suspend fun getPetsByRescuer(rescuerId: Int): List<PetDto>
    
    // Photographer operations
    suspend fun createPhotographer(photographer: PhotographerDto): PhotographerDto?
    suspend fun getPhotographerByUserId(userId: Int): PhotographerDto?
    suspend fun updatePhotographer(photographer: PhotographerDto): PhotographerDto?
    suspend fun deletePhotographer(userId: Int): Boolean
    suspend fun getAllPhotographers(): List<PhotographerDto>
    
    // Shelter operations
    suspend fun createShelter(shelter: ShelterDto): ShelterDto?
    suspend fun getShelterByUserId(userId: Int): ShelterDto?
    suspend fun updateShelter(shelter: ShelterDto): ShelterDto?
    suspend fun deleteShelter(userId: Int): Boolean
    suspend fun getAllShelters(): List<ShelterDto>
    
    // Sterilization Location operations
    suspend fun createSterilizationLocation(location: SterilizationLocationDto): SterilizationLocationDto?
    suspend fun getSterilizationLocationByUserId(userId: Int): SterilizationLocationDto?
    suspend fun updateSterilizationLocation(location: SterilizationLocationDto): SterilizationLocationDto?
    suspend fun deleteSterilizationLocation(userId: Int): Boolean
    suspend fun getAllSterilizationLocations(): List<SterilizationLocationDto>
    
    // Temporal Home operations
    suspend fun createTemporalHome(temporalHome: TemporalHomeDto): TemporalHomeDto?
    suspend fun getTemporalHomeByUserId(userId: Int): TemporalHomeDto?
    suspend fun updateTemporalHome(temporalHome: TemporalHomeDto): TemporalHomeDto?
    suspend fun deleteTemporalHome(userId: Int): Boolean
    suspend fun getAllTemporalHomes(): List<TemporalHomeDto>
}
