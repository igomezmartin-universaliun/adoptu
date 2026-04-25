package com.adoptu.frontend

import com.adoptu.frontend.pages.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

external fun fetch(resource: String, init: dynamic = definedExternally): Promise<FetchResponse>
external interface FetchResponse {
    val ok: Boolean
    fun json(): Promise<dynamic>
    fun text(): Promise<String>
}

external object JSON {
    fun stringify(value: dynamic): String
    fun parse(text: String): dynamic
}

external object console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
}

external val undefined: dynamic

suspend fun Promise<FetchResponse>.awaitMain(): FetchResponse = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

suspend fun Promise<dynamic>.awaitMain(): dynamic = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

fun main() {
    val path = window.location.pathname

    js("window.AdoptuI18n = I18n")
    js("window.AdoptuMyPets = MyPetsPage")
    js("window.AdoptuTemporalHomeProfile = TemporalHomeProfilePage")
    js("window.AdoptuTemporalHomeSearch = TemporalHomeSearchPage")
    js("window.AdoptuAdminShelters = AdminSheltersPage")
    js("window.AdoptuAdminSterilizationLocations = AdminSterilizationLocationsPage")

    Common.init()

    when {
        path == "/" -> IndexPage.init()
        path == "/login" -> LoginPage.init()
        path == "/register" -> RegisterPage.init()
        path == "/profile" -> ProfilePage.init()
        path == "/my-pets" -> MyPetsPage.init()
        path.startsWith("/pet/") -> PetDetailPage.init()
        path == "/pet-food" -> PetFoodPage.init()
        path == "/photographers" -> PhotographersPage.init()
        path == "/shelters" -> SheltersPage.init()
        path == "/sterilization" -> SterilizationPage.init()
        path == "/sterilization-locations" -> SterilizationPage.init()
        path == "/temporal-home" -> TemporalHomeProfilePage.init()
        path == "/temporal-homes" -> TemporalHomeSearchPage.init()
        path.startsWith("/temporal-home/") -> TemporalHomePage.init()
        path == "/admin/shelters" -> AdminSheltersPage.init()
        path == "/admin/sterilization-locations" -> AdminSterilizationLocationsPage.init()
        path == "/forgot-password" -> ForgotPasswordPage.init()
        path == "/reset-password" -> ResetPasswordPage.init()
        path == "/magic-link-login" -> MagicLinkLoginPage.init()
        path == "/email-verification" -> EmailVerificationPage.init()
        path == "/email-change-verification" -> EmailChangeVerificationPage.init()
    }
}
