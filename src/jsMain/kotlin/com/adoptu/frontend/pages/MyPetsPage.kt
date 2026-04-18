package com.adoptu.frontend.pages

import com.adoptu.frontend.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import kotlin.js.json

object MyPetsPage {
    private var existingImages = emptyList<dynamic>()
    private var selectedFiles = emptyList<dynamic>()

    suspend fun init() {
        val params = js("new URLSearchParams")(window.location.search)
        val filter = params.get("filter")
        val editId = params.get("edit")

        loadPets(filter, editId)
        setupFormButtons()
        setupDropzone()
        setupFormSubmit()
        setupLogout()
    }

    private fun loadPets(filter: String?, editId: String?) {
        ApiClient.me().then { user ->
            if (user.authenticated == false || (!user.activeRoles.asList().contains("RESCUER") && !user.activeRoles.asList().contains("ADMIN"))) {
                window.location.href = "/"
                return@then undefined
            }

            val url = "/api/pets" + (filter?.let { "?type=$it" } ?: "")
            fetch(url).then { res ->
                res.json().then { pets ->
                    var filteredPets = pets
                    if (!user.activeRoles.asList().contains("ADMIN")) {
                        filteredPets = js("Array.from")(pets).asDynamic().filter { p -> p.rescuerId == user.id }
                    }
                    renderPets(filteredPets)
                    if (editId != null) {
                        ApiClient.getPet(editId).then { pet ->
                            fillForm(pet)
                            (document.getElementById("form-title") as? HTMLElement)?.textContent = "Edit Pet"
                            (document.getElementById("form-container") as? HTMLElement)?.style?.display = "block"
                            undefined
                        }
                    }
                    loadAdoptionRequests(filteredPets)
                    undefined
                }
            }
        }
    }

