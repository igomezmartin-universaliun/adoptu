package com.adoptu.ports

import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.ShelterDto
import com.adoptu.dto.input.UpdateShelterRequest

interface ShelterRepositoryPort {
    fun getById(id: Int): ShelterDto?
    fun getAll(country: String, state: String? = null): List<ShelterDto>
    fun create(request: CreateShelterRequest): ShelterDto
    fun update(id: Int, request: UpdateShelterRequest): ShelterDto?
    fun delete(id: Int): Boolean
    fun getCountries(): List<String>
    fun getStatesByCountry(country: String): List<String>
}
