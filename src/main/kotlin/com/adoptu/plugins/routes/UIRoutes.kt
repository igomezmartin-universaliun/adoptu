package com.adoptu.plugins.routes

import com.adoptu.pages.*
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.uiRoutes() {
    staticResources("/static", "static")

    get("/") { call.respondHtml(HttpStatusCode.OK) { petsPage() } }
    get("/login") { call.respondHtml(HttpStatusCode.OK) { loginPage() } }
    get("/register") { call.respondHtml(HttpStatusCode.OK) { registerPage() } }
    get("/pets") { call.respondHtml(HttpStatusCode.OK) { petsPage() } }
    get("/pet/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) return@get call.respondRedirect("/pets")
        call.respondHtml(HttpStatusCode.OK) { petDetailPage() }
    }
    get("/my-pets") { call.respondHtml(HttpStatusCode.OK) { myPetsPage() } }
    get("/admin") { call.respondHtml(HttpStatusCode.OK) { adminPage() } }
}
