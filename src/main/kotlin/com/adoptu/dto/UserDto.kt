package com.adoptu.dto

import kotlinx.serialization.Serializable

enum class UserRole {
    ADMIN, RESCUER, ADOPTER, PHOTOGRAPHER, TEMPORAL_HOME
}

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val displayName: String,
    val language: String = "en",
    val activeRoles: Set<UserRole> = emptySet(),
    val lastAcceptedPrivacyPolicy: Long? = null,
    val lastAcceptedTermsAndConditions: Long? = null
)

@Serializable
data class PhotographerDto(
    val userId: Int,
    val displayName: String,
    val username: String? = null,
    val photographerFee: Double? = null,
    val photographerCurrency: String? = null
)

@Serializable
data class AcceptTermsRequest(
    val acceptPrivacyPolicy: Boolean = false,
    val acceptTermsAndConditions: Boolean = false
)

@Serializable
data class PhotographerSettingsRequest(
    val photographerFee: Double,
    val photographerCurrency: String
)

@Serializable
data class PhotographyRequestDto(
    val id: Int,
    val photographerId: Int,
    val photographerName: String? = null,
    val requesterId: Int,
    val requesterName: String? = null,
    val petId: Int? = null,
    val petName: String? = null,
    val message: String? = null,
    val status: String,
    val scheduledDate: Long? = null,
    val createdAt: Long
)

@Serializable
data class CreatePhotographyRequestRequest(
    val photographerId: Int,
    val petId: Int? = null,
    val message: String? = null
)

@Serializable
data class UpdatePhotographyRequestRequest(
    val status: String? = null,
    val scheduledDate: Long? = null
)

@Serializable
data class CreateMultiPhotographerRequestRequest(
    val photographerIds: List<Int>,
    val petId: Int? = null,
    val message: String
)

@Serializable
data class RoleActivationRequest(
    val activate: Boolean
)

@Serializable
data class TemporalHomeDto(
    val userId: Int,
    val alias: String,
    val country: String,
    val state: String? = null,
    val city: String,
    val zip: String? = null,
    val neighborhood: String? = null,
    val createdAt: Long
)

@Serializable
data class TemporalHomeSearchParams(
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val zip: String? = null,
    val neighborhood: String? = null
)

@Serializable
data class CreateTemporalHomeRequest(
    val alias: String,
    val country: String,
    val state: String? = null,
    val city: String,
    val zip: String? = null,
    val neighborhood: String? = null
)

@Serializable
data class SendTemporalHomeRequestRequest(
    val temporalHomeId: Int,
    val petId: Int? = null,
    val message: String
)

@Serializable
data class BlockRescuerRequest(
    val rescuerId: Int
)

@Serializable
data class TemporalHomeRequestDto(
    val id: Int,
    val temporalHomeId: Int,
    val temporalHomeAlias: String? = null,
    val rescuerId: Int,
    val rescuerName: String? = null,
    val petId: Int? = null,
    val petName: String? = null,
    val message: String,
    val status: String,
    val createdAt: Long
)