    private fun renderPets(pets: dynamic) {
        val container = document.getElementById("pets") ?: return
        val emoji = mapOf("DOG" to "\uD83D\uDC15", "CAT" to "\uD83D\uDC31", "BIRD" to "\uD83D\uDC26", "FISH" to "\uD83D\uDC1F")

        if (pets.length == 0) {
            container.innerHTML = "<p>${I18n.t("noPets")}</p>"
            return
        }

        var html = ""
        for (i in 0 until pets.length) {
            val p = pets[i]
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

            html += "<div class=\"pet-card\">$imageHtml<div class=\"pet-card-body\">" +
                "<span class=\"pet-type\">${I18n.t(p.type.toLowerCase())}</span>" +
                "<span class=\"pet-sex $sexClass\">${I18n.t(p.sex.toLowerCase())}</span>" +
                sizeHtml +
                "<div class=\"pet-name\"><h3>${p.name}$urgentBadge</h3>$breedHtml</div>" +
                "<p class=\"pet-info\"><span class=\"pet-age\">${p.ageYears}${I18n.t("years")} ${p.ageMonths}${I18n.t("months")} • ${p.weight} kg</span>" +
                "<span class=\"pet-rescue-date\">${if (p.rescueDate != null) " ${I18n.t("rescued")}: ${js("new Date")(p.rescueDate).toLocaleDateString()}" else ""}</span></p>" +
                "<p>${p.status}</p>" +
                "<div class=\"pet-card-actions\">" +
                "<a href=\"/pet/${p.id}\" class=\"btn\">${I18n.t("viewDetails")}</a>" +
                "<button class=\"btn btn-secondary\" data-edit=\"${p.id}\">${I18n.t("edit")}</button>" +
                "<button class=\"btn btn-secondary\" data-del=\"${p.id}\">${I18n.t("delete")}</button>" +
                "</div></div></div>"
        }
        container.innerHTML = html

        container.querySelectorAll("[data-edit]").forEach { btn ->
            (btn as HTMLElement).onclick = {
                ApiClient.getPet(btn.getAttribute("data-edit")!!).then { pet ->
                    fillForm(pet)
                    (document.getElementById("form-title") as? HTMLElement)?.textContent = "Edit Pet"
                    (document.getElementById("form-container") as? HTMLElement)?.style?.display = "block"
                    undefined
                }
                undefined
            }
        }
        container.querySelectorAll("[data-del]").forEach { btn ->
            (btn as HTMLElement).onclick = {
                if (js("confirm")("Delete this pet?") == true) {
                    ApiClient.deletePet(btn.getAttribute("data-del")!!).then { loadPets(null, null) }
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

    private fun loadAdoptionRequests(pets: dynamic) {
        val container = document.getElementById("adoption-requests") ?: return
        js("window.approveRequest = function(requestId) { AdoptuMyPets.approveRequest(requestId); }")
        js("window.rejectRequest = function(requestId) { AdoptuMyPets.rejectRequest(requestId); }")

        var allRequests = js("[]")
        var pending = pets.length
        if (pending == 0) {
            container.innerHTML = "<p>No adoption requests</p>"
            return
        }
        for (i in 0 until pets.length) {
            val pet = pets[i]
            ApiClient.getAdoptionRequests(pet.id).then { requests ->
                if (requests.length > 0) {
                    for (j in 0 until requests.length) {
                        requests[j].petName = pet.name
                        requests[j].petType = pet.type
                        allRequests.push(requests[j])
                    }
                }
                pending--
                if (pending == 0) {
                    if (allRequests.length == 0) {
                        container.innerHTML = "<p>No adoption requests</p>"
                    } else {
                        var html = ""
                        for (k in 0 until allRequests.length) {
                            val r = allRequests[k]
                            val emoji = mapOf("DOG" to "\uD83D\uDC15", "CAT" to "\uD83D\uDC31", "BIRD" to "\uD83D\uDC26", "FISH" to "\uD83D\uDC1F")
                            html += "<div class=\"adoption-request-card\">" +
                                "<div class=\"ar-pet\">${emoji[r.petType] ?: "\uD83D\uDC3E"} ${r.petName}</div>" +
                                "<div class=\"ar-status status-${r.status.toLowerCase()}\">${r.status}</div>" +
                                "<div class=\"ar-message\">${r.message ?: "No message"}</div>" +
                                "<div class=\"ar-date\">${js("new Date")(r.createdAt).toLocaleDateString()}</div>" +
                                (if (r.status == "PENDING") "<div class=\"ar-actions\"><button class=\"btn btn-secondary\" onclick=\"approveRequest(${r.id})\">Approve</button><button class=\"btn btn-secondary\" onclick=\"rejectRequest(${r.id})\">Reject</button></div>" else "") +
                                "</div>"
                        }
                        container.innerHTML = html
                    }
                }
                undefined
            }
        }
    }

    fun approveRequest(requestId: String) {
        ApiClient.updateAdoptionRequest(requestId, "APPROVED").then { init() }
    }
    fun rejectRequest(requestId: String) {
        ApiClient.updateAdoptionRequest(requestId, "REJECTED").then { init() }
    }

    private fun fillForm(pet: dynamic) {
        (document.getElementById("pet-id") as? HTMLInputElement)?.value = pet.id
        (document.getElementById("name") as? HTMLInputElement)?.value = pet.name ?: ""
        (document.getElementById("type") as? HTMLSelectElement)?.value = pet.type ?: "DOG"
        (document.getElementById("breed") as? HTMLInputElement)?.value = pet.breed ?: ""
        (document.getElementById("description") as? HTMLTextAreaElement)?.value = pet.description ?: ""
        (document.getElementById("weight") as? HTMLInputElement)?.value = pet.weight?.toString() ?: "0"
        (document.getElementById("ageYears") as? HTMLInputElement)?.value = pet.ageYears?.toString() ?: "0"
        (document.getElementById("ageMonths") as? HTMLInputElement)?.value = pet.ageMonths?.toString() ?: "0"
        (document.getElementById("sex") as? HTMLSelectElement)?.value = pet.sex ?: "MALE"
        (document.getElementById("color") as? HTMLInputElement)?.value = pet.color ?: ""
        (document.getElementById("size") as? HTMLSelectElement)?.value = pet.size ?: ""
        (document.getElementById("temperament") as? HTMLInputElement)?.value = pet.temperament ?: ""
        (document.getElementById("energyLevel") as? HTMLSelectElement)?.value = pet.energyLevel ?: ""
        (document.getElementById("isSterilized") as? HTMLInputElement)?.checked = pet.isSterilized ?: false
        (document.getElementById("isMicrochipped") as? HTMLInputElement)?.checked = pet.isMicrochipped ?: false
        (document.getElementById("microchipId") as? HTMLInputElement)?.value = pet.microchipId ?: ""
        (document.getElementById("vaccinations") as? HTMLTextAreaElement)?.value = pet.vaccinations ?: ""
        (document.getElementById("isGoodWithKids") as? HTMLInputElement)?.checked = pet.isGoodWithKids ?: true
        (document.getElementById("isGoodWithDogs") as? HTMLInputElement)?.checked = pet.isGoodWithDogs ?: true
        (document.getElementById("isGoodWithCats") as? HTMLInputElement)?.checked = pet.isGoodWithCats ?: true
        (document.getElementById("isHouseTrained") as? HTMLInputElement)?.checked = pet.isHouseTrained ?: false
        (document.getElementById("rescueLocation") as? HTMLInputElement)?.value = pet.rescueLocation ?: ""
        if (pet.rescueDate != null) {
            val d = js("new Date")(pet.rescueDate)
            (document.getElementById("rescueDate") as? HTMLInputElement)?.value = d.toISOString().substring(0, 10)
        }
        (document.getElementById("specialNeeds") as? HTMLTextAreaElement)?.value = pet.specialNeeds ?: ""
        (document.getElementById("adoptionFee") as? HTMLInputElement)?.value = pet.adoptionFee?.toString() ?: "0"
        (document.getElementById("currency") as? HTMLSelectElement)?.value = pet.currency ?: "USD"
        (document.getElementById("isUrgent") as? HTMLInputElement)?.checked = pet.isUrgent ?: false

        existingImages = if (pet.images != null) js("Array.from")(pet.images).asDynamic() else js("[]")
        updatePreviews()
    }

    private fun setupFormButtons() {
        (document.getElementById("add-btn") as? HTMLElement)?.onclick = {
            (document.getElementById("pet-form") as? HTMLFormElement)?.reset()
            (document.getElementById("pet-id") as? HTMLInputElement)?.value = ""
            (document.getElementById("currency") as? HTMLSelectElement)?.value = "USD"
            selectedFiles = emptyList()
            existingImages = emptyList()
            updatePreviews()
            (document.getElementById("form-title") as? HTMLElement)?.textContent = "Add Pet"
            (document.getElementById("form-container") as? HTMLElement)?.style?.display = "block"
            undefined
        }
        (document.getElementById("cancel-btn") as? HTMLElement)?.onclick = {
            (document.getElementById("form-container") as? HTMLElement)?.style?.display = "none"
            js("history.replaceState")({}, "", "/my-pets")
            undefined
        }
    }

    private fun setupDropzone() {
        val dropzone = document.getElementById("storage-dropzone") as? HTMLElement ?: return
        val fileInput = document.getElementById("pet-images") as? HTMLInputElement ?: return
        dropzone.onclick = { fileInput.click(); undefined }
        dropzone.ondragover = { e -> e.preventDefault(); dropzone.classList.add("dragover"); undefined }
        dropzone.ondragleave = { dropzone.classList.remove("dragover"); undefined }
        dropzone.ondrop = { e ->
            e.preventDefault()
            dropzone.classList.remove("dragover")
            handleFiles(e.asDynamic().dataTransfer.files)
            undefined
        }
        fileInput.onchange = { handleFiles(fileInput.files); undefined }
    }

    private fun handleFiles(files: dynamic) {
        val remaining = 12 - selectedFiles.size
        if (remaining <= 0) { js("alert")("Maximum 12 photos allowed"); return }
        val filesToAdd = js("Array.from")(files).asDynamic().slice(0, remaining)
        selectedFiles = selectedFiles + js("Array.from")(filesToAdd).asDynamic()
        updatePreviews()
    }

    private fun updatePreviews() {
        val previewContainer = document.getElementById("storage-previews") ?: return
        var html = ""
        for (i in 0 until existingImages.size) {
            val img = existingImages[i]
            html += "<div class=\"preview-item${if (img.isPrimary) " primary" else ""}\">" +
                "<img src=\"${img.imageUrl}\">" +
                (if (img.isPrimary) "<span class=\"primary-badge\">\u2605</span>" else "<button type=\"button\" class=\"primary-btn\" data-set-primary=\"$i\">\u2606</button>") +
                "<button type=\"button\" data-remove-existing=\"$i\">\u00D7</button></div>"
        }
        for (i in 0 until selectedFiles.size) {
            val file = selectedFiles[i]
            html += "<div class=\"preview-item\"><img src=\"${js("URL.createObjectURL")(file)}\"><button type=\"button\" data-remove-preview=\"$i\">\u00D7</button></div>"
        }
        previewContainer.innerHTML = html

        previewContainer.querySelectorAll("[data-set-primary]").forEach { btn ->
            (btn as HTMLElement).onclick = {
                val idx = btn.getAttribute("data-set-primary")!!.toInt()
                val petId = (document.getElementById("pet-id") as? HTMLInputElement)?.value ?: ""
                ApiClient.setPrimaryImage(petId, existingImages[idx].id).then {
                    for (j in existingImages.indices) existingImages[j].isPrimary = (j == idx)
                    updatePreviews()
                    undefined
                }
                undefined
            }
        }
        previewContainer.querySelectorAll("[data-remove-existing]").forEach { btn ->
            (btn as HTMLElement).onclick = {
                val idx = btn.getAttribute("data-remove-existing")!!.toInt()
                val petId = (document.getElementById("pet-id") as? HTMLInputElement)?.value ?: ""
                if (js("confirm")("Delete this image?") == true) {
                    ApiClient.removeImage(petId, existingImages[idx].id).then {
                        existingImages = existingImages.toMutableList().apply { removeAt(idx) }
                        updatePreviews()
                        undefined
                    }
                }
                undefined
            }
        }
        previewContainer.querySelectorAll("[data-remove-preview]").forEach { btn ->
            (btn as HTMLElement).onclick = {
                val idx = btn.getAttribute("data-remove-preview")!!.toInt()
                selectedFiles = selectedFiles.toMutableList().apply { removeAt(idx) }
                updatePreviews()
                undefined
            }
        }
    }

    private fun setupFormSubmit() {
        (document.getElementById("pet-form") as? HTMLFormElement)?.onsubmit = { e ->
            e.preventDefault()
            val msg = document.getElementById("message") as? HTMLElement ?: return@onsubmit undefined
            val id = (document.getElementById("pet-id") as? HTMLInputElement)?.value
            val weight = (document.getElementById("weight") as? HTMLInputElement)?.value?.toDoubleOrNull() ?: 0.0
            val ageYears = (document.getElementById("ageYears") as? HTMLInputElement)?.value?.toIntOrNull() ?: 0
            val ageMonths = (document.getElementById("ageMonths") as? HTMLInputElement)?.value?.toIntOrNull() ?: 0
            val adoptionFee = (document.getElementById("adoptionFee") as? HTMLInputElement)?.value?.toDoubleOrNull() ?: 0.0

            if (weight < 0) { msg.className = "message error"; msg.textContent = "Weight must be zero or positive"; return@onsubmit undefined }
            if (ageYears < 0) { msg.className = "message error"; msg.textContent = "Age (years) must be zero or positive"; return@onsubmit undefined }
            if (ageMonths < 0 || ageMonths > 11) { msg.className = "message error"; msg.textContent = "Age (months) must be between 0 and 11"; return@onsubmit undefined }
            if (adoptionFee < 0) { msg.className = "message error"; msg.textContent = "Adoption fee must be zero or positive"; return@onsubmit undefined }

            val rescueDateVal = (document.getElementById("rescueDate") as? HTMLInputElement)?.value
            val data = js("({})")
            data.name = (document.getElementById("name") as? HTMLInputElement)?.value
            data.type = (document.getElementById("type") as? HTMLSelectElement)?.value
            data.breed = (document.getElementById("breed") as? HTMLInputElement)?.value?.takeIf { it.isNotEmpty() }
            data.description = (document.getElementById("description") as? HTMLTextAreaElement)?.value
            data.weight = weight
            data.ageYears = ageYears
            data.ageMonths = ageMonths
            data.sex = (document.getElementById("sex") as? HTMLSelectElement)?.value
            data.color = (document.getElementById("color") as? HTMLInputElement)?.value?.takeIf { it.isNotEmpty() }
            data.size = (document.getElementById("size") as? HTMLSelectElement)?.value?.takeIf { it.isNotEmpty() }
            data.temperament = (document.getElementById("temperament") as? HTMLInputElement)?.value?.takeIf { it.isNotEmpty() }
            data.energyLevel = (document.getElementById("energyLevel") as? HTMLSelectElement)?.value?.takeIf { it.isNotEmpty() }
            data.isSterilized = (document.getElementById("isSterilized") as? HTMLInputElement)?.checked
            data.isMicrochipped = (document.getElementById("isMicrochipped") as? HTMLInputElement)?.checked
            data.microchipId = (document.getElementById("microchipId") as? HTMLInputElement)?.value?.takeIf { it.isNotEmpty() }
            data.vaccinations = (document.getElementById("vaccinations") as? HTMLTextAreaElement)?.value?.takeIf { it.isNotEmpty() }
            data.isGoodWithKids = (document.getElementById("isGoodWithKids") as? HTMLInputElement)?.checked
            data.isGoodWithDogs = (document.getElementById("isGoodWithDogs") as? HTMLInputElement)?.checked
            data.isGoodWithCats = (document.getElementById("isGoodWithCats") as? HTMLInputElement)?.checked
            data.isHouseTrained = (document.getElementById("isHouseTrained") as? HTMLInputElement)?.checked
            data.rescueLocation = (document.getElementById("rescueLocation") as? HTMLInputElement)?.value?.takeIf { it.isNotEmpty() }
            data.rescueDate = rescueDateVal?.let { js("new Date")(it).getTime() }
            data.specialNeeds = (document.getElementById("specialNeeds") as? HTMLTextAreaElement)?.value?.takeIf { it.isNotEmpty() }
            data.adoptionFee = adoptionFee
            data.currency = (document.getElementById("currency") as? HTMLSelectElement)?.value
            data.isUrgent = (document.getElementById("isUrgent") as? HTMLInputElement)?.checked

            if (id != null) {
                ApiClient.updatePet(id, data).then {
                    uploadImages(id, msg)
                    undefined
                }
            } else {
                ApiClient.createPet(data).then { pet ->
                    uploadImages(pet.id, msg)
                    undefined
                }
            }
            undefined
        }
    }

    private fun uploadImages(petId: String, msg: HTMLElement) {
        if (selectedFiles.isEmpty()) {
            msg.className = "message success"
            msg.textContent = "Saved!"
            (document.getElementById("form-container") as? HTMLElement)?.style?.display = "none"
            loadPets(null, null)
            return
        }
        var i = 0
        fun uploadNext() {
            if (i >= selectedFiles.size) {
                msg.className = "message success"
                msg.textContent = "Saved!"
                (document.getElementById("form-container") as? HTMLElement)?.style?.display = "none"
                loadPets(null, null)
                return
            }
            ApiClient.addImage(petId, selectedFiles[i], i == 0).then {
                i++
                uploadNext()
                undefined
            }
        }
        uploadNext()
    }

    private fun setupLogout() {
        (document.getElementById("logout-link") as? HTMLElement)?.onclick = { e ->
            e.preventDefault()
            ApiClient.logout().then { window.location.href = "/" }
            undefined
        }
    }
}
