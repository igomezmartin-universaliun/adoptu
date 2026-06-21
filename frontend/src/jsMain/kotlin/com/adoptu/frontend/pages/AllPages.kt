package com.adoptu.frontend.pages

import com.adoptu.frontend.ApiClientModule
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*

@JsExport
@JsName("PetDetailPage")
object PetDetailPageModule {
    fun init() {
        val id = window.location.pathname.split("/").lastOrNull()
        if (id == null || id.isEmpty()) { window.location.href = "/"; return }

        ApiClientModule.getPet(id).then<Unit> { pet -> renderPetDetail(pet) }
    }

    private fun renderPetDetail(pet: dynamic) {
        val container = document.getElementById("pet-detail")
        if (container != null) {
            container.innerHTML = "<h1>${pet.name}</h1>"
        }
    }
}

@JsExport
@JsName("SheltersPage")
object SheltersPageModule {
    fun init() {
        try {
            renderEmpty()
        } catch (e: dynamic) {
            renderEmpty()
        }
    }

    @JsName("searchShelters")
    fun searchShelters() {
        try {
            val country = document.getElementById("search-country")?.unsafeCast<HTMLSelectElement>()?.value ?: ""
            if (country.isEmpty()) {
                renderEmpty()
                return
            }
            val params = mutableListOf<String>()
            params.add("country=$country")
            val state = document.getElementById("search-state")?.unsafeCast<HTMLInputElement>()?.value
            if (!state.isNullOrEmpty()) params.add("state=$state")
            val city = document.getElementById("search-city")?.unsafeCast<HTMLInputElement>()?.value
            if (!city.isNullOrEmpty()) params.add("city=$city")
            val zip = document.getElementById("search-zip")?.unsafeCast<HTMLInputElement>()?.value
            if (!zip.isNullOrEmpty()) params.add("zip=$zip")
            val query = "?" + params.joinToString("&")
            val url = "/api/shelters$query"
            window.asDynamic().fetch(url).then { res ->
                res.json().then { data ->
                    renderShelters(data)
                }
            }.catch { _ ->
                renderEmpty()
            }
        } catch (e: dynamic) {
            renderEmpty()
        }
    }

    private fun renderShelters(shelters: dynamic) {
        try {
            val container = document.getElementById("shelters")
            if (container != null) {
                container.innerHTML = ""
            }
        } catch (e: dynamic) {}
    }

    private fun renderEmpty() {
        try {
            val container = document.getElementById("shelters")
            if (container != null) {
                container.innerHTML = ""
            }
        } catch (e: dynamic) {}
    }
}

@JsExport
@JsName("TemporalHomeSearchPage")
object TemporalHomeSearchPageModule {
    fun init() {}
}
@JsExport
@JsName("PhotographersPage")
object PhotographersPageModule {
    fun init() {}
}

@JsExport
@JsName("SterilizationLocationsPage")
object SterilizationLocationsPageModule {
    fun init() {}
}
