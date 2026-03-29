package com.adoptu.routes

import com.adoptu.pages.*
import com.adoptu.services.auth.WebAuthnService
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.koin.ktor.ext.inject

fun Route.uiRoutes() {
    val webAuthnService by inject<WebAuthnService>()
    staticResources("/static", "static")

    get("/") { call.respondHtml(HttpStatusCode.OK) { indexPage() } }
    get("/login") { call.respondHtml(HttpStatusCode.OK) { loginPage() } }
    get("/register") { call.respondHtml(HttpStatusCode.OK) { registerPage() } }
    get("/photographers") { call.respondHtml(HttpStatusCode.OK) { photographersPage() } }
    get("/pet/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) return@get call.respondRedirect("/pets")
        call.respondHtml(HttpStatusCode.OK) { petDetailPage() }
    }
    get("/pets") { call.respondHtml(HttpStatusCode.OK) { petsPage() } }
    get("/my-pets") { call.respondHtml(HttpStatusCode.OK) { myPetsPage() } }
    get("/profile") { call.respondHtml(HttpStatusCode.OK) { profilePage() } }
    get("/admin") { call.respondHtml(HttpStatusCode.OK) { adminPage() } }
    get("/admin/shelters") { call.respondHtml(HttpStatusCode.OK) { adminSheltersPage() } }
    get("/privacy") { call.respondHtml(HttpStatusCode.OK) { privacyPage() } }
    get("/terms") { call.respondHtml(HttpStatusCode.OK) { termsPage() } }
    get("/temporal-home") { call.respondHtml(HttpStatusCode.OK) { temporalHomeProfilePage() } }
    get("/temporal-homes") { call.respondHtml(HttpStatusCode.OK) { temporalHomesSearchPage() } }
    get("/shelters") { call.respondHtml(HttpStatusCode.OK) { sheltersPage() } }
    get("/sterilization-locations") { call.respondHtml(HttpStatusCode.OK) { sterilizationLocationsPage() } }
    get("/admin/sterilization-locations") { call.respondHtml(HttpStatusCode.OK) { adminSterilizationLocationsPage() } }
    get("/verify") {
        val token = call.request.queryParameters["token"]
        val success: Boolean
        val language: String
        if (token.isNullOrBlank()) {
            success = false
            language = "en"
        } else {
            val result = webAuthnService.verifyTokenAndGetLanguage(token)
            success = result.first
            language = result.second
        }
        call.respondHtml(HttpStatusCode.OK) { emailVerificationPage(success, language) }
    }
    get("/temporal-home/block/{temporalHomeId}") {
        val temporalHomeId = call.parameters["temporalHomeId"]?.toIntOrNull()
        val rescuerId = call.request.queryParameters["rescuer"]?.toIntOrNull()
        if (temporalHomeId != null && rescuerId != null) {
            call.respondHtml(HttpStatusCode.OK) {
                head { title { +"Block Rescuer" } }
                body {
                    h1 { +"Report as Spam & Block Rescuer" }
                    p { +"Are you sure you want to block this rescuer from sending you more requests?" }
                    button(type = ButtonType.button) {
                        +"Block Rescuer"
                        onClick = "blockRescuerAndRedirect($temporalHomeId, $rescuerId)"
                    }
                    script {
                        unsafe { raw("""
                            async function blockRescuerAndRedirect(thId, rId) {
                                try {
                                    const res = await fetch('/api/temporal-homes/block/'+thId+'?rescuer='+rId);
                                    const data = await res.json();
                                    if (data.blocked) {
                                        document.body.innerHTML = '<h1>Rescuer blocked!</h1><p>You will no longer receive requests from this rescuer.</p><a href="/">Go to Home</a>';
                                    } else {
                                        document.body.innerHTML = '<h1>This rescuer was already blocked.</h1><a href="/">Go to Home</a>';
                                    }
                                } catch(e) { document.body.innerHTML = '<h1>Error blocking rescuer</h1>'; }
                            }
                        """) }
                    }
                }
            }
        } else {
            call.respondRedirect("/temporal-home")
        }
    }
}
