package com.adoptu.services

import com.adoptu.dto.input.CreateUserSterilizationLocationRequest
import com.adoptu.dto.input.UpdateUserSterilizationLocationRequest
import com.adoptu.dto.input.UserSterilizationLocationDto
import com.adoptu.ports.UserSterilizationLocationRepositoryPort

class UserSterilizationLocationService(private val repository: UserSterilizationLocationRepositoryPort) {

    fun getByUserId(userId: Int): UserSterilizationLocationDto? = repository.getByUserId(userId)

    fun create(userId: Int, request: CreateUserSterilizationLocationRequest): UserSterilizationLocationDto {
        require(request.name.isNotBlank()) { "Name is required" }
        require(request.country.isNotBlank()) { "Country is required" }
        require(request.city.isNotBlank()) { "City is required" }
        require(request.address.isNotBlank()) { "Address is required" }
        return repository.create(userId, request)
    }

    fun update(userId: Int, request: UpdateUserSterilizationLocationRequest): ServiceResult<UserSterilizationLocationDto> {
        val existing = repository.getByUserId(userId) ?: return ServiceResult.NotFound
        val updated = repository.update(userId, request)
        return if (updated != null) ServiceResult.Success(updated) else ServiceResult.NotFound
    }

    fun delete(userId: Int): ServiceResult<Unit> {
        val existing = repository.getByUserId(userId) ?: return ServiceResult.NotFound
        val deleted = repository.delete(userId)
        return if (deleted) ServiceResult.Success(Unit) else ServiceResult.NotFound
    }

    fun search(country: String, state: String? = null, city: String? = null, neighborhood: String? = null, zip: String? = null): List<UserSterilizationLocationDto> {
        require(country.isNotBlank()) { "Country is required" }
        return repository.search(country, state, city, neighborhood, zip)
    }
}