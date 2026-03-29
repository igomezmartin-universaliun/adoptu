package com.adoptu.dto.input

import kotlinx.serialization.Serializable

@Serializable
data class ShelterDto(
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
    val fiscalId: String? = null,
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val accountNumber: String? = null,
    val iban: String? = null,
    val swiftBic: String? = null,
    val currency: String = "USD",
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateShelterRequest(
    val name: String,
    val country: String,
    val state: String? = null,
    val city: String,
    val address: String,
    val zip: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val fiscalId: String? = null,
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val accountNumber: String? = null,
    val iban: String? = null,
    val swiftBic: String? = null,
    val currency: String = "USD",
    val description: String? = null
)

@Serializable
data class UpdateShelterRequest(
    val name: String? = null,
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val address: String? = null,
    val zip: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val fiscalId: String? = null,
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val accountNumber: String? = null,
    val iban: String? = null,
    val swiftBic: String? = null,
    val currency: String? = null,
    val description: String? = null
)

@Serializable
data class ShelterSearchParams(
    val country: String,
    val state: String? = null
)
