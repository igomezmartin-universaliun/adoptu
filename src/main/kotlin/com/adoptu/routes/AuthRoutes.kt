package com.adoptu.routes

import com.adoptu.dto.input.UserDto
import com.adoptu.dto.input.UserRole
import com.adoptu.dto.output.AuthMeResponse
import com.adoptu.dto.output.RegistrationResponse
import com.adoptu.dto.output.SuccessWithErrorResponse
import com.adoptu.dto.output.VerificationResponse
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.respondError
import com.adoptu.services.ServiceResult
import com.adoptu.services.auth.SessionUser
import com.adoptu.services.auth.WebAuthnService
import com.adoptu.services.validation.AuthValidationService
import com.adoptu.services.crypto.CryptoService
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class EncryptedLoginRequest(
    val encryptedData: String
)

@Serializable
data class PasswordLoginRequest(
    val email: String,
    val encryptedPassword: String
)

fun Route.authRoutes() {
    val webAuthnService by inject<WebAuthnService>()
    val validationService by inject<AuthValidationService>()
    val config by inject<ApplicationConfig>()
    val adminEmail = config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com"

    route("/api/auth") {
        post("/registration-options") {
            val params = call.receiveParameters()
            val email = params["email"] ?: return@post call.respondError("email required")
            val displayName = params["displayName"] ?: return@post call.respondError("displayName required")
            val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
            if (!emailRegex.matches(email)) return@post call.respondError("invalid email format")
            val options = webAuthnService.generateRegistrationOptions(email, displayName)
            call.respond(options)
        }

        post("/register") {
            val params = call.receiveParameters()
            val email = params["email"] ?: return@post call.respondError("email required")
            val displayName = params["displayName"] ?: return@post call.respondError("displayName required")
            val language = params["language"] ?: "en"
            val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
            if (!emailRegex.matches(email)) return@post call.respondError("invalid email format")

            val registrationResponse = params["registrationResponse"]
                ?: return@post call.respondError("registrationResponse required")
            
            val roles = params["roles"] ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { it.trim() }
                ?.map { UserRole.valueOf(it) }
                ?.toSet()
                ?: setOf(UserRole.ADOPTER)

            val effectiveRoles = if (email.equals(adminEmail, ignoreCase = true)) {
                roles + UserRole.ADMIN
            } else {
                roles
            }

            val result = webAuthnService.verifyAndRegister(email, displayName, effectiveRoles, registrationResponse, language)
            processResult(result)
        }

        get("/verify-email") {
            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                return@get call.respond(VerificationResponse(success = false, message = "Token is required"))
            }

            if (webAuthnService.verifyToken(token)) {
                call.respond(VerificationResponse(success = true, message = "Email verified successfully. You can now login."))
            } else {
                call.respond(VerificationResponse(success = false, message = "Invalid or expired token"))
            }
        }

        post("/resend-verification") {
            val session = call.sessions.get<SessionUser>()
            if (session == null) {
                val contentType = call.request.contentType().toString()
                if (contentType.contains("application/x-www-form-urlencoded")) {
                    val params = call.receiveParameters()
                    val email = params["email"]
                    if (!email.isNullOrBlank()) {
                        val sent = webAuthnService.resendVerificationEmailByEmail(email)
                        if (sent) {
                            call.respond(VerificationResponse(success = true, message = "Verification email sent"))
                        } else {
                            call.respond(VerificationResponse(success = false, message = "Failed to send verification email"))
                        }
                        return@post
                    }
                }
                return@post call.respondError("Not authenticated", 401)
            }
            
            val sent = webAuthnService.resendVerificationEmail(session.userId)
            if (sent) {
                call.respond(VerificationResponse(success = true, message = "Verification email sent"))
            } else {
                call.respond(VerificationResponse(success = false, message = "Failed to send verification email"))
            }
        }

        get("/assertion-options") {
            val options = webAuthnService.generateAssertionOptions()
            call.respond(options)
        }

        post("/authenticate") {
            val body = call.receiveText()
            if (body.isBlank()) return@post call.respond(SuccessWithErrorResponse(success = false, error = "No credential"))

            val result = webAuthnService.verifyAndAuthenticate(body)
            if (result != null) {
                val verifiedResult = validationService.validateVerified(result.userId, result.user.username)
                if (verifiedResult is ServiceResult.Error) {
                    call.respond(SuccessWithErrorResponse(success = false, error = "Please verify your email before logging in", email = verifiedResult.message))
                    return@post
                }

                val bannedResult = validationService.validateNotBanned(result.userId)
                if (bannedResult is ServiceResult.Error) {
                    call.respond(SuccessWithErrorResponse(success = false, error = bannedResult.message, email = result.user.username))
                    return@post
                }

                val user = result.user
                application.log.debug("Authenticate: success for userId=${result.userId}, username=${user.username}")
                call.sessions.set(
                    SessionUser(result.userId, user.username, user.displayName)
                )
                call.respond(SuccessResponse(success = true))
            } else {
                application.log.debug("Authenticate: failed - invalid credential")
                call.respond(SuccessWithErrorResponse(success = false, error = "Authentication failed"))
            }
        }

        post("/logout") {
            call.sessions.clear<SessionUser>()
            call.respond(SuccessResponse(success = true))
        }

        get("/me") {
            val session = call.sessions.get<SessionUser>()
            application.log.debug("Session = ${session?.userId}, ${session?.email}")
            if (session != null) {
                try {
                    val userResult = validationService.validateUserById(session.userId)
                    when (userResult) {
                        is ServiceResult.Success -> {
                            userAuthenticationSuccess(userResult, this@route, this@get, session, webAuthnService)
                        }
                        is ServiceResult.NotFound -> {
                            application.log.warn("Session exists but user not found for userId: ${session.userId}")
                            call.respond(AuthMeResponse(authenticated = false))
                        }
                        else -> {
                            call.respond(AuthMeResponse(authenticated = false))
                        }
                    }
                } catch (e: Exception) {
                    application.log.error("Exception in /me endpoint: ${e.message}", e)
                    call.respond(AuthMeResponse(authenticated = false))
                }
            } else {
                call.respond(AuthMeResponse(authenticated = false))
            }
        }

        post("/request-magic-link") {
            val body = try {
                call.receive<EncryptedLoginRequest>()
            } catch (e: Exception) {
                return@post call.respondError("Invalid request body", 400)
            }
            
            val emailResult = validationService.validateAndDecryptEmail(body.encryptedData)
            if (emailResult is ServiceResult.Error) {
                return@post call.respondError(emailResult.message, 400)
            }
            val email = (emailResult as ServiceResult.Success).data
            
            val result = webAuthnService.requestMagicLink(email)
            if (result.isFailure) {
                call.respond(SuccessWithErrorResponse(success = false, error = result.exceptionOrNull()?.message ?: "Failed to send magic link"))
            } else {
                call.respond(SuccessResponse(success = true))
            }
        }

        get("/magic-link-login") {
            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                return@get call.respond(VerificationResponse(success = false, message = "Token is required"))
            }
            
            val result = webAuthnService.verifyAndConsumeMagicLink(token)
            if (result == null) {
                call.respond(VerificationResponse(success = false, message = "Invalid or expired magic link"))
                return@get
            }
            
            val verifiedResult = validationService.validateVerified(result.userId, result.username)
            if (verifiedResult is ServiceResult.Error) {
                call.respond(SuccessWithErrorResponse(success = false, error = "Please verify your email before logging in", email = result.username))
                return@get
            }
            
            val bannedResult = validationService.validateNotBanned(result.userId)
            if (bannedResult is ServiceResult.Error) {
                call.respond(SuccessWithErrorResponse(success = false, error = bannedResult.message, email = result.username))
                return@get
            }
            
            application.log.debug("Magic link login: success for userId=${result.userId}, username=${result.username}")
            call.sessions.set(SessionUser(result.userId, result.username, result.displayName))
            call.respond(SuccessResponse(success = true))
        }

        post("/login-with-password") {
            val body = try {
                call.receive<PasswordLoginRequest>()
            } catch (e: Exception) {
                return@post call.respondError("Invalid request body", 400)
            }
            
            val userResult = validationService.validateEmailAndUser(body.email)
            if (userResult is ServiceResult.Error) {
                call.respond(SuccessWithErrorResponse(success = false, error = "Invalid credentials"))
                return@post
            }
            val user = (userResult as ServiceResult.Success).data
            
            if (!webAuthnService.verifyPassword(user.id, body.encryptedPassword)) {
                call.respond(SuccessWithErrorResponse(success = false, error = "Invalid credentials"))
                return@post
            }
            
            val verifiedResult = validationService.validateVerified(user.id, body.email)
            if (verifiedResult is ServiceResult.Error) {
                call.respond(SuccessWithErrorResponse(success = false, error = "Please verify your email before logging in", email = body.email))
                return@post
            }
            
            val bannedResult = validationService.validateNotBanned(user.id)
            if (bannedResult is ServiceResult.Error) {
                call.respond(SuccessWithErrorResponse(success = false, error = bannedResult.message, email = body.email))
                return@post
            }
            
            application.log.debug("Password login: success for userId=${user.id}, username=${body.email}")
            call.sessions.set(SessionUser(user.id, body.email, user.displayName))
            call.respond(SuccessResponse(success = true))
        }

        post("/forgot-password") {
            val body = try {
                call.receive<EncryptedLoginRequest>()
            } catch (e: Exception) {
                return@post call.respondError("Invalid request body", 400)
            }
            
            val emailResult = validationService.validateAndDecryptEmail(body.encryptedData)
            if (emailResult is ServiceResult.Error) {
                return@post call.respondError(emailResult.message, 400)
            }
            val result = webAuthnService.requestPasswordReset((emailResult as ServiceResult.Success).data)
            if (result.isFailure) {
                call.respond(SuccessWithErrorResponse(success = false, error = result.exceptionOrNull()?.message ?: "Failed to send reset email"))
            } else {
                call.respond(SuccessResponse(success = true))
            }
        }

        post("/reset-password") {
            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                return@post call.respondError("Token is required", 400)
            }
            
            val body = try {
                call.receive<EncryptedLoginRequest>()
            } catch (e: Exception) {
                return@post call.respondError("Invalid request body", 400)
            }
            
            val success = webAuthnService.resetPassword(token, body.encryptedData)
            if (success) {
                call.respond(SuccessResponse(success = true))
            } else {
                call.respond(SuccessWithErrorResponse(success = false, error = "Failed to reset password. Token may be invalid/expired or password doesn't meet requirements (min 8 chars with uppercase, lowercase, number, symbol)."))
            }
        }

        get("/encryption-key") {
            val publicKey = CryptoService.getPublicKey()
            call.respond(mapOf("publicKey" to publicKey))
        }
    }
}

