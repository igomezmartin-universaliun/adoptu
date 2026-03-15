package com.adoptu.plugins

import com.adoptu.plugins.routes.authRoutes
import com.adoptu.plugins.routes.petsRoutes
import com.adoptu.plugins.routes.uiRoutes
import com.adoptu.plugins.routes.usersRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        uiRoutes()
        authRoutes()
        petsRoutes()
        usersRoutes()
    }
}
