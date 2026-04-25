package com.adoptu.frontend.pages

import com.adoptu.frontend.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import kotlin.js.json

object PetDetailPage {
    private val emoji = mapOf("DOG" to "\uD83D\uDC15", "CAT" to "\uD83D\uDC31", "BIRD" to "\uD83D\uDC26", "FISH" to "\uD83D\uDC1F")
    private var currentPet: dynamic = null

    suspend fun init() {
        val id = window.location.pathname.split("/").lastOrNull()
            ?: js("new URLSearchParams")(window.location.search).get("id")
        if (id == null) { window.location.href = "/"; return }

        var user: dynamic
        try { user = ApiClient.me() } catch (e: Throwable) { user = js("({authenticated: false})") }

        currentPet = ApiClient.getPet(id)
        val container = document.getElementById("pet-detail") ?: return
        val isOwner = ((user.activeRoles.asList().contains("RESCUER") || user.activeRoles.asList().contains("ADMIN")) && currentPet.rescuerId == user.id)
        val adoptForm = if (currentPet.status == "AVAILABLE" && user.activeRoles.asList().contains("ADOPTER")) {
            "<form id=\"adopt-form\"><label for=\"msg\">${I18n.t("message")} (${I18n.t("optional")})</label><textarea id=\"msg\" name=\"message\"></textarea><button type=\"submit\" class=\"btn\">${I18n.t("requestAdoption")}</button></form>"
        } else ""
        val editBtn = if (isOwner) "<a href=\"/my-pets?edit=${currentPet.id}\" class=\"btn\">${I18n.t("edit")}</a>" else ""

        val primaryImage = getPrimaryImage(currentPet)
        var details = "<div class=\"pet-detail-header\">"
        if (primaryImage != null) {
            details += "<img src=\"${primaryImage.imageUrl}\" alt=\"${currentPet.name}\" class=\"pet-main-image\">"
        } else {
            details += "<div class=\"pet-detail-placeholder\">${emoji[currentPet.type] ?: "\uD83D\uDC3E"}</div>"
        }
        details += "<span class=\"pet-type\">${I18n.t(currentPet.type.toLowerCase())}</span><h1>${currentPet.name}</h1>"
        if (currentPet.breed != null) details += "<p class=\"pet-breed\">${currentPet.breed}</p>"
        details += "<p><strong>Weight:</strong> ${currentPet.weight} kg | <strong>Age:</strong> ${currentPet.ageYears}y ${currentPet.ageMonths}m | <strong>Sex:</strong> ${I18n.t(currentPet.sex.toLowerCase())}</p>"
        details += "<p><strong>Status:</strong> ${currentPet.status}</p></div>"

        if (isOwner) {
            details += "<div class=\"storage-management\"><h3>${I18n.t("photos")}</h3>"
            details += "<div class=\"pet-images-grid\" id=\"pet-images\">${renderImages()}</div></div>"
        }

        details += "<p>${currentPet.description ?: "No description."}</p>"
        details += "<div class=\"pet-details-grid\">"
        currentPet.color?.let { details += "<div class=\"detail-item\"><strong>${I18n.t("color")}:</strong> $it</div>" }
        currentPet.size?.let { details += "<div class=\"detail-item\"><strong>${I18n.t("size")}:</strong> $it</div>" }
        currentPet.temperament?.let { details += "<div class=\"detail-item\"><strong>${I18n.t("temperament")}:</strong> $it</div>" }
        currentPet.energyLevel?.let { details += "<div class=\"detail-item\"><strong>${I18n.t("energyLevel")}:</strong> $it</div>" }
        details += "</div>"

        details += "<div class=\"pet-details-grid\">"
        details += "<div class=\"detail-item\"><strong>${I18n.t("sterilized")}:</strong> ${if (currentPet.isSterilized == true) "Yes" else "No"}</div>"
        details += "<div class=\"detail-item\"><strong>${I18n.t("microchipped")}:</strong> ${if (currentPet.isMicrochipped == true) "Yes" else "No"}</div>"
        currentPet.microchipId?.let { details += "<div class=\"detail-item\"><strong>${I18n.t("microchipId")}:</strong> $it</div>" }
        details += "</div>"

        details += "<div class=\"pet-details-grid\">"
        details += "<div class=\"detail-item\"><strong>${I18n.t("goodWithKids")}:</strong> ${if (currentPet.isGoodWithKids == true) "Yes" else "No"}</div>"
        details += "<div class=\"detail-item\"><strong>${I18n.t("goodWithDogs")}:</strong> ${if (currentPet.isGoodWithDogs == true) "Yes" else "No"}</div>"
        details += "<div class=\"detail-item\"><strong>${I18n.t("goodWithCats")}:</strong> ${if (currentPet.isGoodWithCats == true) "Yes" else "No"}</div>"
        details += "<div class=\"detail-item\"><strong>${I18n.t("houseTrained")}:</strong> ${if (currentPet.isHouseTrained == true) "Yes" else "No"}</div>"
        details += "</div>"

        currentPet.vaccinations?.let { details += "<div class=\"detail-section\"><strong>${I18n.t("vaccinations")}:</strong><p>$it</p></div>" }
        currentPet.rescueLocation?.let { details += "<div class=\"detail-section\"><strong>${I18n.t("rescueLocation")}:</strong> $it</div>" }
        currentPet.specialNeeds?.let { details += "<div class=\"detail-section\"><strong>${I18n.t("specialNeeds")}:</strong><p>$it</p></div>" }

        val currencySymbols = mapOf("USD" to "$", "EUR" to "\u20AC", "GBP" to "\u00A3", "CAD" to "C$", "AUD" to "A$")
        if (currentPet.adoptionFee > 0) {
            val sym = currencySymbols[currentPet.currency] ?: "$"
            details += "<div class=\"detail-section\"><strong>${I18n.t("adoptionFee")}:</strong> $sym${currentPet.adoptionFee} ${currentPet.currency}</div>"
        }
        if (currentPet.isUrgent == true) details += "<div class=\"urgent-badge\">URGENT - Needs home soon!</div>"

        details += adoptForm + editBtn + "</div>"
        container.innerHTML = details

        (document.getElementById("adopt-form") as? HTMLFormElement)?.onsubmit = { e ->
            e.preventDefault()
            if (user.id == null) { window.location.href = "/login"; return@onsubmit undefined }
            val msg = document.getElementById("message") as? HTMLElement
            val message = (document.getElementById("msg") as? HTMLTextAreaElement)?.value ?: ""
            ApiClient.adoptPet(id, message).then {
                msg?.className = "message success"
                msg?.textContent = "Adoption request submitted!"
                (document.getElementById("adopt-form") as? HTMLElement)?.style?.display = "none"
                undefined
            }.catch { err ->
                msg?.className = "message error"
                msg?.textContent = err.asDynamic().message
                undefined
            }
            undefined
        }
    }