private suspend fun userAuthenticationSuccess(
    userResult: ServiceResult.Success<UserDto>,
    route: Route,
    context: RoutingContext,
    session: SessionUser,
    webAuthnService: WebAuthnService,
) {
    val user = userResult.data
    route.application.log.debug("User = ${user.id}")
    val activeRolesList = user.activeRoles.map { it.name }

    context.call.respond(
        AuthMeResponse(
            authenticated = true,
            id = session.userId,
            email = session.email,
            displayName = session.displayName,
            language = user.language,
            activeRoles = activeRolesList,
            lastAcceptedPrivacyPolicy = user.lastAcceptedPrivacyPolicy,
            lastAcceptedTermsAndConditions = user.lastAcceptedTermsAndConditions,
            emailVerified = webAuthnService.isUserVerified(session.userId),
            isBanned = user.isBanned,
            banReason = user.banReason,
            photographerFee = user.photographerFee,
            photographerCurrency = user.photographerCurrency,
            photographerCountry = user.photographerCountry,
            photographerState = user.photographerState
        )
    )
}

private suspend fun RoutingContext.processResult(result: WebAuthnService.RegistrationResult?) {
    if (result != null) {
        if (result.emailSent) {
            call.respond(
                RegistrationResponse(
                    success = true,
                    message = "Registration successful. Please check your email to verify your account.",
                    emailVerificationSent = true
                )
            )
        } else {
            call.respond(
                RegistrationResponse(
                    success = false,
                    message = "Registration successful but failed to send verification email. Please request a new verification link.",
                    emailVerificationSent = false
                )
            )
        }
    } else {
        call.respond(RegistrationResponse(success = false, message = "Registration failed"))
    }
}
