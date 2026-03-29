package com.adoptu.dto.input

import kotlinx.serialization.Serializable

@Serializable
data class SterilizationLocationDto(
    val id: Int,
    val name: String,
    val country: String,
    val state: String? = null,
    val city: String,
    val address: String,
    val zip: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateSterilizationLocationRequest(
    val name: String,
    val country: String,
    val state: String? = null,
    val city: String,
    val address: String,
    val zip: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val description: String? = null
)

@Serializable
data class UpdateSterilizationLocationRequest(
    val name: String? = null,
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val address: String? = null,
    val zip: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val description: String? = null
)

@Serializable
data class SterilizationLocationSearchParams(
    val country: String? = null,
    val state: String? = null,
    val city: String? = null
)

@Serializable
data class SterilizationLocationsByLocation(
    val country: String,
    val states: List<SterilizationLocationsByState>
)

@Serializable
data class SterilizationLocationsByState(
    val state: String?,
    val cities: List<SterilizationLocationsByCity>
)

@Serializable
data class SterilizationLocationsByCity(
    val city: String,
    val locations: List<SterilizationLocationDto>
)
