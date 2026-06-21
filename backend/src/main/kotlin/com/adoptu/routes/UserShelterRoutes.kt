package com.adoptu.routes

import com.adoptu.dto.input.CreateUserShelterRequest
import com.adoptu.dto.input.UpdateUserShelterRequest
import com.adoptu.plugins.respondData
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondUnauthorized
import com.adoptu.services.ServiceResult
import com.adoptu.services.UserShelterService
import com.adoptu.services.auth.SessionUser
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

fun Route.userShelterRoutes() {
    val service by inject<UserShelterService>()

    route("/api/users") {
        post("/shelter") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()

            val body = call.receive<CreateUserShelterRequest>()
            try {
                val shelter = service.create(session.userId, body)
                call.respond(shelter)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            } catch (e: Exception) {
                call.respondError(e.message ?: "Failed to create shelter", 500)
            }
        }

        get("/shelter") {
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondUnauthorized()

            val shelter = service.getByUserId(session.userId)
            if (shelter == null) {
                call.respondError("Shelter profile not found", 404)
            } else {
                call.respond(shelter)
            }
        }

        put("/shelter") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondUnauthorized()

            val body = call.receive<UpdateUserShelterRequest>()
            call.respondData(service.update(session.userId, body))
        }

        delete("/shelter") {
            val session = call.sessions.get<SessionUser>()
                ?: return@delete call.respondUnauthorized()

            call.respondData(service.delete(session.userId))
        }
    }

    route("/api/user-shelters") {
        get {
            val country = call.request.queryParameters["country"]
            if (country.isNullOrBlank()) {
                return@get call.respondError("Country is required", 400)
            }
            val state = call.request.queryParameters["state"]
            val city = call.request.queryParameters["city"]
            val zip = call.request.queryParameters["zip"]
            val shelters = service.search(country, state, city, zip)
            call.respond(shelters)
        }
    }
}