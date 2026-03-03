package com.adoptu.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SuccessResponse(val success: Boolean)

suspend fun ApplicationCall.respondError(message: String, status: Int = 400) {
    respond(HttpStatusCode.fromValue(status), ErrorResponse(error = message))
}
