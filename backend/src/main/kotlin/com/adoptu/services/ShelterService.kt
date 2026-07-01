package com.adoptu.services

import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.ShelterDto
import com.adoptu.dto.input.UpdateShelterRequest
import com.adoptu.ports.ShelterRepositoryPort

class ShelterService(private val shelterRepository: ShelterRepositoryPort) {

    suspend fun getAll(country: String, state: String? = null, city: String? = null, neighborhood: String? = null, zip: String? = null): List<ShelterDto> {
        require(country.isNotBlank()) { "Country is required" }
        return shelterRepository.getAll(country, state, city, neighborhood, zip)
    }

    suspend fun getById(id: Int): ShelterDto? = shelterRepository.getById(id)

    suspend fun create(request: CreateShelterRequest): ShelterDto {
        require(request.name.isNotBlank()) { "Name is required" }
        require(request.country.isNotBlank()) { "Country is required" }
        require(request.city.isNotBlank()) { "City is required" }
        require(request.address.isNotBlank()) { "Address is required" }
        return shelterRepository.create(request)
    }

    suspend fun update(id: Int, request: UpdateShelterRequest): ServiceResult<ShelterDto> {
        val existing = shelterRepository.getById(id) ?: return ServiceResult.NotFound
        val updated = shelterRepository.update(id, request)
        return if (updated != null) ServiceResult.Success(updated) else ServiceResult.NotFound
    }

    suspend fun delete(id: Int): ServiceResult<Unit> {
        val existing = shelterRepository.getById(id) ?: return ServiceResult.NotFound
        val deleted = shelterRepository.delete(id)
        return if (deleted) ServiceResult.Success(Unit) else ServiceResult.NotFound
    }

    suspend fun getCountries(): List<String> = shelterRepository.getCountries()

    suspend fun getStatesByCountry(country: String): List<String> = shelterRepository.getStatesByCountry(country)
}
