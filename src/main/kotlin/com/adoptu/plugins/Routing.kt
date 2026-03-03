package com.adoptu.plugins

import com.adoptu.plugins.routes.authRoutes
import com.adoptu.plugins.routes.petsRoutes
import com.adoptu.plugins.routes.uiRoutes
import com.adoptu.plugins.routes.usersRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        uiRoutes()
        authRoutes()
        petsRoutes()
        usersRoutes()
    }
}
