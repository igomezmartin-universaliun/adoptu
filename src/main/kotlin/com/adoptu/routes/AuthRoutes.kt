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
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

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
            banReason = user.banReason
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
