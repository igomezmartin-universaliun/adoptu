package com.adoptu.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssertionOptionsDto(
    val challenge: String,
    val rpId: String
)



@Serializable
data class RelyingPartyDto(val id: String, val name: String)

@Serializable
data class WebAuthnUserIdentityDto(
    val id: String,
    val name: String,
    val displayName: String
)

@Serializable
data class PubKeyCredParamDto(
    val type: String,
    val alg: Int
)

@Serializable
data class RegistrationOptionsDto(
    val rp: RelyingPartyDto,
    val user: WebAuthnUserIdentityDto,
    val challenge: String,
    val pubKeyCredParams: List<PubKeyCredParamDto>
)