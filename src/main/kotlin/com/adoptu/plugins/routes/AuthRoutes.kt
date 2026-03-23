package com.adoptu.plugins.routes

import com.adoptu.services.auth.SessionUser
import com.adoptu.services.auth.WebAuthnService
import com.adoptu.dto.UserRole
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.respondError
import com.adoptu.services.EmailVerificationService
import com.adoptu.services.PhotographerService
import com.adoptu.services.UserService
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
private data class AuthMeResponse(
    val authenticated: Boolean,
    val id: Int? = null,
    val email: String? = null,
    val displayName: String? = null,
    val language: String = "en",
    val activeRoles: List<String> = emptyList(),
    val lastAcceptedPrivacyPolicy: Long? = null,
    val lastAcceptedTermsAndConditions: Long? = null,
    val photographerFee: Double? = null,
    val photographerCurrency: String? = null,
    val photographerCountry: String? = null,
    val photographerState: String? = null,
    val emailVerified: Boolean = false
)

@Serializable
private data class SuccessWithErrorResponse(val success: Boolean, val error: String? = null, val needsProfileCompletion: Boolean = false)

@Serializable
private data class RegistrationResponse(val success: Boolean, val message: String? = null, val emailVerificationSent: Boolean = false)

@Serializable
private data class VerificationResponse(val success: Boolean, val message: String? = null)

fun Route.authRoutes() {
    val webAuthnService by inject<WebAuthnService>()
    val userService by inject<UserService>()
    val photographerService by inject<PhotographerService>()
    val emailVerificationService by inject<EmailVerificationService>()
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
            val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
            if (!emailRegex.matches(email)) return@post call.respondError("invalid email format")
            val rolesParam = params["roles"] ?: "ADOPTER"
            val roles = rolesParam.split(",").filter { it.isNotBlank() }.map { it.trim() }
            val registrationResponse = params["registrationResponse"]
                ?: return@post call.respondError("registrationResponse required")

            val primaryRole = if (email.equals(adminEmail, ignoreCase = true)) UserRole.ADMIN else (roles.firstOrNull()?.let { UserRole.valueOf(it) } ?: UserRole.ADOPTER)

            val userId = webAuthnService.verifyAndRegister(email, displayName, primaryRole.name, roles, registrationResponse)
            if (userId != null) {
                val needsProfileCompletion = roles.contains("PHOTOGRAPHER") || roles.contains("TEMPORAL_HOME")
                
                val emailSent = emailVerificationService.generateAndSendVerificationEmail(userId, email, displayName)
                if (emailSent) {
                    call.respond(RegistrationResponse(
                        success = true,
                        message = "Registration successful. Please check your email to verify your account.",
                        emailVerificationSent = true
                    ))
                } else {
                    call.respond(RegistrationResponse(
                        success = false,
                        message = "Registration successful but failed to send verification email. Please request a new verification link.",
                        emailVerificationSent = false
                    ))
                }
            } else {
                call.respond(RegistrationResponse(success = false, message = "Registration failed"))
            }
        }

        get("/verify-email") {
            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                return@get call.respond(VerificationResponse(success = false, message = "Token is required"))
            }

            val verified = emailVerificationService.verifyToken(token)
            if (verified) {
                call.respond(VerificationResponse(success = true, message = "Email verified successfully. You can now login."))
            } else {
                call.respond(VerificationResponse(success = false, message = "Invalid or expired token"))
            }
        }

        post("/resend-verification") {
            val session = call.sessions.get<SessionUser>()
            if (session == null) {
                return@post call.respondError("Not authenticated", 401)
            }

            val user = userService.getById(session.userId)
            if (user == null) {
                return@post call.respondError("User not found", 404)
            }

            if (emailVerificationService.isUserVerified(session.userId)) {
                return@post call.respond(VerificationResponse(success = true, message = "Email already verified"))
            }

            val sent = emailVerificationService.resendVerificationEmail(session.userId, user.email ?: session.email, user.displayName)
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
                if (!emailVerificationService.isUserVerified(result.userId)) {
                    call.respond(SuccessWithErrorResponse(success = false, error = "Please verify your email before logging in"))
                    return@post
                }
                val user = result.user
                println("DEBUG /authenticate: success for userId=${result.userId}, username=${user.username}")
                call.sessions.set(
                    SessionUser(result.userId, user.username, user.displayName)
                )
                call.respond(SuccessResponse(success = true))
            } else {
                println("DEBUG /authenticate: failed - invalid credential")
                call.respond(SuccessWithErrorResponse(success = false, error = "Authentication failed"))
            }
        }

        post("/logout") {
            call.sessions.clear<SessionUser>()
            call.respond(SuccessResponse(success = true))
        }

        get("/me") {
            val session = call.sessions.get<SessionUser>()
            println("DEBUG /me: session = ${session?.userId}, ${session?.email}")
            if (session != null) {
                try {
                    val user = userService.getById(session.userId)
                    println("DEBUG /me: user = ${user?.id}")
                    if (user != null) {
                        val activeRolesList = user.activeRoles.map { it.name }
                        val isPhotographer = activeRolesList.contains("PHOTOGRAPHER") || activeRolesList.contains("ADMIN")
                        val photographerData = if (isPhotographer) {
                            photographerService.getPhotographerById(session.userId)
                        } else null
                        
                        call.respond(
                            AuthMeResponse(
                                authenticated = true,
                                id = session.userId,
                                email = session.email,
                                displayName = session.displayName,
                                language = user.language,
                                activeRoles = activeRolesList,
                                lastAcceptedPrivacyPolicy = user.lastAcceptedPrivacyPolicy,
                                lastAcceptedTermsAndConditions = user.lastAcceptedTermsAndConditions,
                                photographerFee = photographerData?.photographerFee,
                                photographerCurrency = photographerData?.photographerCurrency,
                                photographerCountry = photographerData?.country,
                                photographerState = photographerData?.state,
                                emailVerified = emailVerificationService.isUserVerified(session.userId)
                            )
                        )
                    } else {
                        println("DEBUG: Session exists but user not found for userId: ${session.userId}")
                        call.respond(AuthMeResponse(authenticated = false))
                    }
                } catch (e: Exception) {
                    println("DEBUG: Exception in /me endpoint: ${e.message}")
                    e.printStackTrace()
                    call.respond(AuthMeResponse(authenticated = false))
                }
            } else {
                call.respond(AuthMeResponse(authenticated = false))
            }
        }
    }
}