    private fun getPrimaryImage(pet: dynamic): dynamic? {
        if (pet.images == null || pet.images.length == 0) return null
        for (i in 0 until pet.images.length) {
            if (pet.images[i].isPrimary) return pet.images[i]
        }
        return pet.images[0]
    }

    private fun renderImages(): String {
        if (currentPet.images == null || currentPet.images.length == 0) return "<p>${I18n.t("noPhotos")}</p>"
        var html = ""
        for (i in 0 until currentPet.images.length) {
            val img = currentPet.images[i]
            html += "<div class=\"pet-image-item${if (img.isPrimary) " primary" else ""}\">" +
                "<img src=\"${img.imageUrl}\" alt=\"Pet photo\">" +
                (if (img.isPrimary) "<span class=\"primary-badge\">Primary</span>" else "") +
                "</div>"
        }
        return html
    }
}

object PetFoodPage {
    suspend fun init() {
        showFoodInfo("DOG")
        document.querySelectorAll(".pet-type-btn").forEach { btn ->
            (btn as HTMLElement).onclick = {
                document.querySelectorAll(".pet-type-btn").forEach { b -> (b as HTMLElement).classList.remove("active") }
                (btn as HTMLElement).classList.add("active")
                showFoodInfo(btn.getAttribute("data-type") ?: "DOG")
                undefined
            }
        }
    }

    private fun showFoodInfo(petType: String) {
        val container = document.getElementById("food-info") ?: return
        val foodData = getFoodData(petType) ?: return
        var html = ""
        foodData.forEach { cat ->
            html += "<div class=\"food-category\"><h3>${cat.title}</h3><ul class=\"food-list\">"
            cat.items.forEach { item ->
                html += "<li><strong>${item.name}</strong>"
                if (item.detail.isNotEmpty()) html += " <span class=\"food-detail\">(${item.detail})</span>"
                if (item.description.isNotEmpty()) html += "<p class=\"food-desc\">${item.description}</p>"
                html += "</li>"
            }
            html += "</ul></div>"
        }
        container.innerHTML = html
    }

    private data class FoodItem(val name: String, val detail: String, val description: String)
    private data class FoodCategory(val type: String, val title: String, val items: List<FoodItem>)

    private fun getFoodData(petType: String): List<FoodCategory>? = when (petType) {
        "DOG" -> listOf(
            FoodCategory("safe", "Safe Foods", listOf(
                FoodItem("Chicken", "Cooked, boneless", "Lean protein, easy to digest"),
                FoodItem("Rice", "White or brown", "Good source of carbohydrates"),
                FoodItem("Carrots", "Raw or cooked", "Low in calories, high in fiber"),
                FoodItem("Apples", "Without seeds", "Rich in vitamins"),
                FoodItem("Peanut Butter", "Unsalted, no xylitol", "Good protein source")
            )),
            FoodCategory("harmful", "Harmful Foods", listOf(
                FoodItem("Grapes", "All varieties", "Can cause kidney failure"),
                FoodItem("Onions", "All forms", "Damages red blood cells"),
                FoodItem("Chocolate", "All types", "Toxic to dogs"),
                FoodItem("Garlic", "All forms", "Causes anemia")
            )),
            FoodCategory("cannot", "Cannot Eat", listOf(
                FoodItem("Xylitol", "Artificial sweetener", "Highly toxic"),
                FoodItem("Macadamia Nuts", "All forms", "Causes weakness"),
                FoodItem("Avocado", "Pit, skin, all", "Contains persin"),
                FoodItem("Alcohol", "All forms", "Highly toxic")
            ))
        )
        "CAT" -> listOf(
            FoodCategory("safe", "Safe Foods", listOf(
                FoodItem("Cooked Fish", "Without bones", "High in protein"),
                FoodItem("Chicken", "Cooked, plain", "Good protein source"),
                FoodItem("Pumpkin", "Plain, cooked", "Helps digestion"),
                FoodItem("Eggs", "Cooked", "Complete protein")
            )),
            FoodCategory("harmful", "Harmful Foods", listOf(
                FoodItem("Raw Eggs", "With avidin", "Interferes with biotin"),
                FoodItem("Raw Fish", "Contains thiaminase", "Breaks down B vitamins"),
                FoodItem("Dog Food", "Any", "Lacks taurine"),
                FoodItem("Milk", "Most cats", "Lactose intolerance")
            )),
            FoodCategory("cannot", "Cannot Eat", listOf(
                FoodItem("Chocolate", "All types", "Theobromine toxic"),
                FoodItem("Onions/Garlic", "All forms", "Damages RBC"),
                FoodItem("Grapes/Raisins", "All varieties", "Kidney damage"),
                FoodItem("Alcohol", "All forms", "Very toxic")
            ))
        )
        else -> null
    }
}

object PhotographersPage {
    suspend fun init() {
        createRequestModal()
        (document.getElementById("search-btn") as? HTMLElement)?.onclick = { searchPhotographers(); undefined }
        listOf("search-state", "search-city", "search-zip", "search-neighborhood").forEach { id ->
            (document.getElementById(id) as? HTMLInputElement)?.addEventListener("input", { debounce(::searchPhotographers, 500) })
        }
        (document.getElementById("search-country") as? HTMLSelectElement)?.onchange = { searchPhotographers(); undefined }
    }

