package com.adoptu.frontend.pages

import com.adoptu.frontend.*
import kotlinx.browser.document
import org.w3c.dom.*
import kotlin.js.json

object IndexPage {
    private val emoji = mapOf("DOG" to "\uD83D\uDC15", "CAT" to "\uD83D\uDC31", "BIRD" to "\uD83D\uDC26", "FISH" to "\uD83D\uDC1F")
    private var currentType = ""
    private var currentSex = ""

    suspend fun init() {
        document.querySelectorAll(".filter-btn").forEach { btn ->
            (btn as HTMLElement).onclick = {
                document.querySelectorAll(".filter-btn").forEach { b -> (b as HTMLElement).classList.remove("active") }
                (btn as HTMLElement).classList.add("active")
                currentType = btn.getAttribute("data-type") ?: ""
                loadPets()
                undefined
            }
        }

        val sexFilter = document.querySelector(".filter-sex") as? HTMLSelectElement
        sexFilter?.onchange = {
            currentSex = sexFilter.value
            loadPets()
            undefined
        }

        loadPets()
    }

    private fun loadPets() {
        val container = document.getElementById("pets") ?: return
        val typeParam = if (currentType.isNotEmpty()) currentType else null

        val url = "/api/pets" + (typeParam?.let { "?type=$it" } ?: "")
        fetch(url).then { response ->
            response.json().then { pets ->
                val filteredPets = js("Array.from")(pets).asDynamic().filter { p ->
                    currentSex.isEmpty() || p.sex == currentSex
                }

                if (filteredPets.length == 0) {
                    container.innerHTML = "<p>${I18n.t("noPetsFound")}</p>"
                } else {
                    var html = ""
                    for (i in 0 until filteredPets.length) {
                        val p = filteredPets[i]
                        val primaryImage = getPrimaryImage(p)
                        val imageHtml = if (primaryImage != null) {
                            "<img src=\"${primaryImage.imageUrl}\" alt=\"${p.name}\">"
                        } else {
                            "<div class=\"pet-card-placeholder\">${emoji[p.type] ?: "\uD83D\uDC3E"}</div>"
                        }

                        val sexClass = if (p.sex == "MALE") "male" else "female"
                        val sizeHtml = if (p.size != null) "<span class=\"pet-size\">${p.size}</span>" else ""
                        val breedHtml = if (p.breed != null) "<span class=\"pet-breed\">${p.breed}</span>" else ""
                        val urgentBadge = if (p.isUrgent == true) " \u26A0\uFE0F" else ""
                        val rescueDateHtml = if (p.rescueDate != null) {
                            val dateStr = js("new Date")(p.rescueDate).toLocaleDateString()
                            "<span class=\"pet-rescue-date\"><span class=\"label\">${I18n.t("rescued")}</span><span class=\"value\">$dateStr</span></span>"
                        } else ""

                        html += "<a href=\"/pet/${p.id}\" class=\"pet-card\">$imageHtml" +
                            "<div class=\"pet-card-body\">" +
                            "<span class=\"pet-type\">${I18n.t(p.type.toLowerCase())}</span>" +
                            "<span class=\"pet-sex $sexClass\">${I18n.t(p.sex.toLowerCase())}</span>" +
                            sizeHtml +
                            "<div class=\"pet-name\"><h3>${p.name}$urgentBadge</h3>$breedHtml</div>" +
                            "<p class=\"pet-info\">" +
                            "<span class=\"pet-age\"><span class=\"label\">${I18n.t("age")}</span><span class=\"value\">${p.ageYears}${I18n.t("years")} ${p.ageMonths}${I18n.t("months")}</span></span>" +
                            rescueDateHtml +
                            "</p>" +
                            "<p>${p.status}</p>" +
                            "</div></a>"
                    }
                    container.innerHTML = html
                }
                undefined
            }
        }
    }

    private fun getPrimaryImage(pet: dynamic): dynamic? {
        if (pet.images == null || pet.images.length == 0) return null
        for (i in 0 until pet.images.length) {
            if (pet.images[i].isPrimary) return pet.images[i]
        }
        return pet.images[0]
    }
}
