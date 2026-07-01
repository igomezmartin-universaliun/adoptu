package com.adoptu.ports

import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.SterilizationLocationDto
import com.adoptu.dto.input.SterilizationLocationsByLocation
import com.adoptu.dto.input.UpdateSterilizationLocationRequest

interface SterilizationLocationRepositoryPort {
    suspend fun getById(id: Int): SterilizationLocationDto?
    suspend fun getAll(country: String? = null, state: String? = null, city: String? = null, neighborhood: String? = null, zip: String? = null): List<SterilizationLocationDto>
    suspend fun create(request: CreateSterilizationLocationRequest): SterilizationLocationDto
    suspend fun update(id: Int, request: UpdateSterilizationLocationRequest): SterilizationLocationDto?
    suspend fun delete(id: Int): Boolean
    suspend fun getCountries(): List<String>
    suspend fun getStatesByCountry(country: String): List<String>
    suspend fun getCitiesByCountryAndState(country: String, state: String?): List<String>
    suspend fun getGroupedByLocation(): List<SterilizationLocationsByLocation>
}