    private fun searchPhotographers() {
        val country = (document.getElementById("search-country") as? HTMLSelectElement)?.value ?: ""
        if (country.isEmpty()) {
            (document.getElementById("photographers") as? HTMLElement)?.innerHTML = "<p>${I18n.t("pleaseSelectCountry")}</p>"
            return
        }
        val params = js("new URLSearchParams()")
        params.append("country", country)
        listOf("state", "city", "zip", "neighborhood").forEach { key ->
            val v = (document.getElementById("search-$key") as? HTMLElement)?.asDynamic()?.value?.toString()
            if (!v.isNullOrEmpty()) params.append(key, v)
        }
        loadPhotographers("/api/photographers?" + params.toString())
    }

    private fun loadPhotographers(url: String) {
        fetch(url).then { res ->
            if (!res.ok) throw Error("Failed")
            res.json().then { photographers ->
                val container = document.getElementById("photographers") as? HTMLElement ?: return@then undefined
                if (photographers.length == 0) {
                    container.innerHTML = "<p>${I18n.t("noPhotographersAvailable")}</p>"
                    return@then undefined
                }
                var html = ""
                for (i in 0 until photographers.length) {
                    val p = photographers[i]
                    val fee = if (p.photographerFee != null) "${p.photographerFee} ${p.photographerCurrency ?: "USD"}" else "Free"
                    val location = listOf(p.photographerCity, p.photographerState, p.photographerCountry).filter { it != null }.joinToString(", ")
                    html += "<div class=\"photographer-card\"><div class=\"photographer-info\"><h3>${p.displayName}</h3>"
                    if (location.isNotEmpty()) html += "<p class=\"photographer-location\">$location</p>"
                    html += "<p class=\"photographer-fee\">${I18n.t("sessionFee")}: <strong>$fee</strong></p></div>"
                    html += "<button class=\"btn request-btn\" data-id=\"${p.userId}\" data-name=\"${p.displayName}\" data-fee=\"$fee\">${I18n.t("requestPhotoSession")}</button></div>"
                }
                container.innerHTML = html
                container.querySelectorAll(".request-btn").forEach { btn ->
                    (btn as HTMLElement).onclick = {
                        val photographerId = btn.getAttribute("data-id")!!.toInt()
                        val photographerName = btn.getAttribute("data-name")!!
                        val fee = btn.getAttribute("data-fee")!!
                        showRequestModal(photographerId, photographerName, fee)
                        undefined
                    }
                }
                undefined
            }
        }
    }

    private fun createRequestModal() {
        val modal = document.createElement("div") as HTMLDivElement
        modal.id = "request-modal"
        modal.className = "form-modal"
        modal.style.display = "none"
        modal.innerHTML = """
            <div class="form-modal-content card-bg">
                <h2 id="modal-title">${I18n.t("requestPhotoSession")}</h2>
                <p id="modal-photographer" class="photographer-fee"></p>
                <form id="request-form">
                    <div class="form-group">
                        <label for="request-message">${I18n.t("message")}</label>
                        <textarea id="request-message" rows="4" class="form-control"></textarea>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">${I18n.t("sendRequest")}</button>
                        <button type="button" class="btn" id="cancel-request">${I18n.t("cancel")}</button>
                    </div>
                </form>
            </div>
        """.trimIndent()
        document.body?.appendChild(modal)

        (document.getElementById("cancel-request") as? HTMLElement)?.onclick = { modal.style.display = "none"; undefined }
        modal.onclick = { e -> if (e.target == modal) modal.style.display = "none"; undefined }
        (document.getElementById("request-form") as? HTMLFormElement)?.onsubmit = { e ->
            e.preventDefault()
            val message = (document.getElementById("request-message") as? HTMLTextAreaElement)?.value ?: ""
            val photographerId = modal.getAttribute("data-photographer-id")!!.toInt()
            ApiClient.createPhotographyRequest(photographerId, null, message).then {
                modal.style.display = "none"
                js("alert")(I18n.t("requestSentSuccessfully"))
                undefined
            }.catch { err ->
                js("alert")("Error: ${err.asDynamic().message}")
                undefined
            }
            undefined
        }
    }

    private fun showRequestModal(photographerId: Int, photographerName: String, fee: String) {
        val modal = document.getElementById("request-modal") as? HTMLDivElement ?: return
        (document.getElementById("modal-title") as? HTMLElement)?.textContent = I18n.t("requestPhotoSession")
        (document.getElementById("modal-photographer") as? HTMLElement)?.innerHTML =
            "${I18n.t("requestTo")}: <strong>$photographerName</strong><br>${I18n.t("sessionFee")}: <strong>$fee</strong>"
        (document.getElementById("request-message") as? HTMLTextAreaElement)?.placeholder =
            I18n.t("enterMessage").replace("{name}", photographerName)
        (document.getElementById("request-message") as? HTMLTextAreaElement)?.value = ""
        modal.setAttribute("data-photographer-id", photographerId.toString())
        modal.style.display = "flex"
    }
}

object SheltersPage {
    suspend fun init() {
        (document.getElementById("search-btn") as? HTMLElement)?.onclick = { searchShelters(); undefined }
        listOf("search-state", "search-city", "search-zip", "search-neighborhood").forEach { id ->
            (document.getElementById(id) as? HTMLInputElement)?.addEventListener("input", { debounce(::searchShelters, 500) })
        }
        (document.getElementById("search-country") as? HTMLSelectElement)?.onchange = { searchShelters(); undefined }
    }

