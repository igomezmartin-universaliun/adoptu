package com.adoptu.routes

import com.adoptu.dto.input.CreateUserSterilizationLocationRequest
import com.adoptu.dto.input.UpdateUserSterilizationLocationRequest
import com.adoptu.plugins.respondData
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondUnauthorized
import com.adoptu.services.UserSterilizationLocationService
import com.adoptu.services.auth.SessionUser
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

fun Route.userSterilizationLocationRoutes() {
    val service by inject<UserSterilizationLocationService>()

    route("/api/users") {
        post("/sterilization-location") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondUnauthorized()

            val body = call.receive<CreateUserSterilizationLocationRequest>()
            try {
                val location = service.create(session.userId, body)
                call.respond(location)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            } catch (e: Exception) {
                call.respondError(e.message ?: "Failed to create sterilization location", 500)
            }
        }

        get("/sterilization-location") {
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondUnauthorized()

            val location = service.getByUserId(session.userId)
            if (location == null) {
                call.respondError("Sterilization location not found", 404)
            } else {
                call.respond(location)
            }
        }

        put("/sterilization-location") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondUnauthorized()

            val body = call.receive<UpdateUserSterilizationLocationRequest>()
            call.respondData(service.update(session.userId, body))
        }

        delete("/sterilization-location") {
            val session = call.sessions.get<SessionUser>()
                ?: return@delete call.respondUnauthorized()

            call.respondData(service.delete(session.userId))
        }
    }

    route("/api/user-sterilization-locations") {
        get {
            val country = call.request.queryParameters["country"]
            if (country.isNullOrBlank()) {
                return@get call.respondError("Country is required", 400)
            }
            val state = call.request.queryParameters["state"]
            val city = call.request.queryParameters["city"]
            val zip = call.request.queryParameters["zip"]
            val locations = service.search(country, state, city, zip)
            call.respond(locations)
        }
    }
}