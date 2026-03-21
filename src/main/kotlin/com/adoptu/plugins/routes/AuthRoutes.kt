package com.adoptu.plugins.routes

import com.adoptu.auth.SessionUser
import com.adoptu.auth.WebAuthnService
import com.adoptu.dto.UserRole
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.respondError
import com.adoptu.services.UserService
import io.ktor.server.application.*
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
    val photographerCurrency: String? = null
)

@Serializable
private data class SuccessWithErrorResponse(val success: Boolean, val error: String? = null)

fun Route.authRoutes() {
    val webAuthnService by inject<WebAuthnService>()
    val userService by inject<UserService>()
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
                call.sessions.set(SessionUser(userId, email, displayName))
                call.respond(SuccessResponse(success = true))
            } else {
                call.respond(SuccessWithErrorResponse(success = false, error = "Registration failed"))
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
                            userService.getPhotographerById(session.userId)
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
                                photographerCurrency = photographerData?.photographerCurrency
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
