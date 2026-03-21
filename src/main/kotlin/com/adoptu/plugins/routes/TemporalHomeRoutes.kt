package com.adoptu.plugins.routes

import com.adoptu.auth.SessionUser
import com.adoptu.dto.BlockRescuerRequest
import com.adoptu.dto.CreateTemporalHomeRequest
import com.adoptu.dto.SendTemporalHomeRequestRequest
import com.adoptu.dto.TemporalHomeSearchParams
import com.adoptu.dto.UserRole
import com.adoptu.plugins.respondError
import com.adoptu.services.TemporalHomeService
import com.adoptu.services.UserService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

fun Route.temporalHomeRoutes() {
    val temporalHomeService by inject<TemporalHomeService>()
    val userService by inject<UserService>()

    route("/api/users") {
        post("/temporal-home") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val existing = temporalHomeService.getTemporalHome(session.userId)
            if (existing != null) {
                return@post call.respondError("Temporal home profile already exists", 400)
            }

            val body = call.receive<CreateTemporalHomeRequest>()
            
            if (body.alias.isBlank()) {
                return@post call.respondError("Alias is required", 400)
            }
            if (body.country.isBlank()) {
                return@post call.respondError("Country is required", 400)
            }
            if (body.city.isBlank()) {
                return@post call.respondError("City is required", 400)
            }

            try {
                val temporalHome = temporalHomeService.createTemporalHome(session.userId, body)
                userService.activateTemporalHomeProfile(session.userId)
                call.respond(temporalHome)
            } catch (e: Exception) {
                call.respondError(e.message ?: "Failed to create temporal home", 500)
            }
        }

        get("/temporal-home") {
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondError("Unauthorized", 401)

            val temporalHome = temporalHomeService.getTemporalHome(session.userId)
            if (temporalHome == null) {
                return@get call.respondError("Temporal home profile not found", 404)
            }
            call.respond(temporalHome)
        }

        get("/temporal-home/requests") {
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondError("Unauthorized", 401)
            val user = userService.getById(session.userId) ?: return@get call.respondError("User not found", 404)
            val activeRoles = user.activeRoles.map { it.name }
            if (!activeRoles.contains("TEMPORAL_HOME") && !activeRoles.contains("ADMIN")) {
                return@get call.respondError("Forbidden", 403)
            }

            val requests = temporalHomeService.getMyRequests(session.userId)
            call.respond(requests)
        }
    }

    route("/api/temporal-homes") {
        get {
            val country = call.parameters["country"]
            val state = call.parameters["state"]
            val city = call.parameters["city"]
            val zip = call.parameters["zip"]
            val neighborhood = call.parameters["neighborhood"]

            val params = TemporalHomeSearchParams(
                country = country,
                state = state,
                city = city,
                zip = zip,
                neighborhood = neighborhood
            )

            val results = temporalHomeService.searchTemporalHomes(params)
            call.respond(results)
        }

        post("/request") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)
            val user = userService.getById(session.userId) ?: return@post call.respondError("User not found", 404)
            val activeRoles = user.activeRoles.map { it.name }
            if (!activeRoles.contains("RESCUER") && !activeRoles.contains("ADMIN")) {
                return@post call.respondError("Only rescuers can send requests", 403)
            }

            val body = call.receive<SendTemporalHomeRequestRequest>()

            if (body.message.isBlank()) {
                return@post call.respondError("Message is required", 400)
            }

            val result = temporalHomeService.sendRequest(session.userId, body)
            if (result.isFailure) {
                return@post call.respondError(result.exceptionOrNull()?.message ?: "Failed to send request", 400)
            }
            call.respond(mapOf("success" to true, "requestId" to result.getOrNull()))
        }
    }

    route("/api/temporal-homes") {
        get("/block/{temporalHomeId}") {
            val temporalHomeId = call.parameters["temporalHomeId"]?.toIntOrNull()
                ?: return@get call.respondError("Invalid temporal home ID", 400)
            
            val rescuerId = call.parameters["rescuer"]?.toIntOrNull()
                ?: return@get call.respondError("Invalid rescuer ID", 400)

            val blocked = temporalHomeService.blockRescuer(temporalHomeId, rescuerId)
            call.respond(mapOf("blocked" to blocked))
        }

        post("/block") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)
            val user = userService.getById(session.userId) ?: return@post call.respondError("User not found", 404)
            val activeRoles = user.activeRoles.map { it.name }
            if (!activeRoles.contains("TEMPORAL_HOME") && !activeRoles.contains("ADMIN")) {
                return@post call.respondError("Forbidden", 403)
            }

            val body = call.receive<BlockRescuerRequest>()

            val blocked = temporalHomeService.blockRescuer(session.userId, body.rescuerId)
            call.respond(mapOf("blocked" to blocked))
        }
    }
}