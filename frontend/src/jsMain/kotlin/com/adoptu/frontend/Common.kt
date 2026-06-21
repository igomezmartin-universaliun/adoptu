package com.adoptu.frontend

import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlin.js.json

external fun fetch(resource: String, init: dynamic = definedExternally): Promise<FetchResponse>
external interface FetchResponse {
    val ok: Boolean
    fun json(): Promise<dynamic>
    fun text(): Promise<String>
}

suspend fun Promise<FetchResponse>.awaitCommon(): FetchResponse = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

suspend fun Promise<dynamic>.awaitCommon(): dynamic = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

external object console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
}

external val undefined: dynamic
external object JSON {
    fun stringify(value: dynamic): String
    fun parse(text: String): dynamic
}

object Common {
    val isLoggedIn = js("window.isLoggedInGlobal") == true

    suspend fun initI18n(userLanguage: String? = null) {
        val lang = userLanguage ?: localStorage.getItem("lang") ?: "en"
        if (lang != "en") {
            I18n.loadLang(lang)
        }
        I18n.currentLang = lang
        I18n.updatePage()
        I18n.updateDropdownLabel()
    }

    suspend fun init() {
        js("window.onCountryChange = function() {")
        js("  var hasCountry = document.getElementById('search-country').value !== '';")
        js("  var filters = document.querySelectorAll('.search-filters input');")
        js("  filters.forEach(function(input) { input.disabled = !hasCountry; });")
        js("}")

        js("window.setLang = function(lang) {")
        js("  var langEmojis = {en: '\uD83C\uDDFA\uD83C\uDDF8', es: '\uD83C\uDDEA\uD83C\uDDF8', fr: '\uD83C\uDDEB\uD83C\uDDF7', pt: '\uD83C\uDDE7\uD83C\uDDF7', zh: '\uD83C\uDDE8\uD83C\uDDF3'};")
        js("  AdoptuI18n.setLang(lang);")
        js("  var btn = document.querySelector('.lang-dropbtn');")
        js("  if (btn) btn.innerHTML = langEmojis[lang] + ' \u25BC';")
        js("}")

        js("window.t = function(key) { return AdoptuI18n.t(key); }")

        if (isLoggedIn) {
            initLoggedIn()
        } else {
            initI18n()
        }
    }

    private suspend fun initLoggedIn() {
        val user = try {
            ApiClient.me()
        } catch (e: Throwable) {
            console.error("Failed to fetch user:", e)
            return
        }

        if (user.authenticated == false) return

        initI18n(user.language?.toString())

        val roles = (user.activeRoles as? Array<*>)?.map { it.toString() } ?: emptyList()
        val currentPath = window.location.pathname

        if ((roles.contains("PHOTOGRAPHER") || roles.contains("TEMPORAL_HOME")) && currentPath != "/profile") {
            val needsRedirect = checkProfileCompletion(user, roles)
            if (needsRedirect) {
                window.location.href = "/profile"
                return
            }
        }

        setupInactivityTimer()
    }

    private suspend fun checkProfileCompletion(user: dynamic, roles: List<String>): Boolean {
        if (roles.contains("PHOTOGRAPHER")) {
            if (user.photographerCountry == null || user.photographerState == null) return true
        }
        if (roles.contains("TEMPORAL_HOME")) {
            try {
                ApiClient.getTemporalHome()
            } catch (e: Throwable) {
                return true
            }
        }
        return false
    }

    private fun setupInactivityTimer() {
        val timeout = 5 * 60 * 1000
        var timer: dynamic = null

        val resetTimer = {
            js("clearTimeout")(timer)
            timer = js("setTimeout")(suspend {
                try {
                    ApiClient.logout()
                    window.location.href = "/"
                } catch (e: Throwable) {
                    window.location.href = "/"
                }
            }, timeout)
        }

        listOf("click", "keypress", "mousemove", "scroll", "touchstart").forEach { event ->
            document.addEventListener(event, { resetTimer() }, js("{passive: true}"))
        }
        resetTimer()
    }
}
