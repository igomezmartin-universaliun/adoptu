package com.adoptu.routes

import com.adoptu.dto.input.CreateMultiPhotographerRequestRequest
import com.adoptu.dto.input.CreatePhotographyRequestRequest
import com.adoptu.dto.input.PhotographerSettingsRequest
import com.adoptu.dto.input.RoleActivationRequest
import com.adoptu.dto.input.UpdatePhotographyRequestRequest
import com.adoptu.dto.input.UserDto
import com.adoptu.plugins.respondData
import com.adoptu.plugins.respondError
import com.adoptu.plugins.respondForbidden
import com.adoptu.plugins.respondNotFound
import com.adoptu.plugins.respondUnauthorized
import com.adoptu.services.PhotographerService
import com.adoptu.services.ServiceResult
import com.adoptu.services.UserService
import com.adoptu.services.validation.PhotographersValidationService
import com.adoptu.services.auth.SessionUser
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

fun Route.photographerRoutes() {
    val photographerService by inject<PhotographerService>()
    val validationService by inject<PhotographersValidationService>()

    suspend fun RoutingContext.validateUser(): ServiceResult<UserDto> {
        val sessionResult = validationService.validateSession(call.sessions.get<SessionUser>())
        if (sessionResult is ServiceResult.Forbidden) {
            return ServiceResult.Forbidden
        }
        val session = (sessionResult as ServiceResult.Success).data
        return validationService.validateUserById(session.userId)
    }

    route("/api/photographers") {
        get {
            val country = call.parameters["country"]
            val state = call.parameters["state"]
            val photographers = photographerService.getPhotographers(country, state)
            call.respond(photographers)
        }

        post("/profile") {
            val sessionResult = validationService.validateSession(call.sessions.get<SessionUser>())
            if (sessionResult is ServiceResult.Forbidden) {
                return@post call.respondUnauthorized()
            }
            val session = (sessionResult as ServiceResult.Success).data

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                photographerService.activatePhotographerProfile(session.userId)
            } else {
                photographerService.deactivatePhotographerProfile(session.userId)
            }
            val userResult = validationService.validateUser(user)
            if (userResult is ServiceResult.NotFound) {
                return@post call.respondNotFound()
            }

            call.respond(user!!)
        }

        put("/settings") {
            val userResult = validateUser()
            when (userResult) {
                is ServiceResult.Forbidden -> return@put call.respondUnauthorized()
                is ServiceResult.NotFound -> return@put call.respondNotFound()
                else -> {}
            }
            val user = (userResult as ServiceResult.Success).data
            val session = (validationService.validateSession(call.sessions.get<SessionUser>()) as ServiceResult.Success).data

            val roleResult = validationService.validateRole(user, "PHOTOGRAPHER")
            if (roleResult is ServiceResult.Forbidden) {
                return@put call.respondForbidden()
            }

            val body = call.receive<PhotographerSettingsRequest>()
            val feeResult = validationService.validatePhotographerFee(body.photographerFee)
            if (feeResult is ServiceResult.Error) {
                return@put call.respondError(feeResult.message, 400)
            }

            try {
                val photographer = photographerService.updatePhotographerSettings(session.userId, body)
                if (photographer == null) {
                    return@put call.respondNotFound()
                }
                call.respond(photographer)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        post("/requests") {
            val sessionResult = validationService.validateSession(call.sessions.get<SessionUser>())
            if (sessionResult is ServiceResult.Forbidden) {
                return@post call.respondUnauthorized()
            }
            val session = (sessionResult as ServiceResult.Success).data

            val body = call.receive<CreatePhotographyRequestRequest>()

            val result = photographerService.createPhotographyRequest(
                requesterId = session.userId,
                photographerId = body.photographerId,
                petId = body.petId,
                message = body.message
            )

            call.respond(result)
        }

        post("/requests/multiple") {
            val sessionResult = validationService.validateSession(call.sessions.get<SessionUser>())
            if (sessionResult is ServiceResult.Forbidden) {
                return@post call.respondUnauthorized()
            }
            val session = (sessionResult as ServiceResult.Success).data

            val body = call.receive<CreateMultiPhotographerRequestRequest>()

            val result = photographerService.createPhotographyRequest(
                requesterId = session.userId,
                photographerIds = body.photographerIds,
                petId = body.petId,
                message = body.message
            )

            result.fold(
                onSuccess = { requestIds ->
                    call.respond(mapOf("success" to true, "requestIds" to requestIds))
                },
                onFailure = { error ->
                    call.respondError(error.message ?: "Failed to create requests", 400)
                }
            )
        }

        get("/requests") {
            val userResult = validateUser()
            when (userResult) {
                is ServiceResult.Forbidden -> return@get call.respondUnauthorized()
                is ServiceResult.NotFound -> return@get call.respondNotFound()
                else -> {}
            }
            val user = (userResult as ServiceResult.Success).data

            val result = photographerService.getRequestsForUser(user)
            call.respond(result)
        }

        put("/requests/{id}") {
            val sessionResult = validationService.validateSession(call.sessions.get<SessionUser>())
            if (sessionResult is ServiceResult.Forbidden) {
                return@put call.respondUnauthorized()
            }
            val session = (sessionResult as ServiceResult.Success).data

            val idResult = validationService.validateId(call.parameters["id"])
            if (idResult is ServiceResult.Error) {
                return@put call.respondError(idResult.message, 400)
            }
            val requestId = (idResult as ServiceResult.Success).data

            val body = call.receive<UpdatePhotographyRequestRequest>()
            val userResult = validationService.validateUserById(session.userId)
            val user = if (userResult is ServiceResult.Success) userResult.data else null

            try {
                call.respondData(photographerService.updatePhotographyRequest(session.userId, user, requestId, body))
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }
    }
}
