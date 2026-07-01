package com.adoptu.services

import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.SterilizationLocationDto
import com.adoptu.dto.input.SterilizationLocationsByLocation
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.ports.SterilizationLocationRepositoryPort

class SterilizationLocationService(private val repository: SterilizationLocationRepositoryPort) {

    suspend fun getAll(country: String? = null, state: String? = null, city: String? = null, neighborhood: String? = null, zip: String? = null): List<SterilizationLocationDto> {
        return repository.getAll(country, state, city, neighborhood, zip)
    }

    suspend fun getById(id: Int): SterilizationLocationDto? = repository.getById(id)

    suspend fun create(request: CreateSterilizationLocationRequest): SterilizationLocationDto {
        require(request.name.isNotBlank()) { "Name is required" }
        require(request.country.isNotBlank()) { "Country is required" }
        require(request.city.isNotBlank()) { "City is required" }
        require(request.address.isNotBlank()) { "Address is required" }
        return repository.create(request)
    }

    suspend fun update(id: Int, request: UpdateSterilizationLocationRequest): ServiceResult<SterilizationLocationDto> {
        val existing = repository.getById(id) ?: return ServiceResult.NotFound
        val updated = repository.update(id, request)
        return if (updated != null) ServiceResult.Success(updated) else ServiceResult.NotFound
    }

    suspend fun delete(id: Int): ServiceResult<Unit> {
        val existing = repository.getById(id) ?: return ServiceResult.NotFound
        val deleted = repository.delete(id)
        return if (deleted) ServiceResult.Success(Unit) else ServiceResult.NotFound
    }

    suspend fun getCountries(): List<String> = repository.getCountries()

    suspend fun getStatesByCountry(country: String): List<String> = repository.getStatesByCountry(country)

    suspend fun getCitiesByCountryAndState(country: String, state: String?): List<String> = repository.getCitiesByCountryAndState(country, state)

    suspend fun getGroupedByLocation(): List<SterilizationLocationsByLocation> = repository.getGroupedByLocation()
}
