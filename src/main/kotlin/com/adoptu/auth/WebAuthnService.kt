package com.adoptu.auth

import com.adoptu.models.Users
import com.adoptu.models.WebAuthnCredentials
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.RegistrationData
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.security.SecureRandom
import java.util.*

object WebAuthnService {
    private val objectConverter = ObjectConverter()
    private val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter)
    private val attestedCredentialDataConverter = AttestedCredentialDataConverter(objectConverter)
    private val secureRandom = SecureRandom()
    private const val RP_ID = "localhost"
    private const val RP_NAME = "Adopt-U Pet Adoption"
    private const val ORIGIN = "http://localhost:8080"

    @Serializable
    data class RelyingParty(
        val id: String,
        val name: String
    )

    @Serializable
    data class PublicKeyUser(
        val id: String,
        val name: String,
        val displayName: String
    )

    @Serializable
    data class PubKeyCredParam(
        val type: String,
        val alg: Int
    )

    @Serializable
    data class RegistrationOptionsResponse(
        val rp: RelyingParty,
        val user: PublicKeyUser,
        val challenge: String,
        val pubKeyCredParams: List<PubKeyCredParam>
    )

    @Serializable
    data class AssertionOptionsResponse(
        val challenge: String,
        val rpId: String,
        val userVerification: String
    )

    @Serializable
    data class AuthenticatedUser(
        val id: Int,
        val username: String,
        val displayName: String,
        val email: String? = null,
        val role: String
    )

    @Serializable
    data class AuthResult(
        val userId: Int,
        val user: AuthenticatedUser
    )

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun generateRegistrationOptions(username: String, displayName: String): RegistrationOptionsResponse {
        val challenge = ByteArray(32).also { secureRandom.nextBytes(it) }
        ChallengeStore.store(username, challenge)

        val userId = ByteArray(32).also { secureRandom.nextBytes(it) }

        return RegistrationOptionsResponse(
            rp = RelyingParty(id = RP_ID, name = RP_NAME),
            user = PublicKeyUser(
                id = base64UrlEncode(userId),
                name = username,
                displayName = displayName
            ),
            challenge = base64UrlEncode(challenge),
            pubKeyCredParams = listOf(
                PubKeyCredParam(type = "public-key", alg = -7),
                PubKeyCredParam(type = "public-key", alg = -257)
            )
        )
    }

    fun verifyAndRegister(
        username: String,
        displayName: String,
        role: String,
        registrationResponseJson: String
    ): Int? {
        val storedChallenge = ChallengeStore.retrieve(username) ?: return null
        ChallengeStore.remove(username)

        val serverProperty = ServerProperty.builder()
            .origin(Origin(ORIGIN))
            .rpId(RP_ID)
            .challenge(DefaultChallenge(storedChallenge))
            .build()

        return try {
            val registrationData: RegistrationData =
                webAuthnManager.parseRegistrationResponseJSON(registrationResponseJson)
            val params = RegistrationParameters(serverProperty, null, false, true)
            webAuthnManager.verify(registrationData, params)

            val attestedCredentialData =
                registrationData.attestationObject!!.authenticatorData.attestedCredentialData!!
            val acdBytes = attestedCredentialDataConverter.convert(attestedCredentialData)

            transaction {
                val userId = Users.insert {
                    it[Users.username] = username
                    it[Users.displayName] = displayName
                    it[Users.role] = role
                    it[Users.createdAt] = System.currentTimeMillis()
                } get Users.id

                WebAuthnCredentials.insert {
                    it[WebAuthnCredentials.userId] = userId
                    it[WebAuthnCredentials.credentialId] = base64UrlEncode(attestedCredentialData.credentialId)
                    it[WebAuthnCredentials.attestedCredentialDataBase64] = Base64.getEncoder().encodeToString(acdBytes)
                    it[WebAuthnCredentials.signCount] = registrationData.attestationObject!!.authenticatorData.signCount
                    it[WebAuthnCredentials.transports] = null
                    it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
                }

                userId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateAssertionOptions(): AssertionOptionsResponse {
        val challenge = ByteArray(32).also { secureRandom.nextBytes(it) }
        ChallengeStore.storeAssertion(challenge)

        return AssertionOptionsResponse(
            challenge = base64UrlEncode(challenge),
            rpId = RP_ID,
            userVerification = "required"
        )
    }

    fun verifyAndAuthenticate(authenticationResponseJson: String): AuthResult? {
        val authenticationData = try {
            webAuthnManager.parseAuthenticationResponseJSON(authenticationResponseJson)
        } catch (_: Exception) {
            return null
        }

        val credentialId = authenticationData.credentialId
        val credentialIdB64 = base64UrlEncode(credentialId)

        val credentialRow = transaction {
            WebAuthnCredentials
                .selectAll()
                .where { WebAuthnCredentials.credentialId eq credentialIdB64 }
                .firstOrNull()
        } ?: return null

        val storedChallenge = ChallengeStore.retrieveAssertion() ?: return null
        ChallengeStore.removeAssertion()

        val serverProperty = ServerProperty.builder()
            .origin(Origin(ORIGIN))
            .rpId(RP_ID)
            .challenge(DefaultChallenge(storedChallenge))
            .build()

        val acdBytes = Base64.getDecoder().decode(credentialRow[WebAuthnCredentials.attestedCredentialDataBase64])
        val attestedCredentialData = attestedCredentialDataConverter.convert(acdBytes)

        val credentialRecord = CredentialRecordImpl(
            null,
            null,
            null,
            null,
            credentialRow[WebAuthnCredentials.signCount],
            attestedCredentialData,
            null,
            null,
            null,
            null
        )

        return try {
            val params = AuthenticationParameters(serverProperty, credentialRecord, null, true, true)
            webAuthnManager.verify(authenticationData, params)

            transaction {
                WebAuthnCredentials.update({ WebAuthnCredentials.id eq credentialRow[WebAuthnCredentials.id] }) {
                    it[signCount] = authenticationData.authenticatorData!!.signCount
                }
            }

            val userId = credentialRow[WebAuthnCredentials.userId]
            val user = transaction {
                Users.selectAll().where { Users.id eq userId }.firstOrNull()
            } ?: return null

            AuthResult(
                userId = userId,
                user = AuthenticatedUser(
                    id = user[Users.id],
                    username = user[Users.username],
                    displayName = user[Users.displayName],
                    role = user[Users.role]
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private object ChallengeStore {
        private val challenges = mutableMapOf<String, ByteArray>()
        private var assertionChallenge: ByteArray? = null

        fun store(username: String, challenge: ByteArray) {
            challenges[username] = challenge
        }

        fun retrieve(username: String): ByteArray? = challenges.remove(username)

        fun remove(username: String) {
            challenges.remove(username)
        }

        fun storeAssertion(challenge: ByteArray) {
            assertionChallenge = challenge
        }

        fun retrieveAssertion(): ByteArray? = assertionChallenge.also { assertionChallenge = null }
        fun removeAssertion() {
            assertionChallenge = null
        }
    }
}