    private fun searchShelters() {
        val country = (document.getElementById("search-country") as? HTMLSelectElement)?.value ?: ""
        val errorDiv = document.getElementById("shelters-error") as? HTMLElement
        val container = document.getElementById("shelters") as? HTMLElement ?: return
        errorDiv?.style?.display = "none"
        errorDiv?.textContent = ""
        container.innerHTML = "<p>${I18n.t("loading")}</p>"
        if (country.isEmpty()) {
            errorDiv?.style?.display = "block"
            errorDiv?.textContent = I18n.t("countryRequired")
            container.innerHTML = ""
            return
        }
        val params = js("new URLSearchParams()")
        params.append("country", country)
        listOf("state", "city", "zip", "neighborhood").forEach { key ->
            val v = (document.getElementById("search-$key") as? HTMLElement)?.asDynamic()?.value?.toString()
            if (!v.isNullOrEmpty()) params.append(key, v)
        }
        fetch("/api/shelters?" + params.toString()).then { res ->
            if (!res.ok) throw Error("Failed")
            res.json().then { shelters ->
                if (shelters.length == 0) {
                    container.innerHTML = "<p>${I18n.t("noSheltersFound")}</p>"
                    return@then undefined
                }
                var html = ""
                for (i in 0 until shelters.length) {
                    val s = shelters[i]
                    val location = listOf(s.city, s.state, s.country).filter { it != null }.joinToString(", ")
                    html += "<div class=\"shelter-card\"><h3>${s.name}</h3>"
                    if (location.isNotEmpty()) html += "<p class=\"shelter-location\">$location</p>"
                    html += "<p class=\"shelter-address\">${s.address}${if (s.zip != null) ", ${s.zip}" else ""}</p>"
                    s.phone?.let { html += "<p class=\"shelter-phone\"><strong>${I18n.t("phone")}:</strong> $it</p>" }
                    s.email?.let { html += "<p class=\"shelter-email\"><strong>${I18n.t("email")}:</strong> <a href=\"mailto:$it\">$it</a></p>" }
                    s.website?.let { html += "<p class=\"shelter-website\"><a href=\"$it\" target=\"_blank\">${I18n.t("visitWebsite")}</a></p>" }
                    s.fiscalId?.let { html += "<p class=\"shelter-fiscal-id\"><strong>${I18n.t("fiscalId")}:</strong> $it</p>" }
                    if (s.bankName != null || s.accountNumber != null || s.iban != null) {
                        html += "<div class=\"shelter-donation-info\"><h4>${I18n.t("donationInformation")}</h4>"
                        s.bankName?.let { html += "<p><strong>${I18n.t("bankName")}:</strong> $it</p>" }
                        s.accountHolderName?.let { html += "<p><strong>${I18n.t("accountHolder")}:</strong> $it</p>" }
                        s.accountNumber?.let { html += "<p><strong>${I18n.t("accountNumber")}:</strong> $it</p>" }
                        s.iban?.let { html += "<p><strong>IBAN:</strong> $it</p>" }
                        s.swiftBic?.let { html += "<p><strong>SWIFT/BIC:</strong> $it</p>" }
                        s.currency?.let { html += "<p><strong>${I18n.t("currency")}:</strong> $it</p>" }
                        html += "</div>"
                    }
                    s.description?.let { html += "<p class=\"shelter-description\">$it</p>" }
                    html += "</div>"
                }
                container.innerHTML = html
                undefined
            }
        }.catch {
            errorDiv?.style?.display = "block"
            errorDiv?.textContent = I18n.t("errorLoadingShelters")
            container.innerHTML = ""
        }
    }
}

object SterilizationPage {
    suspend fun init() {
        (document.getElementById("search-btn") as? HTMLElement)?.onclick = { searchLocations(); undefined }
        listOf("search-state", "search-city", "search-zip", "search-neighborhood").forEach { id ->
            (document.getElementById(id) as? HTMLInputElement)?.addEventListener("input", { debounce(::searchLocations, 500) })
        }
        (document.getElementById("search-country") as? HTMLSelectElement)?.onchange = { searchLocations(); undefined }
    }

    private fun searchLocations() {
        val country = (document.getElementById("search-country") as? HTMLSelectElement)?.value ?: ""
        if (country.isEmpty()) {
            (document.getElementById("locations-container") as? HTMLElement)?.innerHTML = "<p>${I18n.t("pleaseSelectCountry")}</p>"
            return
        }
        val params = js("new URLSearchParams()")
        params.append("country", country)
        listOf("state", "city", "zip", "neighborhood").forEach { key ->
            val v = (document.getElementById("search-$key") as? HTMLElement)?.asDynamic()?.value?.toString()
            if (!v.isNullOrEmpty()) params.append(key, v)
        }
        fetch("/api/sterilization-locations?" + params.toString()).then { res ->
            res.json().then { locations ->
                val container = document.getElementById("locations-container") as? HTMLElement ?: return@then undefined
                if (locations.length == 0) {
                    container.innerHTML = "<p>${I18n.t("noLocationsFound")}</p>"
                    return@then undefined
                }
                var html = "<div class=\"location-list\">"
                for (i in 0 until locations.length) {
                    val loc = locations[i]
                    html += "<div class=\"location-card card-bg\"><h3>${loc.name}</h3>"
                    html += "<p class=\"location-address\">${loc.address}, ${loc.city}${if (loc.state != null) ", ${loc.state}" else ""}, ${loc.country}</p>"
                    loc.phone?.let { html += "<p class=\"location-phone\"><strong>${I18n.t("phone")}:</strong> $it</p>" }
                    loc.email?.let { html += "<p class=\"location-email\"><strong>${I18n.t("email")}:</strong> $it</p>" }
                    loc.website?.let { html += "<p class=\"location-website\"><a href=\"$it\" target=\"_blank\">${I18n.t("website")}</a></p>" }
                    loc.description?.let { html += "<p class=\"location-description\">$it</p>" }
                    html += "</div>"
                }
                html += "</div>"
                container.innerHTML = html
                undefined
            }
        }.catch {
            (document.getElementById("locations-container") as? HTMLElement)?.innerHTML = "<p>${I18n.t("errorLoadingLocations")}</p>"
        }
    }
}

object TemporalHomePage {
    suspend fun init() {
        val path = window.location.pathname
        if (path.startsWith("/temporal-home/")) {
            val id = path.split("/").last()
            loadTemporalHomeDetail(id)
        }
    }

