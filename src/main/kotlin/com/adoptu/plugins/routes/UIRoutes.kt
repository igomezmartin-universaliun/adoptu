package com.adoptu.plugins.routes

import com.adoptu.pages.adminPage
import com.adoptu.pages.indexPage
import com.adoptu.pages.loginPage
import com.adoptu.pages.myPetsPage
import com.adoptu.pages.petDetailPage
import com.adoptu.pages.photographersPage
import com.adoptu.pages.privacyPage
import com.adoptu.pages.profilePage
import com.adoptu.pages.registerPage
import com.adoptu.pages.termsPage
import com.adoptu.pages.temporalHomeProfilePage
import com.adoptu.pages.temporalHomesSearchPage
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.ButtonType
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.onClick
import kotlinx.html.unsafe

fun Route.uiRoutes() {
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
    get("/my-pets") { call.respondHtml(HttpStatusCode.OK) { myPetsPage() } }
    get("/profile") { call.respondHtml(HttpStatusCode.OK) { profilePage() } }
    get("/admin") { call.respondHtml(HttpStatusCode.OK) { adminPage() } }
    get("/privacy") { call.respondHtml(HttpStatusCode.OK) { privacyPage() } }
    get("/terms") { call.respondHtml(HttpStatusCode.OK) { termsPage() } }
    get("/temporal-home") { call.respondHtml(HttpStatusCode.OK) { temporalHomeProfilePage() } }
    get("/temporal-homes") { call.respondHtml(HttpStatusCode.OK) { temporalHomesSearchPage() } }
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
