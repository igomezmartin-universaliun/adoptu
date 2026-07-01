package com.adoptu.routes

import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.UpdateShelterRequest
import com.adoptu.plugins.respondData
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondSuccess
import com.adoptu.services.ServiceResult
import com.adoptu.services.ShelterService
import com.adoptu.services.validation.ValidationConstants
import io.ktor.http.HttpHeaders
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
                return@get call.respondError(ValidationConstants.COUNTRY_IS_REQUIRED, 400)
            }
            val state = call.request.queryParameters["state"]
            val city = call.request.queryParameters["city"]
            val neighborhood = call.request.queryParameters["neighborhood"]
            val zip = call.request.queryParameters["zip"]
            val shelters = shelterService.getAll(country, state, city, neighborhood, zip)
            // Public, unauthenticated listing - cached at the CDN edge (see
            // infra/cloudfront.tf: ordered_cache_behavior for "/api/shelters*").
            // The admin variant lives under the separate /api/admin/shelters
            // prefix, so this wildcard never touches an authenticated route.
            call.response.header(HttpHeaders.CacheControl, "public, max-age=30")
            call.respond(shelters)
        }

        get("/countries") {
            val countries = shelterService.getCountries()
            call.respond(mapOf("countries" to countries))
        }

        get("/countries/{country}/states") {
            val country = call.parameters["country"] ?: return@get call.respondError(ValidationConstants.COUNTRY_IS_REQUIRED, 400)
            val states = shelterService.getStatesByCountry(country)
            call.respond(mapOf("states" to states))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondError(ValidationConstants.INVALID_ID)
            val shelter = shelterService.getById(id)
            if (shelter != null) {
                call.respond(shelter)
            } else {
                call.respondError(ValidationConstants.SHELTER_NOT_FOUND, 404)
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
            val city = call.request.queryParameters["city"]
            val neighborhood = call.request.queryParameters["neighborhood"]
            val zip = call.request.queryParameters["zip"]
            val shelters = if (country.isNullOrBlank()) {
                emptyList()
            } else {
                shelterService.getAll(country, state, city, neighborhood, zip)
            }
            call.respond(shelters)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondError(ValidationConstants.INVALID_ID)
            val shelter = shelterService.getById(id)
            if (shelter != null) {
                call.respond(shelter)
            } else {
                call.respondError(ValidationConstants.SHELTER_NOT_FOUND, 404)
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
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondError(ValidationConstants.INVALID_ID)
            val request = call.receive<UpdateShelterRequest>()
            call.respondData(shelterService.update(id, request))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respondError(ValidationConstants.INVALID_ID)
            call.respondSuccess(shelterService.delete(id))
        }
    }
}