    private fun loadTemporalHomeDetail(id: String) {
        fetch("/api/temporal-homes/$id").then { res ->
            res.json().then { th ->
                val container = document.getElementById("temporal-home-detail") ?: return@then undefined
                var html = "<div class=\"temporal-home-card\"><h2>${th.alias ?: "Temporal Home"}</h2>"
                html += "<p>${listOf(th.city, th.state, th.country).filter { it != null }.joinToString(", ")}</p>"
                if (th.neighborhood != null) html += "<p>Neighborhood: ${th.neighborhood}</p>"
                if (th.zip != null) html += "<p>ZIP: ${th.zip}</p>"
                html += "</div>"
                container.innerHTML = html
                undefined
            }
        }
    }
}

fun blockRescuerAndRedirect(thId: String, rId: String) {
    fetch("/api/temporal-homes/block/$thId?rescuer=$rId").then { res ->
        res.json().then { data ->
            if (data.blocked == true) {
                document.body?.innerHTML = "<h1>Rescuer blocked!</h1><p>You will no longer receive requests from this rescuer.</p><a href=\"/\">Go to Home</a>"
            } else {
                document.body?.innerHTML = "<h1>This rescuer was already blocked.</h1><a href=\"/\">Go to Home</a>"
            }
            undefined
        }
    }.catch { document.body?.innerHTML = "<h1>Error blocking rescuer</h1>" }
}

object TemporalHomeProfilePage {
    suspend fun init() {
        try {
            val user = ApiClient.me()
            if (user.authenticated == false) { window.location.href = "/login"; return }
            if (!user.activeRoles.asList().contains("TEMPORAL_HOME") && !user.activeRoles.asList().contains("ADMIN")) {
                window.location.href = "/"; return
            }
        } catch (e: Throwable) { window.location.href = "/login"; return }

        loadRequests()
        js("window.blockRescuer = function(rescuerId) { AdoptuTemporalHomeProfile.blockRescuer(rescuerId); }")
    }

    private fun loadRequests() {
        fetch("/api/users/temporal-home/requests").then { res ->
            if (!res.ok) throw Error("Failed")
            res.json().then { requests ->
                val container = document.getElementById("requests-container") as? HTMLElement ?: return@then undefined
                if (requests.length == 0) {
                    container.innerHTML = "<p>No requests yet.</p>"
                    return@then undefined
                }
                var html = ""
                for (i in 0 until requests.length) {
                    val r = requests[i]
                    html += "<div class=\"request-card\"><p><strong>${r.rescuerName}</strong> wants help with ${r.petName ?: "a pet"}</p>"
                    html += "<p>${r.message}</p>"
                    html += "<button class=\"btn btn-small\" onclick=\"blockRescuer(${r.rescuerId})\">Block Rescuer</button></div>"
                }
                container.innerHTML = html
                undefined
            }
        }
    }

    fun blockRescuer(rescuerId: String) {
        if (js("confirm")("Block this rescuer from sending you more requests?") != true) return
        ApiClient.blockRescuer(rescuerId).then { result ->
            js("alert")(if (result.blocked == true) "Rescuer blocked!" else "Already blocked")
            loadRequests()
            undefined
        }.catch { err ->
            js("alert")(err.asDynamic().message)
            undefined
        }
    }
}

object TemporalHomeSearchPage {
    suspend fun init() {
        js("window.searchTemporalHomes = function() { AdoptuTemporalHomeSearch.search(); }")
        js("window.displayResults = function(homes) { AdoptuTemporalHomeSearch.displayResults(homes); }")
        (document.getElementById("search-btn") as? HTMLElement)?.onclick = { search(); undefined }
        (document.getElementById("search-state") as? HTMLInputElement)?.addEventListener("input", { debounce(::search, 500) })
    }

    fun search() {
        val params = js("new URLSearchParams()")
        listOf("country", "state", "city", "zip", "neighborhood").forEach { key ->
            val v = (document.getElementById("search-$key") as? HTMLElement)?.asDynamic()?.value?.toString()
            if (!v.isNullOrEmpty()) params.append(key, v)
        }
        fetch("/api/temporal-homes?" + params.toString()).then { res ->
            if (!res.ok) throw Error("Search failed")
            res.json().then { homes ->
                displayResults(homes)
                undefined
            }
        }.catch {
            (document.getElementById("results-container") as? HTMLElement)?.innerHTML = "<p>Error loading results.</p>"
        }
    }

    fun displayResults(homes: dynamic) {
        val container = document.getElementById("results-container") as? HTMLElement ?: return
        if (homes.length == 0) {
            container.innerHTML = "<p data-i18n=\"noTemporalHomes\">${I18n.t("noTemporalHomes")}</p>"
            return
        }
        var html = ""
        for (i in 0 until homes.length) {
            val home = homes[i]
            html += "<div class=\"temporal-home-card\"><h3>${home.alias ?: "Temporal Home"}</h3>"
            html += "<p>${listOf(home.city, home.state, home.country).filter { it != null }.joinToString(", ")}</p>"
            html += "<a href=\"/temporal-home/${home.id}\">${I18n.t("viewDetails")}</a></div>"
        }
        container.innerHTML = html
    }
}

object AdminSheltersPage {
    private var editingId: String? = null

    suspend fun init() {
        (document.getElementById("filter-country") as? HTMLSelectElement)?.onchange = { loadShelters(); undefined }
        (document.getElementById("filter-state") as? HTMLSelectElement)?.onchange = { loadShelters(); undefined }
        (document.getElementById("shelter-form") as? HTMLFormElement)?.onsubmit = { e ->
            e.preventDefault()
            saveShelter()
            undefined
        }
        (document.getElementById("cancel-btn") as? HTMLElement)?.onclick = { cancelEdit(); undefined }
        js("window.editShelter = function(id) { AdoptuAdminShelters.editShelter(id); }")
        js("window.deleteShelter = function(id) { AdoptuAdminShelters.deleteShelter(id); }")
        loadShelters()
    }

