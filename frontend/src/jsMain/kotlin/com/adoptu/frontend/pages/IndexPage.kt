package com.adoptu.frontend.pages

import com.adoptu.frontend.ApiClientModule
import com.adoptu.frontend.I18n
import com.adoptu.frontend.forEachElement
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import kotlin.js.Promise

private val emoji = mapOf("DOG" to "🐕", "CAT" to "🐱", "BIRD" to "🐦", "FISH" to "🐟")

@JsExport
@JsName("IndexPage")
object IndexPageModule {
    private var currentType = ""
    private var currentSex = ""

    fun init() {
        val countrySelect = document.getElementById("pets-country") as? HTMLSelectElement

        document.querySelectorAll(".filter-btn").forEachElement { node ->
            val btn = node.unsafeCast<HTMLElement>()
            btn.addEventListener("click", {
                document.querySelectorAll(".filter-btn").forEachElement { b -> b.unsafeCast<HTMLElement>().classList.remove("active") }
                btn.classList.add("active")
                currentType = btn.asDynamic().dataset.type?.toString() ?: ""
                loadPets()
            })
        }

        val sexFilter = document.querySelector(".filter-sex") as? HTMLSelectElement
        sexFilter?.addEventListener("change", {
            currentSex = sexFilter.value
            loadPets()
        })

        countrySelect?.addEventListener("change", { loadPets() })

        ApiClientModule.me().then<Unit> { user ->
            if (countrySelect != null && user.authenticated != false && user.country != null) {
                countrySelect.value = user.country.toString()
            }
        }.catch { }.then<Unit> { loadPets() }
    }

    fun loadPets(): Promise<Unit> {
        val countrySelect = document.getElementById("pets-country") as? HTMLSelectElement
        val country = countrySelect?.value ?: ""
        updateCountryHint()
        val container = document.getElementById("pets").unsafeCast<HTMLElement?>()
        val errorDiv = document.getElementById("pets-error").unsafeCast<HTMLElement?>()

        if (country.isEmpty()) {
            errorDiv?.style?.display = "block"
            errorDiv?.textContent = I18n.t("countryRequired")
            container?.innerHTML = ""
            return Promise.resolve(Unit)
        }
        errorDiv?.style?.display = "none"
        errorDiv?.textContent = ""

        return ApiClientModule.getPets(currentType.ifEmpty { null }, country).then<Unit> { pets ->
            renderPets(pets, container, country)
        }
    }

    private fun updateCountryHint() {
        val countrySelect = document.getElementById("pets-country") as? HTMLSelectElement
        val hint = document.getElementById("pets-country-hint").unsafeCast<HTMLElement?>()
        val hasCountry = countrySelect?.value?.isNotEmpty() == true
        hint?.style?.display = if (hasCountry) "none" else ""
    }

    private fun renderPets(pets: dynamic, container: HTMLElement?, country: String) {
        val list = (pets as? Array<dynamic>) ?: arrayOf()
        val filtered = list.filter { p -> currentSex.isEmpty() || p.sex == currentSex }
        if (filtered.isEmpty()) {
            container?.innerHTML = "<p>${I18n.t("noPetsFound")}</p>"
            return
        }
        container?.innerHTML = filtered.joinToString("") { p ->
            val images = p.images as? Array<dynamic>
            val primaryImage = images?.firstOrNull { it.isPrimary == true } ?: images?.firstOrNull()
            val imageHtml = if (primaryImage != null) {
                "<img src=\"${primaryImage.imageUrl}\" alt=\"${p.name}\">"
            } else {
                "<div class=\"pet-card-placeholder\">${emoji[p.type.toString()] ?: "🐾"}</div>"
            }
            val sexClass = if (p.sex == "MALE") "male" else "female"
            val sizeHtml = if (p.size != null) "<span class=\"pet-size\">${p.size}</span>" else ""
            val urgent = if (p.isUrgent == true) " ⚠️" else ""
            val breedHtml = if (p.breed != null) "<span class=\"pet-breed\">${p.breed}</span>" else ""
            val rescueDateHtml = if (p.rescueDate != null) {
                val date = js("new Date(p.rescueDate)").toLocaleDateString()
                "<span class=\"label\">${I18n.t("rescued")}</span><span class=\"value\">$date</span>"
            } else ""
            "<a href=\"/pet/${p.id}\" class=\"pet-card\">$imageHtml<div class=\"pet-card-body\">" +
                "<span class=\"pet-type\">${I18n.t(p.type.toString().lowercase())}</span>" +
                "<span class=\"pet-sex $sexClass\">${I18n.t(p.sex.toString().lowercase())}</span>$sizeHtml" +
                "<div class=\"pet-name\"><h3>${p.name}$urgent</h3>$breedHtml</div>" +
                "<p class=\"pet-info\"><span class=\"pet-age\"><span class=\"label\">${I18n.t("age")}</span>" +
                "<span class=\"value\">${p.ageYears}${I18n.t("years")} ${p.ageMonths}${I18n.t("months")}</span></span>" +
                "<span class=\"pet-rescue-date\">$rescueDateHtml</span></p>" +
                "<p class=\"pet-status\">${p.status}</p></div></a>"
        }
    }
}
