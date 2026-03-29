package com.adoptu.plugins

import com.adoptu.routes.*
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
        adminUsersRoutes()
        photographerRoutes()
        temporalHomeRoutes()
        shelterRoutes()
        adminShelterRoutes()
        sterilizationLocationRoutes()
        adminSterilizationLocationRoutes()
    }
}