    private fun loadShelters() {
        val country = (document.getElementById("filter-country") as? HTMLSelectElement)?.value ?: ""
        val state = (document.getElementById("filter-state") as? HTMLSelectElement)?.value ?: ""
        val container = document.getElementById("shelters") as? HTMLElement ?: return
        val stateSelect = document.getElementById("filter-state") as? HTMLSelectElement

        if (country.isEmpty()) {
            container.innerHTML = "<p>${I18n.t("selectCountryToFilter")}</p>"
            return
        }

        stateSelect?.innerHTML = "<option value=\"\">${I18n.t("allStates")}</option>"
        fetch("/api/shelters/countries/$country/states").then { res ->
            if (res.ok) {
                res.json().then { data ->
                    if (data.states != null) {
                        for (i in 0 until data.states.length) {
                            val option = document.createElement("option") as HTMLOptionElement
                            option.value = data.states[i]
                            option.textContent = data.states[i]
                            stateSelect.appendChild(option)
                        }
                    }
                    undefined
                }
            } else undefined
        }

        val params = js("new URLSearchParams()")
        params.append("country", country)
        if (state.isNotEmpty()) params.append("state", state)

        fetch("/api/shelters?" + params.toString()).then { res ->
            if (!res.ok) throw Error("Failed")
            res.json().then { shelters ->
                if (shelters.length == 0) {
                    container.innerHTML = "<p>${I18n.t("noSheltersFound")}</p>"
                    return@then undefined
                }
                var html = "<table class=\"admin-table\"><thead><tr><th>${I18n.t("name")}</th><th>${I18n.t("location")}</th><th>${I18n.t("contact")}</th><th>${I18n.t("actions")}</th></tr></thead><tbody>"
                for (i in 0 until shelters.length) {
                    val s = shelters[i]
                    html += "<tr><td><strong>${escapeHtml(s.name)}</strong></td>"
                    html += "<td>${escapeHtml(s.city ?: "")}, ${escapeHtml(s.state ?: "")}, ${escapeHtml(s.country ?: "")}</td>"
                    html += "<td>${escapeHtml(s.phone ?: s.email ?: "-")}</td>"
                    html += "<td><button class=\"btn btn-secondary\" onclick=\"editShelter(${s.id})\">${I18n.t("edit")}</button> "
                    html += "<button class=\"btn btn-danger\" onclick=\"deleteShelter(${s.id})\">${I18n.t("delete")}</button></td></tr>"
                }
                html += "</tbody></table>"
                container.innerHTML = html
                undefined
            }
        }
    }

    private fun saveShelter() {
        val id = (document.getElementById("shelter-id") as? HTMLInputElement)?.value
        val data = getFormData()
        val method = if (id != null) "PUT" else "POST"
        val url = if (id != null) "/api/admin/shelters/$id" else "/api/admin/shelters"
        fetch(url, js("({method: method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)})")).then { res ->
            if (!res.ok) {
                res.json().then { error -> throw Error(error.message ?: "Failed") }
            }
            showMessage(if (id != null) I18n.t("shelterUpdated") else I18n.t("shelterAdded"), "success")
            clearForm()
            loadShelters()
            undefined
        }.catch { err ->
            showMessage(err.asDynamic().message ?: I18n.t("errorSavingShelter"), "error")
        }
    }

    private fun getFormData(): dynamic {
        val data = js("({})")
        listOf("name", "country", "city", "address").forEach { id ->
            data[id] = (document.getElementById(id) as? HTMLInputElement)?.value
        }
        listOf("state", "zip", "phone", "email", "website", "fiscalId", "bankName", "accountHolderName", "accountNumber", "iban", "swiftBic", "description").forEach { id ->
            val v = (document.getElementById(id) as? HTMLInputElement)?.value
            data[id] = if (v.isNullOrEmpty()) null else v
        }
        data.currency = (document.getElementById("currency") as? HTMLSelectElement)?.value
        return data
    }

    private fun clearForm() {
        (document.getElementById("shelter-form") as? HTMLFormElement)?.reset()
        (document.getElementById("shelter-id") as? HTMLInputElement)?.value = ""
        (document.getElementById("submit-btn") as? HTMLElement)?.textContent = I18n.t("addShelter")
        (document.getElementById("cancel-btn") as? HTMLElement)?.style?.display = "none"
        editingId = null
    }

    fun cancelEdit() { clearForm() }

    fun editShelter(id: String) {
        editingId = id
        fetch("/api/shelters/$id").then { res ->
            if (!res.ok) throw Error("Failed")
            res.json().then { shelter ->
                (document.getElementById("shelter-id") as? HTMLInputElement)?.value = id
                listOf("name", "country", "city", "address").forEach { field ->
                    (document.getElementById(field) as? HTMLInputElement)?.value = shelter[field]?.toString() ?: ""
                }
                listOf("state", "zip", "phone", "email", "website", "fiscalId", "bankName", "accountHolderName", "accountNumber", "iban", "swiftBic", "description").forEach { field ->
                    (document.getElementById(field) as? HTMLInputElement)?.value = shelter[field]?.toString() ?: ""
                }
                (document.getElementById("currency") as? HTMLSelectElement)?.value = shelter.currency ?: "USD"
                (document.getElementById("submit-btn") as? HTMLElement)?.textContent = I18n.t("updateShelter")
                (document.getElementById("cancel-btn") as? HTMLElement)?.style?.display = "inline-block"
                (document.getElementById("shelter-form") as? HTMLElement)?.scrollIntoView(js("({behavior: 'smooth'})"))
                undefined
            }
        }
    }

    fun deleteShelter(id: String) {
        if (js("confirm")(I18n.t("confirmDeleteShelter")) != true) return
        fetch("/api/admin/shelters/$id", js("({method: 'DELETE'})")).then { res ->
            if (!res.ok) throw Error("Failed")
            showMessage(I18n.t("shelterDeleted"), "success")
            loadShelters()
            undefined
        }.catch { err ->
            showMessage(I18n.t("errorDeletingShelter"), "error")
        }
    }

    private fun showMessage(msg: String, type: String) {
        val div = document.getElementById("message") as? HTMLElement ?: return
        div.textContent = msg
        div.className = if (type == "error") "error-message" else "success-message"
        div.style.display = "block"
    }

    private fun escapeHtml(s: String?): String {
        if (s == null) return ""
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    }
}

