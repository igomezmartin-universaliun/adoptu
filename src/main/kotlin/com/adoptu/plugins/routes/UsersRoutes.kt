package com.adoptu.plugins.routes

import com.adoptu.services.auth.SessionUser
import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.RoleActivationRequest
import com.adoptu.plugins.respondError
import com.adoptu.services.UserService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(val displayName: String)

@Serializable
data class UpdateLanguageRequest(val language: String)

fun Route.usersRoutes() {
    val userService by inject<UserService>()

    route("/api/users") {
        post("/accept-terms") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val body = call.receive<AcceptTermsRequest>()
            val user = userService.acceptTerms(session.userId, body)
                ?: return@post call.respondError("User not found", 404)

            call.respond(user)
        }

        put("/profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondError("Unauthorized", 401)

            val body = call.receive<UpdateProfileRequest>()
            val language = call.request.queryParameters["language"]
            try {
                val user = userService.updateProfile(session.userId, body.displayName, language)
                    ?: return@put call.respondError("User not found", 404)
                call.respond(user)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        put("/language") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondError("Unauthorized", 401)

            val body = call.receive<UpdateLanguageRequest>()
            try {
                val user = userService.updateLanguage(session.userId, body.language)
                    ?: return@put call.respondError("User not found", 404)
                call.respond(user)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        get("/rescuers") {
            val rescuers = userService.getRescuers()
            call.respond(rescuers)
        }

        post("/rescuer-profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                userService.activateRescuerProfile(session.userId)
            } else {
                userService.deactivateRescuerProfile(session.userId)
            }
                ?: return@post call.respondError("User not found", 404)

            call.respond(user)
        }

        post("/temporal-home-profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                userService.activateTemporalHomeProfile(session.userId)
            } else {
                userService.deactivateTemporalHomeProfile(session.userId)
            }
                ?: return@post call.respondError("User not found", 404)

            call.respond(user)
        }
    }
}
