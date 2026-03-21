package com.adoptu.auth

import com.adoptu.models.Users
import com.adoptu.models.UserActiveRoles
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
        val role: String
    )

    @Serializable
    data class AuthResult(
        val userId: Int,
        val user: AuthenticatedUser
    )

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun generateRegistrationOptions(email: String, displayName: String): RegistrationOptionsResponse {
        val challenge = ByteArray(32).also { secureRandom.nextBytes(it) }
        ChallengeStore.store(email, challenge)

        val userId = ByteArray(32).also { secureRandom.nextBytes(it) }

        return RegistrationOptionsResponse(
            rp = RelyingParty(id = RP_ID, name = RP_NAME),
            user = PublicKeyUser(
                id = base64UrlEncode(userId),
                name = email,
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
        email: String,
        displayName: String,
        role: String,
        roles: List<String> = listOf(role),
        registrationResponseJson: String
    ): Int? {
        val storedChallenge = ChallengeStore.retrieve(email) ?: return null
        ChallengeStore.remove(email)

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
                val existingUser = Users.selectAll().where { Users.username eq email }.firstOrNull()

                val userId = if (existingUser != null) {
                    existingUser[Users.id]
                } else {
                    Users.insert {
                        it[Users.username] = email
                        it[Users.displayName] = displayName
                        it[Users.createdAt] = System.currentTimeMillis()
                    } get Users.id
                }

                val existingCredential = WebAuthnCredentials
                    .selectAll()
                    .where { WebAuthnCredentials.userId eq userId }
                    .firstOrNull()

                if (existingCredential == null) {
                    WebAuthnCredentials.insert {
                        it[WebAuthnCredentials.userId] = userId
                        it[WebAuthnCredentials.credentialId] = base64UrlEncode(attestedCredentialData.credentialId)
                        it[WebAuthnCredentials.attestedCredentialDataBase64] = Base64.getEncoder().encodeToString(acdBytes)
                        it[WebAuthnCredentials.signCount] = registrationData.attestationObject!!.authenticatorData.signCount
                        it[WebAuthnCredentials.transports] = null
                        it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
                    }
                }

                if (existingUser == null) {
                    roles.forEach { roleName ->
                        UserActiveRoles.insert {
                            it[UserActiveRoles.userId] = userId
                            it[UserActiveRoles.role] = roleName
                        }
                    }
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

            val primaryRole = transaction {
                val roles = UserActiveRoles.selectAll()
                    .where { UserActiveRoles.userId eq userId }
                    .map { it[UserActiveRoles.role] }
                if (roles.contains("ADMIN")) "ADMIN" else roles.firstOrNull() ?: "ADOPTER"
            }

            AuthResult(
                userId = userId,
                user = AuthenticatedUser(
                    id = user[Users.id],
                    username = user[Users.username],
                    displayName = user[Users.displayName],
                    role = primaryRole
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