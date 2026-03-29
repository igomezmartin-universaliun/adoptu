package com.adoptu.routes

import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.UpdateShelterRequest
import com.adoptu.plugins.respondData
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondSuccess
import com.adoptu.services.ServiceResult
import com.adoptu.services.ShelterService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shelterRoutes() {
    val shelterService by inject<ShelterService>()

    route("/api/shelters") {
        get {
            val country = call.request.queryParameters["country"]
            if (country.isNullOrBlank()) {
                return@get call.respondError("Country is required", 400)
            }
            val state = call.request.queryParameters["state"]
            val shelters = shelterService.getAll(country, state)
            call.respond(shelters)
        }

        get("/countries") {
            val countries = shelterService.getCountries()
            call.respond(mapOf("countries" to countries))
        }

        get("/countries/{country}/states") {
            val country = call.parameters["country"] ?: return@get call.respondError("Country is required", 400)
            val states = shelterService.getStatesByCountry(country)
            call.respond(mapOf("states" to states))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondError("Invalid ID")
            val shelter = shelterService.getById(id)
            if (shelter != null) {
                call.respond(shelter)
            } else {
                call.respondError("Shelter not found", 404)
            }
        }
    }
}

fun Route.adminShelterRoutes() {
    val shelterService by inject<ShelterService>()

    route("/api/admin/shelters") {
        get {
            val country = call.request.queryParameters["country"]
            val state = call.request.queryParameters["state"]
            val shelters = if (country.isNullOrBlank()) {
                emptyList()
            } else {
                shelterService.getAll(country, state)
            }
            call.respond(shelters)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondError("Invalid ID")
            val shelter = shelterService.getById(id)
            if (shelter != null) {
                call.respond(shelter)
            } else {
                call.respondError("Shelter not found", 404)
            }
        }

        post {
            val request = call.receive<CreateShelterRequest>()
            try {
                val shelter = shelterService.create(request)
                call.respond(shelter)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondError("Invalid ID")
            val request = call.receive<UpdateShelterRequest>()
            call.respondData(shelterService.update(id, request))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respondError("Invalid ID")
            call.respondSuccess(shelterService.delete(id))
        }
    }
}
