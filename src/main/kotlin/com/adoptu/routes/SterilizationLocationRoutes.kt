package com.adoptu.routes

import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.plugins.respondData
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondSuccess
import com.adoptu.services.ServiceResult
import com.adoptu.services.SterilizationLocationService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.sterilizationLocationRoutes() {
    val service by inject<SterilizationLocationService>()

    route("/api/sterilization-locations") {
        get {
            val country = call.request.queryParameters["country"]
            val state = call.request.queryParameters["state"]
            val city = call.request.queryParameters["city"]
            val locations = service.getAll(country, state, city)
            call.respond(locations)
        }

        get("/grouped") {
            val locations = service.getGroupedByLocation()
            call.respond(locations)
        }

        get("/countries") {
            val countries = service.getCountries()
            call.respond(mapOf("countries" to countries))
        }

        get("/countries/{country}/states") {
            val country = call.parameters["country"] ?: return@get call.respondError("Country is required", 400)
            val states = service.getStatesByCountry(country)
            call.respond(mapOf("states" to states))
        }

        get("/countries/{country}/states/{state}/cities") {
            val country = call.parameters["country"] ?: return@get call.respondError("Country is required", 400)
            val state = call.parameters["state"]
            val cities = service.getCitiesByCountryAndState(country, state)
            call.respond(mapOf("cities" to cities))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondError("Invalid ID")
            val location = service.getById(id)
            if (location != null) {
                call.respond(location)
            } else {
                call.respondError("Sterilization location not found", 404)
            }
        }
    }
}

fun Route.adminSterilizationLocationRoutes() {
    val service by inject<SterilizationLocationService>()

    route("/api/admin/sterilization-locations") {
        get {
            val country = call.request.queryParameters["country"]
            val state = call.request.queryParameters["state"]
            val city = call.request.queryParameters["city"]
            val locations = service.getAll(country, state, city)
            call.respond(locations)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondError("Invalid ID")
            val location = service.getById(id)
            if (location != null) {
                call.respond(location)
            } else {
                call.respondError("Sterilization location not found", 404)
            }
        }

        post {
            val request = call.receive<CreateSterilizationLocationRequest>()
            try {
                val location = service.create(request)
                call.respond(location)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondError("Invalid ID")
            val request = call.receive<UpdateSterilizationLocationRequest>()
            call.respondData(service.update(id, request))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respondError("Invalid ID")
            call.respondSuccess(service.delete(id))
        }
    }
}
