package com.adoptu.plugins.routes

import com.adoptu.auth.SessionUser
import com.adoptu.dto.CreateAdoptionRequestRequest
import com.adoptu.dto.CreatePetRequest
import com.adoptu.dto.UpdatePetRequest
import com.adoptu.dto.UserRole
import com.adoptu.plugins.SuccessResponse
import com.adoptu.plugins.respondError
import com.adoptu.services.PetService
import com.adoptu.services.ServiceResult
import io.ktor.http.content.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

fun Route.petsRoutes() {
    val petService by inject<PetService>()

    route("/api/pets") {
        get {
            val type = call.request.queryParameters["type"]
            val pets = petService.getAll(type)
            call.respond(pets)
        }

        get("/{id}") {
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@get call.respondError("Invalid ID")
            val pet = petService.getById(id)
            if (pet != null) call.respond(pet) else call.respondError("Not found", 404)
        }

        post {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)
            if (session.role !in listOf(UserRole.RESCUER, UserRole.ADMIN)) return@post call.respondError("Forbidden", 403)

            val request = call.receive<CreatePetRequest>()
            val pet = petService.create(session.userId, request)
            call.respond(pet)
        }

        put("/{id}") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondError("Unauthorized", 401)
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@put call.respondError("Invalid ID")

            val body = call.receive<UpdatePetRequest>()
            when (val result = petService.update(id, session.userId, session.role, body)) {
                is ServiceResult.Success -> call.respond(result.data)
                is ServiceResult.NotFound -> call.respondError("Not found", 404)
                is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
            }
        }

        delete("/{id}") {
            val session = call.sessions.get<SessionUser>()
                ?: return@delete call.respondError("Unauthorized", 401)
            val id = call.parameters["id"]!!.toIntOrNull() ?: return@delete call.respondError("Invalid ID")

            when (val result = petService.delete(id, session.userId, session.role)) {
                is ServiceResult.Success -> call.respond(SuccessResponse(success = true))
                is ServiceResult.NotFound -> call.respondError("Not found", 404)
                is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
            }
        }

        post("/{id}/images") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)
            val petId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondError("Invalid ID")

            val imageIdsParam = call.request.queryParameters["imageIds"]
            if (imageIdsParam != null) {
                try {
                    val imageIds = imageIdsParam.split(",").mapNotNull { it.toIntOrNull() }
                    when (val result = runBlocking { petService.updatePetImages(petId, session.userId, session.role, imageIds) }) {
                        is ServiceResult.Success -> call.respond(mapOf("images" to result.data))
                        is ServiceResult.NotFound -> call.respondError("Not found", 404)
                        is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
                    }
                } catch (e: Exception) {
                    call.respondError("Failed to update images. Please try again later.", 500)
                }
                return@post
            }

            val multipart = call.receiveMultipart()
            var imageData: ByteArray? = null
            var fileName = "image"
            var contentType = "image/jpeg"
            var isPrimary = false

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName ?: "image"
                        contentType = part.contentType?.toString() ?: "image/jpeg"
                        imageData = part.streamProvider().readBytes()
                    }
                    is PartData.FormItem -> {
                        if (part.name == "isPrimary") {
                            isPrimary = part.value.toBoolean()
                        }
                    }
                    else -> {}
                }
            }

            if (imageData == null) {
                return@post call.respondError("No image provided")
            }

            try {
                when (val result = runBlocking { petService.uploadAndAddImage(
                    petId = petId,
                    userId = session.userId,
                    userRole = session.role,
                    imageName = fileName,
                    contentType = contentType,
                    imageData = imageData,
                    isPrimary = isPrimary
                ) }) {
                    is ServiceResult.Success -> call.respond(result.data)
                    is ServiceResult.NotFound -> call.respondError("Not found", 404)
                    is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
                }
            } catch (e: Exception) {
                call.respondError("Failed to upload image. Please try again later.", 500)
            }
        }

        delete("/{petId}/images/{imageId}") {
            val session = call.sessions.get<SessionUser>()
                ?: return@delete call.respondError("Unauthorized", 401)
            val petId = call.parameters["petId"]?.toIntOrNull() ?: return@delete call.respondError("Invalid pet ID")
            val imageId = call.parameters["imageId"]?.toIntOrNull() ?: return@delete call.respondError("Invalid image ID")

            try {
                when (val result = runBlocking { petService.removeImage(petId, imageId, session.userId, session.role) }) {
                    is ServiceResult.Success -> call.respond(SuccessResponse(success = true))
                    is ServiceResult.NotFound -> call.respondError("Not found", 404)
                    is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
                }
            } catch (e: Exception) {
                call.respondError("Failed to delete image. Please try again later.", 500)
            }
        }

        put("/{petId}/images/{imageId}/primary") {
            val session = call.sessions.get<SessionUser>()
                ?: return@put call.respondError("Unauthorized", 401)
            val petId = call.parameters["petId"]?.toIntOrNull() ?: return@put call.respondError("Invalid pet ID")
            val imageId = call.parameters["imageId"]?.toIntOrNull() ?: return@put call.respondError("Invalid image ID")

            try {
                when (val result = petService.setPrimaryImage(petId, imageId, session.userId, session.role)) {
                    is ServiceResult.Success -> call.respond(SuccessResponse(success = true))
                    is ServiceResult.NotFound -> call.respondError("Not found", 404)
                    is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
                }
            } catch (e: Exception) {
                call.respondError("Failed to set primary image. Please try again later.", 500)
            }
        }

        post("/{id}/adopt") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)
            if (session.role != UserRole.ADOPTER) return@post call.respondError("Only adopters can request adoption", 403)

            val id = call.parameters["id"]!!.toIntOrNull() ?: return@post call.respondError("Invalid ID")
            val body = call.receive<CreateAdoptionRequestRequest>()
            val message = body.message

            val request = petService.createAdoptionRequest(id, session.userId, message)
            call.respond(request)
        }
    }
}