object AdminSterilizationLocationsPage {
    private var editingId: String? = null

    suspend fun init() {
        (document.getElementById("location-form") as? HTMLFormElement)?.onsubmit = { e ->
            e.preventDefault()
            saveLocation()
            undefined
        }
        js("window.editLocation = function(id) { AdoptuAdminSterilizationLocations.editLocation(id); }")
        js("window.deleteLocation = function(id) { AdoptuAdminSterilizationLocations.deleteLocation(id); }")
        js("window.showForm = function() { AdoptuAdminSterilizationLocations.showForm(); }")
        loadCountries()
        loadLocations()
    }

    private fun loadCountries() {
        fetch("/api/sterilization-locations/countries").then { res ->
            res.json().then { data ->
                val select = document.getElementById("form-country") as? HTMLSelectElement ?: return@then undefined
                var html = "<option value=\"\">${I18n.t("selectCountry")}</option>"
                for (i in 0 until data.countries.length) {
                    html += "<option value=\"${data.countries[i]}\">${data.countries[i]}</option>"
                }
                select.innerHTML = html
                undefined
            }
        }
    }

    private fun loadLocations() {
        fetch("/api/admin/sterilization-locations").then { res ->
            res.json().then { locations ->
                val container = document.getElementById("locations-container") as? HTMLElement ?: return@then undefined
                if (locations.length == 0) {
                    container.innerHTML = "<p>${I18n.t("noLocationsFound")}</p>"
                    return@then undefined
                }
                var html = "<div class=\"location-list\">"
                for (i in 0 until locations.length) {
                    val loc = locations[i]
                    html += "<div class=\"location-card card-bg\"><h3>${escapeHtml(loc.name)}</h3>"
                    html += "<p class=\"location-address\">${escapeHtml(loc.address)}, ${escapeHtml(loc.city)}${if (loc.state != null) ", ${escapeHtml(loc.state)}" else ""}, ${escapeHtml(loc.country)}</p>"
                    loc.phone?.let { html += "<p class=\"location-phone\"><strong>${I18n.t("phone")}:</strong> ${escapeHtml(it)}</p>" }
                    loc.email?.let { html += "<p class=\"location-email\"><strong>${I18n.t("email")}:</strong> ${escapeHtml(it)}</p>" }
                    loc.website?.let { html += "<p class=\"location-website\"><a href=\"${escapeHtml(it)}\" target=\"_blank\">${I18n.t("website")}</a></p>" }
                    loc.description?.let { html += "<p class=\"location-description\">${escapeHtml(it)}</p>" }
                    html += "<div class=\"pet-card-actions\">"
                    html += "<button class=\"btn\" onclick=\"editLocation(${loc.id})\">${I18n.t("edit")}</button>"
                    html += "<button class=\"btn btn-danger\" onclick=\"deleteLocation(${loc.id})\">${I18n.t("delete")}</button>"
                    html += "</div></div>"
                }
                html += "</div>"
                container.innerHTML = html
                undefined
            }
        }
    }

    fun showForm() {
        editingId = null
        (document.getElementById("location-form") as? HTMLFormElement)?.reset()
        (document.getElementById("form-modal") as? HTMLElement)?.style?.display = "flex"
        loadCountries()
    }

    fun editLocation(id: String) {
        editingId = id
        fetch("/api/sterilization-locations/$id").then { res ->
            res.json().then { loc ->
                loadCountries()
                (document.getElementById("location-form") as dynamic).name.value = loc.name
                (document.getElementById("form-country") as? HTMLSelectElement)?.value = loc.country
                listOf("state", "city", "address", "zip", "phone", "email", "website", "description").forEach { field ->
                    (document.getElementById("location-form") as dynamic)[field].value = loc[field]?.toString() ?: ""
                }
                (document.getElementById("form-modal") as? HTMLElement)?.style?.display = "flex"
                undefined
            }
        }
    }

    fun deleteLocation(id: String) {
        if (js("confirm")(I18n.t("confirmDelete")) != true) return
        fetch("/api/admin/sterilization-locations/$id", js("({method: 'DELETE'})")).then { loadLocations(); undefined }
    }

    private fun saveLocation() {
        val form = document.getElementById("location-form") as dynamic
        val data = js("({})")
        data.name = form.name.value
        data.country = form.country.value
        listOf("state", "city", "address", "zip", "phone", "email", "website", "description").forEach { field ->
            val v = form[field].value
            data[field] = if (v.isEmpty()) null else v
        }
        val method = if (editingId != null) "PUT" else "POST"
        val url = if (editingId != null) "/api/admin/sterilization-locations/$editingId" else "/api/admin/sterilization-locations"
        fetch(url, js("({method: method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)})")).then { res ->
            if (res.ok) {
                (document.getElementById("form-modal") as? HTMLElement)?.style?.display = "none"
                editingId = null
                loadLocations()
            } else {
                (document.getElementById("message") as? HTMLElement)?.innerHTML = "<div class=\"message error\">${I18n.t("errorSaving")}</div>"
            }
            undefined
        }
    }

    private fun escapeHtml(s: String?): String {
        if (s == null) return ""
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    }
}

object ForgotPasswordPage {
    suspend fun init() {
        (document.getElementById("submit-btn") as? HTMLElement)?.onclick = {
            val msg = document.getElementById("message") as? HTMLElement ?: return@onclick undefined
            val email = (document.getElementById("email") as? HTMLInputElement)?.value ?: ""
            if (email.isEmpty()) {
                msg.className = "message error"
                msg.textContent = "Email is required"
                return@onclick undefined
            }
            msg.textContent = "Sending..."
            msg.className = ""
            RsaCrypto.getPublicKey().then { key ->
                RsaCrypto.encrypt(email, key.toString()).then { encrypted ->
                    fetch("/api/auth/forgot-password", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({encryptedData: encrypted})})")).then { res ->
                        res.json().then { result ->
                            if (result.success == true) {
                                msg.className = "message success"
                                msg.textContent = "Password reset link sent! Check your email."
                                (document.getElementById("email") as? HTMLInputElement)?.value = ""
                            } else {
                                msg.className = "message error"
                                msg.textContent = result.error ?: "Failed to send reset link."
                            }
                            undefined
                        }
                    }
                }
            }.catch {
                msg.className = "message error"
                msg.textContent = "Failed to send reset link."
            }
            undefined
        }
    }
}

