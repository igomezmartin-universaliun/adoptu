package com.adoptu.plugins.routes

import com.adoptu.auth.SessionUser
import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.plugins.respondError
import com.adoptu.services.UserService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject

fun Route.usersRoutes() {
    val userService by inject<UserService>()

    route("/api/users") {
        post("/accept-terms") {
            val session = call.sessions.get<SessionUser>()
                ?: return@post call.respondError("Unauthorized", 401)

            val body = call.receive<AcceptTermsRequest>()
            val user = userService.acceptTerms(session.userId, body)
                ?: return@post call.respondError("User not found", 404)

            call.respond(user)
        }
    }
}
