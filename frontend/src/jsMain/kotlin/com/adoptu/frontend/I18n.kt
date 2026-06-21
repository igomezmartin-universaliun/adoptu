package com.adoptu.frontend

import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.*
import kotlin.js.Promise
import kotlin.js.json
import kotlin.math.roundToInt

object I18n {
    var currentLang: String = "en"
    private val translations = mutableMapOf<String, dynamic>()
    private val loadedLangs = mutableSetOf<String>()

    val langEmojis = mapOf(
        "en" to "\uD83C\uDDFA\uD83C\uDDF8",
        "es" to "\uD83C\uDDEA\uD83C\uDDF8",
        "fr" to "\uD83C\uDDEB\uD83C\uDDF7",
        "pt" to "\uD83C\uDDE7\uD83C\uDDF7",
        "zh" to "\uD83C\uDDE8\uD83C\uDDF3"
    )

    suspend fun loadLang(lang: String) {
        if (loadedLangs.contains(lang) || lang == "en") return
        try {
            val script = document.createElement("script") as HTMLScriptElement
            script.src = "/static/js/i18n/i18n-$lang.js"
            document.head?.appendChild(script)
            Promise<Unit> { resolve, _ ->
                script.onload = { loadedLangs.add(lang); resolve(Unit) }
                script.onerror = { loadedLangs.add(lang); resolve(Unit) }
            }.awaitI18n()
        } catch (e: Exception) {
            console.error("Failed to load language: $lang", e)
        }
    }

    fun t(key: String): String {
        val parts = key.split(".")
        var value: dynamic = translations[currentLang]
        for (part in parts) {
            value = value?.asDynamic()[part]
            if (value == undefined) break
        }
        if (value != undefined && value != null) return value.toString()

        value = translations["en"]
        for (part in parts) {
            value = value?.asDynamic()[part]
            if (value == undefined) break
        }
        return if (value != undefined && value != null) value.toString() else key
    }

    fun setLang(lang: String, persist: Boolean = true) {
        loadLang(lang) {
            currentLang = lang
            if (persist) {
                localStorage.setItem("lang", lang)
            }
            updatePage()
            updateDropdownLabel()
        }
    }

    private fun loadLang(lang: String, callback: () -> Unit) {
        if (loadedLangs.contains(lang) || lang == "en") {
            currentLang = lang
            callback()
            return
        }
        val script = document.createElement("script") as HTMLScriptElement
        script.src = "/static/js/i18n/i18n-$lang.js"
        document.head?.appendChild(script)
        script.onload = { loadedLangs.add(lang); currentLang = lang; callback() }
        script.onerror = { loadedLangs.add(lang); currentLang = lang; callback() }
    }

    fun updatePage() {
        document.querySelectorAll("[data-i18n]").forEach { el ->
            val key = el.getAttribute("data-i18n")
            if (key != null) {
                val text = t(key)
                if (text.isNotEmpty() && text != key) {
                    el.textContent = text
                }
            }
        }
        document.querySelectorAll("[data-i18n-placeholder]").forEach { el ->
            val key = el.getAttribute("data-i18n-placeholder")
            if (key != null) {
                (el as? HTMLInputElement)?.placeholder = t(key)
            }
        }
        sortCountryOptions()
        updateActiveLangOption()
    }

    fun updateActiveLangOption() {
        document.querySelectorAll(".lang-option").forEach { opt ->
            if (opt.getAttribute("data-lang") == currentLang) {
                opt.classList.add("active")
            } else {
                opt.classList.remove("active")
            }
        }
    }

    fun sortCountryOptions() {
        val countrySelect = document.getElementById("search-country") as? HTMLSelectElement ?: return
        val options = js("Array.from")(countrySelect.options).asDynamic()
        val selectedValue = countrySelect.value
        js("options.sort")(js("(a, b) => a.textContent.localeCompare(b.textContent)"))
        countrySelect.innerHTML = ""
        for (i in 0 until options.length) {
            countrySelect.appendChild(options[i] as Node)
        }
        countrySelect.value = selectedValue
    }

    fun updateDropdownLabel() {
        val btn = document.querySelector(".lang-dropbtn") as? HTMLElement
        btn?.innerHTML = (langEmojis[currentLang] ?: "\uD83C\uDF10") + " \u25BC"
    }

    fun registerTranslations(lang: String, data: dynamic) {
        translations[lang] = data
    }
}

fun setLangGlobal(lang: String) {
    I18n.setLang(lang)
    val btn = document.querySelector(".lang-dropbtn") as? HTMLElement
    btn?.innerHTML = (I18n.langEmojis[lang] ?: "\uD83C\uDF10") + " \u25BC"
}

fun t(key: String): String = I18n.t(key)

suspend fun Promise<*>.awaitI18n(): Unit = kotlin.coroutines.suspendCoroutine { cont ->
    this.asDynamic().then(
        onFulfilled = { cont.resume(Unit) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

external object console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
}

external val undefined: dynamic
