package com.adoptu.services

import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.SterilizationLocationDto
import com.adoptu.dto.input.SterilizationLocationsByLocation
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.ports.SterilizationLocationRepositoryPort

class SterilizationLocationService(private val repository: SterilizationLocationRepositoryPort) {

    fun getAll(country: String? = null, state: String? = null, city: String? = null): List<SterilizationLocationDto> {
        return repository.getAll(country, state, city)
    }

    fun getById(id: Int): SterilizationLocationDto? = repository.getById(id)

    fun create(request: CreateSterilizationLocationRequest): SterilizationLocationDto {
        require(request.name.isNotBlank()) { "Name is required" }
        require(request.country.isNotBlank()) { "Country is required" }
        require(request.city.isNotBlank()) { "City is required" }
        require(request.address.isNotBlank()) { "Address is required" }
        return repository.create(request)
    }

    fun update(id: Int, request: UpdateSterilizationLocationRequest): ServiceResult<SterilizationLocationDto> {
        val existing = repository.getById(id) ?: return ServiceResult.NotFound
        val updated = repository.update(id, request)
        return if (updated != null) ServiceResult.Success(updated) else ServiceResult.NotFound
    }

    fun delete(id: Int): ServiceResult<Unit> {
        val existing = repository.getById(id) ?: return ServiceResult.NotFound
        val deleted = repository.delete(id)
        return if (deleted) ServiceResult.Success(Unit) else ServiceResult.NotFound
    }

    fun getCountries(): List<String> = repository.getCountries()

    fun getStatesByCountry(country: String): List<String> = repository.getStatesByCountry(country)

    fun getCitiesByCountryAndState(country: String, state: String?): List<String> = repository.getCitiesByCountryAndState(country, state)

    fun getGroupedByLocation(): List<SterilizationLocationsByLocation> = repository.getGroupedByLocation()
}
