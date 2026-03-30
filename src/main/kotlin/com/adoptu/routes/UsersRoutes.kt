package com.adoptu.routes

import com.adoptu.dto.input.AcceptTermsRequest
import com.adoptu.dto.input.BanUserRequest
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.RoleActivationRequest
import com.adoptu.dto.input.UserRole
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondForbidden
import com.adoptu.plugins.respondInvalidId
import com.adoptu.plugins.respondNotFound
import com.adoptu.plugins.respondUnauthorized
import com.adoptu.services.PhotographerService
import com.adoptu.services.validation.ValidationConstants
import com.adoptu.services.UserService
import com.adoptu.services.auth.SessionUser
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class UpdateProfileRequest(val displayName: String)

@Serializable
data class UpdateLanguageRequest(val language: String)

fun Route.usersRoutes() {
    val userService by inject<UserService>()
    val photographerService by inject<PhotographerService>()

    route("/api/users") {
        post("/accept-terms") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()

            val body = call.receive<AcceptTermsRequest>()
            val user = userService.acceptTerms(session.userId, body)
                ?: return@post call.respondNotFound()

            call.respond(user)
        }

        put("/profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondUnauthorized()

            val body = call.receive<UpdateProfileRequest>()
            val language = call.request.queryParameters["language"]
            try {
                val user = userService.updateProfile(session.userId, body.displayName, language)
                    ?: return@put call.respondNotFound(ValidationConstants.USER_NOT_FOUND)
                call.respond(user)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        put("/language") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondUnauthorized()

            val body = call.receive<UpdateLanguageRequest>()
            try {
                val user = userService.updateLanguage(session.userId, body.language)
                    ?: return@put call.respondNotFound(ValidationConstants.USER_NOT_FOUND)
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
                ?: return@post call.respondUnauthorized()

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                userService.activateRescuerProfile(session.userId)
            } else {
                userService.deactivateRescuerProfile(session.userId)
            }
                ?: return@post call.respondNotFound()

            call.respond(user)
        }

        post("/temporal-home-profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                userService.activateTemporalHomeProfile(session.userId)
            } else {
                userService.deactivateTemporalHomeProfile(session.userId)
            }
                ?: return@post call.respondNotFound()

            call.respond(user)
        }

        put("/photographer-settings") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondUnauthorized()

            val body = call.receive<PhotographerSettingsRequest>()
            val photographer = photographerService.updatePhotographerSettings(session.userId, body)
                ?: return@put call.respondNotFound()

            call.respond(photographer)
        }

        post("/photographer-profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                photographerService.activatePhotographerProfile(session.userId)
            } else {
                photographerService.deactivatePhotographerProfile(session.userId)
            }
                ?: return@post call.respondNotFound()

            call.respond(user)
        }
    }
}

fun Route.adminUsersRoutes() {
    val userService by inject<UserService>()

    route("/api/admin/users") {
        get {
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondUnauthorized()
            
            val user = userService.getById(session.userId)
            if (user == null || !user.activeRoles.contains(UserRole.ADMIN)) {
                return@get call.respondForbidden()
            }
            
            val users = userService.getAllUsers()
            call.respond(users)
        }

        get("/{id}") {
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondUnauthorized()
            
            val admin = userService.getById(session.userId)
            if (admin == null || !admin.activeRoles.contains(UserRole.ADMIN)) {
                return@get call.respondForbidden()
            }
            
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondInvalidId(ValidationConstants.INVALID_ID)
            val user = userService.getById(id) ?: return@get call.respondNotFound()
            call.respond(user)
        }

        post("/{id}/ban") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()
            
            val admin = userService.getById(session.userId)
            if (admin == null || !admin.activeRoles.contains(UserRole.ADMIN)) {
                return@post call.respondForbidden()
            }
            
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondInvalidId(ValidationConstants.INVALID_ID)
            val body = call.receive<BanUserRequest>()
            
            if (id == session.userId) {
                return@post call.respondError("Cannot ban yourself", 400)
            }
            
            val targetUser = userService.getById(id)
            if (targetUser == null) {
                return@post call.respondNotFound()
            }
            if (targetUser.activeRoles.contains(UserRole.ADMIN)) {
                return@post call.respondError("Cannot ban an admin", 400)
            }
            
            val banned = userService.banUser(id, body.reason)
            if (banned) {
                call.respond(SuccessResponse(success = true))
            } else {
                call.respondError("Failed to ban user", 500)
            }
        }

        post("/{id}/unban") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()
            
            val admin = userService.getById(session.userId)
            if (admin == null || !admin.activeRoles.contains(UserRole.ADMIN)) {
                return@post call.respondForbidden()
            }
            
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondInvalidId(ValidationConstants.INVALID_ID)
            
            val unbanned = userService.unbanUser(id)
            if (unbanned) {
                call.respond(SuccessResponse(success = true))
            } else {
                call.respondError("Failed to unban user", 500)
            }
        }
    }
}
