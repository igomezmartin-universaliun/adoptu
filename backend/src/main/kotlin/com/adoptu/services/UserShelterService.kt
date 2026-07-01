package com.adoptu.services

import com.adoptu.dto.input.CreateUserShelterRequest
import com.adoptu.dto.input.UpdateUserShelterRequest
import com.adoptu.dto.input.UserShelterDto
import com.adoptu.ports.UserShelterRepositoryPort

class UserShelterService(private val repository: UserShelterRepositoryPort) {

    suspend fun getByUserId(userId: Int): UserShelterDto? = repository.getByUserId(userId)

    suspend fun create(userId: Int, request: CreateUserShelterRequest): UserShelterDto {
        require(request.name.isNotBlank()) { "Name is required" }
        require(request.country.isNotBlank()) { "Country is required" }
        require(request.city.isNotBlank()) { "City is required" }
        require(request.address.isNotBlank()) { "Address is required" }
        if (repository.getByUserId(userId) != null) {
            val updateRequest = UpdateUserShelterRequest(
                name = request.name, country = request.country, state = request.state,
                city = request.city, neighborhood = request.neighborhood, address = request.address,
                zip = request.zip, phone = request.phone, email = request.email,
                website = request.website, fiscalId = request.fiscalId, bankName = request.bankName,
                accountHolderName = request.accountHolderName, accountNumber = request.accountNumber,
                iban = request.iban, swiftBic = request.swiftBic, currency = request.currency,
                description = request.description
            )
            return repository.update(userId, updateRequest) ?: throw Exception("Failed to update shelter")
        }
        return repository.create(userId, request)
    }

    suspend fun update(userId: Int, request: UpdateUserShelterRequest): ServiceResult<UserShelterDto> {
        val existing = repository.getByUserId(userId) ?: return ServiceResult.NotFound
        val updated = repository.update(userId, request)
        return if (updated != null) ServiceResult.Success(updated) else ServiceResult.NotFound
    }

    suspend fun delete(userId: Int): ServiceResult<Unit> {
        val existing = repository.getByUserId(userId) ?: return ServiceResult.NotFound
        val deleted = repository.delete(userId)
        return if (deleted) ServiceResult.Success(Unit) else ServiceResult.NotFound
    }

    suspend fun search(country: String, state: String? = null, city: String? = null, neighborhood: String? = null, zip: String? = null): List<UserShelterDto> {
        require(country.isNotBlank()) { "Country is required" }
        return repository.search(country, state, city, neighborhood, zip)
    }
}