object ResetPasswordPage {
    suspend fun init() {
        val token = getTokenFromUrl()
        if (token == null) {
            (document.getElementById("message") as? HTMLElement)?.let {
                it.className = "message error"
                it.textContent = "Invalid or missing token."
            }
            (document.getElementById("submit-btn") as? HTMLElement)?.asDynamic()?.disabled = true
            return
        }

        (document.getElementById("submit-btn") as? HTMLElement)?.onclick = {
            val msg = document.getElementById("message") as? HTMLElement ?: return@onclick undefined
            val password = (document.getElementById("password") as? HTMLInputElement)?.value ?: ""
            val confirmPassword = (document.getElementById("confirm-password") as? HTMLInputElement)?.value ?: ""

            if (password.length < 8) {
                msg.className = "message error"
                msg.textContent = I18n.t("passwordTooShort")
                return@onclick undefined
            }
            if (!Regex("[A-Z]").containsMatchIn(password)) {
                msg.className = "message error"
                msg.textContent = I18n.t("passwordNeedUppercase")
                return@onclick undefined
            }
            if (!Regex("[a-z]").containsMatchIn(password)) {
                msg.className = "message error"
                msg.textContent = I18n.t("passwordNeedLowercase")
                return@onclick undefined
            }
            if (!Regex("[0-9]").containsMatchIn(password)) {
                msg.className = "message error"
                msg.textContent = I18n.t("passwordNeedNumber")
                return@onclick undefined
            }
            if (!Regex("[!@#\$%^&*(),.?\":{}|<>\\-_+=()\\[\\]\\\\|°º«»¿]").containsMatchIn(password)) {
                msg.className = "message error"
                msg.textContent = I18n.t("passwordNeedSymbol")
                return@onclick undefined
            }
            if (password != confirmPassword) {
                msg.className = "message error"
                msg.textContent = I18n.t("passwordsDoNotMatch")
                return@onclick undefined
            }

            msg.textContent = "Resetting password..."
            msg.className = ""
            RsaCrypto.getPublicKey().then { key ->
                RsaCrypto.encrypt(password, key.toString()).then { encrypted ->
                    fetch("/api/auth/reset-password?token=${js("encodeURIComponent")(token)}", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({encryptedData: encrypted})})")).then { res ->
                        res.json().then { result ->
                            if (result.success == true) {
                                msg.className = "message success"
                                msg.textContent = "Password reset successfully! You can now login."
                                (document.getElementById("password") as? HTMLInputElement)?.value = ""
                                (document.getElementById("confirm-password") as? HTMLInputElement)?.value = ""
                            } else {
                                msg.className = "message error"
                                msg.textContent = result.error ?: "Failed to reset password."
                            }
                            undefined
                        }
                    }
                }
            }.catch {
                msg.className = "message error"
                msg.textContent = "Failed to reset password."
            }
            undefined
        }
    }

    private fun getTokenFromUrl(): String? {
        return js("new URLSearchParams")(window.location.search).get("token")
    }
}

object MagicLinkLoginPage {
    suspend fun init() {
        val params = js("new URLSearchParams")(window.location.search)
        val token = params.get("token")
        val msg = document.getElementById("message") as? HTMLElement ?: return

        if (token == null) {
            msg.className = "message error"
            msg.textContent = "Invalid or missing token."
            return
        }

        fetch("/api/auth/magic-link-login?token=${js("encodeURIComponent")(token)}").then { res ->
            res.json().then { result ->
                if (result.success == true) {
                    msg.className = "message success"
                    msg.textContent = "Login successful! Redirecting..."
                    js("setTimeout")(suspend { window.location.href = "/" }, 1000)
                } else {
                    msg.className = "message error"
                    msg.textContent = result.error ?: "Login failed. The link may be invalid or expired."
                }
                undefined
            }
        }.catch {
            msg.className = "message error"
            msg.textContent = "Login failed."
        }
    }
}

object EmailVerificationPage {
    private var seconds = 10

    suspend fun init() {
        val countdownEl = document.getElementById("countdown") as? HTMLElement
        js("setInterval")(suspend {
            seconds--
            countdownEl?.textContent = seconds.toString()
            if (seconds <= 0) {
                window.location.href = "/"
            }
        }, 1000)
    }
}

object EmailChangeVerificationPage {
    suspend fun init() {
        val params = js("new URLSearchParams")(window.location.search)
        val token = params.get("token")
        val msg = document.getElementById("message") as? HTMLElement ?: return

        if (token == null) {
            msg.className = "message error"
            msg.textContent = "Invalid or missing token."
            return
        }

        fetch("/api/users/verify-email-change?token=${js("encodeURIComponent")(token)}").then { res ->
            res.json().then { result ->
                if (result.success == true) {
                    msg.className = "message success"
                    msg.textContent = result.message ?: "Email changed successfully!"
                } else {
                    msg.className = "message error"
                    msg.textContent = result.message ?: "Failed to change email. The link may be invalid or expired."
                }
                undefined
            }
        }.catch {
            msg.className = "message error"
            msg.textContent = "Failed to change email."
        }
    }
}

external fun fetch(resource: String, init: dynamic = definedExternally): Promise<FetchResponse>
external interface FetchResponse {
    val ok: Boolean
    fun json(): Promise<dynamic>
    fun text(): Promise<String>
}

suspend fun Promise<FetchResponse>.awaitPage(): FetchResponse = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

external object JSON {
    fun stringify(value: dynamic): String
}

external object console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
}

external val undefined: dynamic

class Error(message: String) : Throwable(message)

fun debounce(fn: () -> Unit, wait: Int) {
    js("clearTimeout")(js("window._debounceTimer"))
    js("window._debounceTimer = setTimeout")(suspend { fn() }, wait)
}
