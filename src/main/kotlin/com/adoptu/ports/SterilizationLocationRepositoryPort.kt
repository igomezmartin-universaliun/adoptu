package com.adoptu.ports

import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.SterilizationLocationDto
import com.adoptu.dto.input.SterilizationLocationsByLocation
import com.adoptu.dto.input.UpdateSterilizationLocationRequest

interface SterilizationLocationRepositoryPort {
    fun getById(id: Int): SterilizationLocationDto?
    fun getAll(country: String? = null, state: String? = null, city: String? = null): List<SterilizationLocationDto>
    fun create(request: CreateSterilizationLocationRequest): SterilizationLocationDto
    fun update(id: Int, request: UpdateSterilizationLocationRequest): SterilizationLocationDto?
    fun delete(id: Int): Boolean
    fun getCountries(): List<String>
    fun getStatesByCountry(country: String): List<String>
    fun getCitiesByCountryAndState(country: String, state: String?): List<String>
    fun getGroupedByLocation(): List<SterilizationLocationsByLocation>
}
