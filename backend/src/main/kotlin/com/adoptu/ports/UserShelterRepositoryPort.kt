package com.adoptu.ports

import com.adoptu.dto.input.CreateUserShelterRequest
import com.adoptu.dto.input.UpdateUserShelterRequest
import com.adoptu.dto.input.UserShelterDto

interface UserShelterRepositoryPort {
    fun getByUserId(userId: Int): UserShelterDto?
    fun create(userId: Int, request: CreateUserShelterRequest): UserShelterDto
    fun update(userId: Int, request: UpdateUserShelterRequest): UserShelterDto?
    fun delete(userId: Int): Boolean
    fun search(country: String, state: String?, city: String?, neighborhood: String?, zip: String?): List<UserShelterDto>
}