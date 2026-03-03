package com.adoptu.plugins.routes

import com.adoptu.auth.SessionUser
import com.adoptu.auth.WebAuthnService
import com.adoptu.dto.UserRole
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.respondError
import com.adoptu.services.UserService
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
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val lastAcceptedPrivacyPolicy: Long? = null,
    val lastAcceptedTermsAndConditions: Long? = null
)

@Serializable
private data class SuccessWithErrorResponse(val success: Boolean, val error: String? = null)

fun Route.authRoutes() {
    val webAuthnService by inject<WebAuthnService>()
    val userService by inject<UserService>()

    route("/api/auth") {
        post("/registration-options") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respondError("username required")
            val displayName = params["displayName"] ?: return@post call.respondError("displayName required")
            val options = webAuthnService.generateRegistrationOptions(username, displayName)
            call.respond(options)
        }

        post("/register") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respondError("username required")
            val displayName = params["displayName"] ?: return@post call.respondError("displayName required")
            val role = params["role"] ?: "ADOPTER"
            val registrationResponse = params["registrationResponse"]
                ?: return@post call.respondError("registrationResponse required")

            val userId = webAuthnService.verifyAndRegister(username, displayName, role, registrationResponse)
            if (userId != null) {
                call.sessions.set(SessionUser(userId, username, displayName, null, UserRole.valueOf(role)))
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
                call.sessions.set(
                    SessionUser(result.userId, user.username, user.displayName, user.email, UserRole.valueOf(user.role))
                )
                call.respond(SuccessResponse(success = true))
            } else {
                call.respond(SuccessWithErrorResponse(success = false, error = "Authentication failed"))
            }
        }

        post("/logout") {
            call.sessions.clear<SessionUser>()
            call.respond(SuccessResponse(success = true))
        }

        get("/me") {
            val session = call.sessions.get<SessionUser>()
            if (session != null) {
                val user = userService.getById(session.userId)
                if (user != null) {
                    call.respond(
                        AuthMeResponse(
                            authenticated = true,
                            id = session.userId,
                            username = session.username,
                            displayName = session.displayName,
                            email = session.email,
                            role = session.role.name,
                            lastAcceptedPrivacyPolicy = user.lastAcceptedPrivacyPolicy,
                            lastAcceptedTermsAndConditions = user.lastAcceptedTermsAndConditions
                        )
                    )
                } else {
                    call.respond(AuthMeResponse(authenticated = false))
                }
            } else {
                call.respond(AuthMeResponse(authenticated = false))
            }
        }
    }
}
