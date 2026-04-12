@JsExport
@JsName("photographers")
object Photographers {
    @JsName("searchPhotographers")
    @JvmStatic
    external fun searchPhotographers() {
        val country = document.getElementById("search-country")?.asDynamic().value as? String ?: ""
        val state = document.getElementById("search-state")?.asDynamic().value as? String ?: ""
        val city = document.getElementById("search-city")?.asDynamic().value as? String ?: ""
        val zip = document.getElementById("search-zip")?.asDynamic().value as? String ?: ""
        val neighborhood = document.getElementById("search-neighborhood")?.asDynamic().value as? String ?: ""

        if (country.isEmpty()) {
            document.getElementById("photographers")?.innerHTML = "<p>${t("pleaseSelectCountry")}</p>"
            return
        }

        val params = mutableListOf("country=$country")
        if (state.isNotEmpty()) params.add("state=$state")
        if (city.isNotEmpty()) params.add("city=$city")
        if (zip.isNotEmpty()) params.add("zip=$zip")
        if (neighborhood.isNotEmpty()) params.add("neighborhood=$neighborhood")

        loadPhotographers("/api/photographers?${params.joinToString("&")}")
    }

    @JsName("debounce")
    @JvmStatic
    external fun debounce(func: Function<Unit>, wait: Int): Function<Unit> {
        var timeout: dynamic = null
        return { ->
            val later = { ->
                clearTimeout(timeout)
                func.invoke()
            }
            clearTimeout(timeout)
            timeout = setTimeout(later, wait)
        }
    }

    private fun loadPhotographers(url: String) {
        window.fetch(url).then { response: dynamic ->
            if (!response.ok) {
                throw Exception("Failed to load photographers")
            }
            response.json()
        }.then { photographers: dynamic ->
            val container = document.getElementById("photographers")
            if (photographers == null || (photographers as Array<*>).isEmpty()) {
                container?.innerHTML = "<p>${t("noPhotographersAvailable")}</p>"
                return@then
            }

            val cards = photographers.map { p: dynamic ->
                val fee = if (p.photographerFee) "${p.photographerFee} ${p.photographerCurrency ?: "USD"}" else "Free"
                val location = listOfNotNull(
                    p.photographerCity as? String,
                    p.photographerState as? String,
                    p.photographerCountry as? String
                ).joinToString(", ")

                """<div class="photographer-card">
                    <div class="photographer-info">
                        <h3>${p.displayName}</h3>
                        ${if (location.isNotEmpty()) "<p class=\"photographer-location\">$location</p>" else ""}
                        <p class="photographer-fee">${t("sessionFee")}: <strong>$fee</strong></p>
                    </div>
                    <button class="btn request-btn" data-id="${p.userId}" data-name="${p.displayName}" data-fee="$fee">${t("requestPhotoSession")}</button>
                </div>""".trimMargin()
            }.joinToString("")

            container?.innerHTML = cards

            val buttons = document.querySelectorAll(".request-btn")
            buttons.forEach { btn: dynamic ->
                btn.onclick = { ->
                    val photographerId = btn.dataset.id.unsafeCast<Number>().toInt()
                    val photographerName = btn.dataset.name
                    val fee = btn.dataset.fee
                    showRequestModal(photographerId, photographerName, fee)
                }
            }
        }.catch { err: dynamic ->
            document.getElementById("photographers")?.innerHTML = "<p>${t("errorLoadingPhotographers")}</p>"
        }
    }

    private fun createRequestModal() {
        val modal = document.createElement("div")
        modal.id = "request-modal"
        modal.className = "form-modal"
        modal.asDynamic().style.display = "none"
        modal.innerHTML = """
            <div class="form-modal-content card-bg">
                <h2 id="modal-title">${t("requestPhotoSession")}</h2>
                <p id="modal-photographer" class="photographer-fee"></p>
                <form id="request-form">
                    <div class="form-group">
                        <label for="request-message">${t("message")}</label>
                        <textarea id="request-message" rows="4" class="form-control"></textarea>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">${t("sendRequest")}</button>
                        <button type="button" class="btn" id="cancel-request">${t("cancel")}</button>
                    </div>
                </form>
            </div>
        """.trimIndent()
        document.body?.appendChild(modal)

        document.getElementById("cancel-request")?.addEventListener("click") {
            modal.asDynamic().style.display = "none"
        }

        modal.addEventListener("click") { e: dynamic ->
            if (e.target === modal) {
                modal.asDynamic().style.display = "none"
            }
        }

        document.getElementById("request-form")?.addEventListener("submit") { e: dynamic ->
            e.preventDefault()
            val message = document.getElementById("request-message")?.asDynamic().value as? String ?: ""
            val photographerId = modal.dataset.photographerId.unsafeCast<Number>().toInt()

            api.unsafeCast<dynamic>().createPhotographyRequest(photographerId, null, message ?: "")
                .then {
                    modal.asDynamic().style.display = "none"
                    window.alert(t("requestSentSuccessfully"))
                }
                .catch { err: dynamic ->
                    window.alert("Error: ${err.message}")
                }
        }
    }

    private fun showRequestModal(photographerId: Int, photographerName: String, fee: String) {
        val modal = document.getElementById("request-modal")
        document.getElementById("modal-title")?.textContent = t("requestPhotoSession")
        document.getElementById("modal-photographer")?.innerHTML = "${t("requestTo")}: <strong>$photographerName</strong><br>${t("sessionFee")}: <strong>$fee</strong>"
        
        val msgInput = document.getElementById("request-message")
        msgInput?.placeholder = t("enterMessage").replace("{name}", photographerName)
        msgInput?.asDynamic()?.value = ""
        
        modal?.asDynamic()?.dataset?.photographerId = photographerId
        modal?.asDynamic()?.style?.display = "flex"
    }

    @JsName("init")
    @JvmStatic
    external fun init() {
        createRequestModal()
        
        val searchBtn = document.getElementById("search-btn")
        searchBtn?.addEventListener("click") { searchPhotographers() }

        listOf("search-state", "search-city", "search-zip", "search-neighborhood").forEach { id ->
            val input = document.getElementById(id)
            if (input != null) {
                input.addEventListener("input") { debounce({ searchPhotographers() }, 500) }
            }
        }
    }
}

@JsExport
@JsName("api")
external val api: dynamic

@JsExport
@JsName("t")
external fun t(key: String): String

fun init() {
    if (document.readyState == "loading") {
        document.addEventListener("DOMContentLoaded") { Photographers.init() }
    } else {
        Photographers.init()
    }
}