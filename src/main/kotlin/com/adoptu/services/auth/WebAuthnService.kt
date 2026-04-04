package com.adoptu.services.auth

import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.WebAuthnCredentials
import com.adoptu.dto.input.UserRole
import com.adoptu.services.EmailVerificationService
import com.adoptu.services.MagicLinkService
import com.adoptu.services.PasswordService
import com.adoptu.services.UserService
import com.hash_net.beelinecrypto.CryptoService
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.security.SecureRandom
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WebAuthnService(
    private val clock: Clock,
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val passwordService: PasswordService,
    private val magicLinkService: MagicLinkService,
    private val adminEmail: String,
    private val rpId: String,
    private val rpName: String,
    private val origin: String
) {
    private val objectConverter = ObjectConverter()
    private val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter)
    private val attestedCredentialDataConverter = AttestedCredentialDataConverter(objectConverter)
    private val secureRandom = SecureRandom()

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

    data class RegistrationResult(
        val userId: Int,
        val emailSent: Boolean
    )

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun generateRegistrationOptions(email: String, displayName: String): RegistrationOptionsResponse {
        val challenge = ByteArray(32).also { secureRandom.nextBytes(it) }
        ChallengeStore.store(email, challenge)

        val userId = ByteArray(32).also { secureRandom.nextBytes(it) }

        return RegistrationOptionsResponse(
            rp = RelyingParty(id = rpId, name = rpName),
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
        roles: Set<UserRole>,
        registrationResponseJson: String,
        language: String = "en"
    ): RegistrationResult? {
        val storedChallenge = ChallengeStore.retrieve(email) ?: return null
        ChallengeStore.remove(email)

        val serverProperty = ServerProperty.builder()
            .origin(Origin(origin))
            .rpId(rpId)
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

            val userId = transaction {
                val existingUser = Users.selectAll().where { Users.username eq email }.firstOrNull()

                val id = if (existingUser != null) {
                    existingUser[Users.id]
                } else {
                    Users.insert {
                        it[Users.username] = email
                        it[Users.displayName] = displayName
                        it[Users.createdAt] = clock.now().toEpochMilliseconds()
                    } get Users.id
                }

                val existingCredential = WebAuthnCredentials
                    .selectAll()
                    .where { WebAuthnCredentials.userId eq id }
                    .firstOrNull()

                if (existingCredential == null) {
                    WebAuthnCredentials.insert {
                        it[WebAuthnCredentials.userId] = id
                        it[WebAuthnCredentials.credentialId] = base64UrlEncode(attestedCredentialData.credentialId)
                        it[WebAuthnCredentials.attestedCredentialDataBase64] = Base64.getEncoder().encodeToString(acdBytes)
                        it[WebAuthnCredentials.signCount] = registrationData.attestationObject!!.authenticatorData.signCount
                        it[WebAuthnCredentials.transports] = null
                        it[WebAuthnCredentials.createdAt] = clock.now().toEpochMilliseconds()
                    }
                }

                if (existingUser == null) {
                    val effectiveRoles = if (email.equals(adminEmail, ignoreCase = true)) {
                        roles + UserRole.ADMIN
                    } else {
                        roles
                    }
                    effectiveRoles.forEach { role ->
                        UserActiveRoles.insert {
                            it[UserActiveRoles.userId] = id
                            it[UserActiveRoles.role] = role.name
                        }
                    }
                }

                id
            }

            val emailResult = runBlocking {
                emailVerificationService.generateAndSendVerificationEmail(userId, email, displayName, language)
            }

            val emailSent = emailResult.getOrDefault(false)
            RegistrationResult(userId = userId, emailSent = emailSent)
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
            rpId = rpId,
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
            .origin(Origin(origin))
            .rpId(rpId)
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

    suspend fun resendVerificationEmail(userId: Int): Boolean {
        val user = userService.getById(userId) ?: return false
        
        if (userService.isUserVerified(userId)) {
            return false
        }

        if (!emailVerificationService.canSendVerificationEmail(userId)) {
            return false
        }

        val email = user.email ?: return false
        val result = emailVerificationService.resendVerificationEmail(userId, email, user.displayName, user.language)
        return result.getOrDefault(false)
    }

    suspend fun resendVerificationEmailByEmail(email: String): Boolean {
        val user = userService.getByEmail(email) ?: return false
        
        if (userService.isUserVerified(user.id)) {
            return false
        }

        if (!emailVerificationService.canSendVerificationEmail(user.id)) {
            return false
        }

        val result = emailVerificationService.resendVerificationEmail(user.id, email, user.displayName, user.language)
        return result.getOrDefault(false)
    }

    fun verifyToken(token: String): Boolean {
        return userService.verifyToken(token)
    }

    fun verifyTokenAndGetLanguage(token: String): Pair<Boolean, String> {
        return userService.verifyTokenAndGetLanguage(token)
    }

    fun isUserVerified(userId: Int): Boolean {
        return userService.isUserVerified(userId)
    }

    fun getUserByEmail(email: String): com.adoptu.dto.input.UserDto? {
        return userService.getByEmail(email)
    }

    fun getLanguageByEmail(email: String): String {
        return userService.getByEmail(email)?.language ?: "en"
    }

    fun verifyPassword(userId: Int, encryptedPassword: String): Boolean {
        return passwordService.verifyPassword(userId, encryptedPassword)
    }

    fun requestMagicLink(email: String): Result<Boolean> {
        val language = userService.getByEmail(email)?.language ?: "en"
        return magicLinkService.requestMagicLink(email, language)
    }

    fun requestPasswordReset(email: String): Result<Boolean> {
        val language = userService.getByEmail(email)?.language ?: "en"
        return passwordService.requestPasswordReset(email, language)
    }

    fun resetPassword(token: String, encryptedNewPassword: String): Boolean {
        return passwordService.resetPassword(token, encryptedNewPassword)
    }

    fun verifyAndConsumeMagicLink(token: String): MagicLinkService.MagicLinkResult? {
        return magicLinkService.verifyAndConsumeMagicLink(token)
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