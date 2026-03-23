package com.adoptu.plugins.routes

import com.adoptu.services.auth.SessionUser
import com.adoptu.dto.CreateMultiPhotographerRequestRequest
import com.adoptu.dto.CreatePhotographyRequestRequest
import com.adoptu.dto.PhotographerSettingsRequest
import com.adoptu.dto.RoleActivationRequest
import com.adoptu.dto.UpdatePhotographyRequestRequest
import com.adoptu.adapters.db.PhotographyRequests
import com.adoptu.plugins.respondError
import com.adoptu.services.PhotographerService
import com.adoptu.services.UserService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.ktor.ext.inject
import kotlinx.serialization.Serializable

@Serializable
data class PhotographerProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val displayName: String,
    val language: String,
    val activeRoles: Set<String>,
    val lastAcceptedPrivacyPolicy: Long?,
    val lastAcceptedTermsAndConditions: Long?,
    val photographerFee: Double?,
    val photographerCurrency: String?,
    val photographerCountry: String?,
    val photographerState: String?
)

fun Route.photographerRoutes() {
    val userService by inject<UserService>()
    val photographerService by inject<PhotographerService>()

    route("/api/photographers") {
        get {
            val country = call.parameters["country"]
            val state = call.parameters["state"]
            val photographers = photographerService.getPhotographers(country, state)
            call.respond(photographers)
        }

        post("/profile") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val body = call.receive<RoleActivationRequest>()
            val user = if (body.activate) {
                userService.activatePhotographerProfile(session.userId)
            } else {
                userService.deactivatePhotographerProfile(session.userId)
            }
                ?: return@post call.respondError("User not found", 404)

            call.respond(user)
        }

        put("/settings") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondError("Unauthorized", 401)
            val user = userService.getById(session.userId) ?: return@put call.respondError("User not found", 404)
            val activeRoles = user.activeRoles.map { it.name }
            if (!activeRoles.contains("PHOTOGRAPHER") && !activeRoles.contains("ADMIN")) {
                return@put call.respondError("Forbidden", 403)
            }

            val body = call.receive<PhotographerSettingsRequest>()
            if (body.photographerFee < 0) {
                return@put call.respondError("Photographer fee must be zero or positive", 400)
            }

            try {
                val photographer = photographerService.updatePhotographerSettings(session.userId, body)
                    ?: return@put call.respondError("User not found", 404)
                call.respond(photographer)
            } catch (e: IllegalArgumentException) {
                call.respondError(e.message ?: "Invalid request", 400)
            }
        }

        post("/requests") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val body = call.receive<CreatePhotographyRequestRequest>()

            val photographer = photographerService.getPhotographerById(body.photographerId)
                ?: return@post call.respondError("Photographer not found or not active", 404)

            val createdAt = System.currentTimeMillis()
            val requestId = transaction {
                PhotographyRequests.insert {
                    it[photographerId] = body.photographerId
                    it[requesterId] = session.userId
                    it[petId] = body.petId
                    it[message] = body.message
                    it[status] = "PENDING"
                    it[PhotographyRequests.createdAt] = createdAt
                } get PhotographyRequests.id
            }

            call.respond(mapOf(
                "id" to requestId,
                "photographerId" to body.photographerId,
                "photographerName" to photographer.displayName,
                "requesterId" to session.userId,
                "petId" to body.petId,
                "message" to body.message,
                "status" to "PENDING",
                "createdAt" to createdAt
            ))
        }

        post("/requests/multiple") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

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
            val session = call.sessions.get<SessionUser>()
                ?: return@get call.respondError("Unauthorized", 401)
            val user = userService.getById(session.userId) ?: return@get call.respondError("User not found", 404)
            val activeRoles = user.activeRoles.map { it.name }

            val requests = transaction {
                if (activeRoles.contains("PHOTOGRAPHER")) {
                    PhotographyRequests.selectAll()
                        .where { PhotographyRequests.photographerId eq session.userId }
                        .toList()
                } else {
                    PhotographyRequests.selectAll()
                        .where { PhotographyRequests.requesterId eq session.userId }
                        .toList()
                }
            }

            val result = requests.map { row ->
                val photographer = userService.getById(row[PhotographyRequests.photographerId])
                val requester = userService.getById(row[PhotographyRequests.requesterId])
                mapOf(
                    "id" to row[PhotographyRequests.id],
                    "photographerId" to row[PhotographyRequests.photographerId],
                    "photographerName" to photographer?.displayName,
                    "requesterId" to row[PhotographyRequests.requesterId],
                    "requesterName" to requester?.displayName,
                    "petId" to row[PhotographyRequests.petId],
                    "message" to row[PhotographyRequests.message],
                    "status" to row[PhotographyRequests.status],
                    "scheduledDate" to row[PhotographyRequests.scheduledDate],
                    "createdAt" to row[PhotographyRequests.createdAt]
                )
            }
            call.respond(result)
        }

        put("/requests/{id}") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondError("Unauthorized", 401)
            
            val requestId = call.parameters["id"]!!.toIntOrNull() 
                ?: return@put call.respondError("Invalid ID", 400)
            
            val body = call.receive<UpdatePhotographyRequestRequest>()
            
            val existing = transaction {
                PhotographyRequests.selectAll()
                    .where { PhotographyRequests.id eq requestId }
                    .firstOrNull()
            } ?: return@put call.respondError("Request not found", 404)

            val user = userService.getById(session.userId)
            val activeRoles = user?.activeRoles?.map { it.name } ?: emptyList()
            val isPhotographer = existing[PhotographyRequests.photographerId] == session.userId
            val isRequester = existing[PhotographyRequests.requesterId] == session.userId
            val isAdmin = activeRoles.contains("ADMIN")
            
            if (!isPhotographer && !isRequester && !isAdmin) {
                return@put call.respondError("Forbidden", 403)
            }

            if (body.status != null) {
                val validTransitions = if (isPhotographer || isAdmin) {
                    mapOf("PENDING" to listOf("APPROVED", "REJECTED", "CANCELLED"),
                          "APPROVED" to listOf("COMPLETED", "CANCELLED"))
                } else {
                    mapOf("PENDING" to listOf("CANCELLED"))
                }
                val currentStatus = existing[PhotographyRequests.status]
                if (body.status !in (validTransitions[currentStatus] ?: emptyList())) {
                    return@put call.respondError("Invalid status transition", 400)
                }
            }

            transaction {
                PhotographyRequests.update({ PhotographyRequests.id eq requestId }) {
                    if (body.status != null) it[status] = body.status
                    if (body.scheduledDate != null) it[scheduledDate] = body.scheduledDate
                }
            }

            val updated = transaction {
                PhotographyRequests.selectAll()
                    .where { PhotographyRequests.id eq requestId }
                    .firstOrNull()
            }!!

            val photographer = userService.getById(updated[PhotographyRequests.photographerId])
            val requester = userService.getById(updated[PhotographyRequests.requesterId])

            call.respond(mapOf(
                "id" to updated[PhotographyRequests.id],
                "photographerId" to updated[PhotographyRequests.photographerId],
                "photographerName" to photographer?.displayName,
                "requesterId" to updated[PhotographyRequests.requesterId],
                "requesterName" to requester?.displayName,
                "petId" to updated[PhotographyRequests.petId],
                "message" to updated[PhotographyRequests.message],
                "status" to updated[PhotographyRequests.status],
                "scheduledDate" to updated[PhotographyRequests.scheduledDate],
                "createdAt" to updated[PhotographyRequests.createdAt]
            ))
        }
    }
}
