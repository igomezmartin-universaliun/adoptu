package com.adoptu.plugins

import com.adoptu.services.ServiceResult
import com.adoptu.services.validation.ValidationConstants
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

suspend fun ApplicationCall.respondUnauthorized() = respondError(ValidationConstants.UNAUTHORIZED, 401)
suspend fun ApplicationCall.respondForbidden() = respondError(ValidationConstants.FORBIDDEN, 403)
suspend fun ApplicationCall.respondNotFound(message: String = ValidationConstants.USER_NOT_FOUND) = respondError(message, 404)
suspend fun ApplicationCall.respondInvalidId(fieldName: String = ValidationConstants.INVALID_ID) = respondError(fieldName, 400)

interface ServiceResultResponder {
    suspend fun respond(call: ApplicationCall, result: ServiceResult<*>)
}

class DataResponder : ServiceResultResponder {
    override suspend fun respond(call: ApplicationCall, result: ServiceResult<*>) {
        when (result) {
            is ServiceResult.Success -> call.respond(result.data as Any)
            is ServiceResult.NotFound -> call.respondError("Not found", 404)
            is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
            is ServiceResult.Error -> call.respondError(result.message)
        }
    }
}

class SuccessResponder : ServiceResultResponder {
    override suspend fun respond(call: ApplicationCall, result: ServiceResult<*>) {
        when (result) {
            is ServiceResult.Success -> call.respond(SuccessResponse(success = true))
            is ServiceResult.NotFound -> call.respondError("Not found", 404)
            is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
            is ServiceResult.Error -> call.respondError(result.message)
        }
    }
}

class CustomResponder<T>(private val transform: suspend (T) -> Unit) : ServiceResultResponder {
    override suspend fun respond(call: ApplicationCall, result: ServiceResult<*>) {
        when (result) {
            is ServiceResult.Success -> transform(result.data as T)
            is ServiceResult.NotFound -> call.respondError("Not found", 404)
            is ServiceResult.Forbidden -> call.respondError("Forbidden", 403)
            is ServiceResult.Error -> call.respondError(result.message)
        }
    }
}

suspend fun ApplicationCall.respondServiceResult(result: ServiceResult<*>, responder: ServiceResultResponder) {
    responder.respond(this, result)
}

suspend inline fun <T> ApplicationCall.respondData(result: ServiceResult<T>) {
    when (result) {
        is ServiceResult.Success -> respond(result.data as Any)
        is ServiceResult.NotFound -> respondError("Not found", 404)
        is ServiceResult.Forbidden -> respondError("Forbidden", 403)
        is ServiceResult.Error -> respondError(result.message)
    }
}

suspend fun ApplicationCall.respondSuccess(result: ServiceResult<*>) {
    when (result) {
        is ServiceResult.Success -> respond(SuccessResponse(success = true))
        is ServiceResult.NotFound -> respondError("Not found", 404)
        is ServiceResult.Forbidden -> respondError("Forbidden", 403)
        is ServiceResult.Error -> respondError(result.message)
    }
}
