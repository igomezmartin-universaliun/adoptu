package com.adoptu.ports

import com.adoptu.dto.input.CreateUserSterilizationLocationRequest
import com.adoptu.dto.input.UpdateUserSterilizationLocationRequest
import com.adoptu.dto.input.UserSterilizationLocationDto

interface UserSterilizationLocationRepositoryPort {
    fun getByUserId(userId: Int): UserSterilizationLocationDto?
    fun create(userId: Int, request: CreateUserSterilizationLocationRequest): UserSterilizationLocationDto
    fun update(userId: Int, request: UpdateUserSterilizationLocationRequest): UserSterilizationLocationDto?
    fun delete(userId: Int): Boolean
    fun search(country: String, state: String?, city: String?, neighborhood: String?, zip: String?): List<UserSterilizationLocationDto>
}