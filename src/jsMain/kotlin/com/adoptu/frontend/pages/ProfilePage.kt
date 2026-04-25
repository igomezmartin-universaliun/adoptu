package com.adoptu.frontend.pages

import com.adoptu.frontend.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import kotlin.js.json

object ProfilePage {
    private var currentRoles = emptyList<String>()
    private var hasTemporalHomeProfile = false
    private var hasShelterProfile = false
    private var hasSterilizationProfile = false

    suspend fun init() {
        loadProfile()
        setupSaveButton()
        setupPasswordButton()
        setupEmailChangeButton()
        setupPasskeyButton()
        loadPasswordStatus()
        loadPasskeyStatus()

        (document.getElementById("logout-link") as? HTMLElement)?.onclick = { e ->
            e.preventDefault()
            ApiClient.logout().then { window.location.href = "/" }
            undefined
        }
    }

    private suspend fun loadProfile() {
        var user: dynamic = js("window.cachedUserData")
        if (user == null) {
            user = js("window.userDataPromise").awaitCommon()
        }
        if (user == null) {
            try { user = ApiClient.me(); js("window.cachedUserData = user") } catch (e: Throwable) { return }
        }
        if (user.authenticated == false) { window.location.href = "/login"; return }

        (document.getElementById("email") as? HTMLInputElement)?.value = user.email ?: ""
        (document.getElementById("displayName") as? HTMLInputElement)?.value = user.displayName ?: ""
        (document.getElementById("language") as? HTMLSelectElement)?.value = user.language ?: "en"
        currentRoles = (user.activeRoles as? Array<*>)?.map { it.toString() } ?: emptyList()

        listOf("role-rescuer", "role-photographer", "role-temporal-home", "role-shelter", "role-sterilization").forEach { id ->
            (document.getElementById(id) as? HTMLInputElement)?.checked = when (id) {
                "role-rescuer" -> currentRoles.contains("RESCUER")
                "role-photographer" -> currentRoles.contains("PHOTOGRAPHER")
                "role-temporal-home" -> currentRoles.contains("TEMPORAL_HOME")
                "role-shelter" -> currentRoles.contains("SHELTER")
                "role-sterilization" -> currentRoles.contains("STERILIZATION_SERVICE")
                else -> false
            }
        }

        try { ApiClient.getTemporalHome(); hasTemporalHomeProfile = true } catch (e: Throwable) { hasTemporalHomeProfile = false }
        try { val r = fetch("/api/users/shelter"); hasShelterProfile = r.ok } catch (e: Throwable) { hasShelterProfile = false }
        try { val r = fetch("/api/users/sterilization-location"); hasSterilizationProfile = r.ok } catch (e: Throwable) { hasSterilizationProfile = false }

        setupRoleToggles()
        loadRoleSections(user)
    }

    private fun setupRoleToggles() {
        (document.getElementById("role-photographer") as? HTMLInputElement)?.onchange = {
            document.querySelector(".photographer-section")?.asDynamic()?.style?.display =
                if ((it.asDynamic().target.checked)) "block" else "none"
            undefined
        }
        (document.getElementById("role-temporal-home") as? HTMLInputElement)?.onchange = {
            document.querySelector(".temporal-home-section")?.asDynamic()?.style?.display =
                if ((it.asDynamic().target.checked)) "block" else "none"
            undefined
        }
        (document.getElementById("role-shelter") as? HTMLInputElement)?.onchange = {
            document.querySelector(".shelter-section")?.asDynamic()?.style?.display =
                if ((it.asDynamic().target.checked)) "block" else "none"
            undefined
        }
        (document.getElementById("role-sterilization") as? HTMLInputElement)?.onchange = {
            document.querySelector(".sterilization-section")?.asDynamic()?.style?.display =
                if ((it.asDynamic().target.checked)) "block" else "none"
            undefined
        }
    }

    private fun loadRoleSections(user: dynamic) {
        val primaryRole = currentRoles.firstOrNull() ?: "ADOPTER"
        if (primaryRole == "PHOTOGRAPHER" || currentRoles.contains("PHOTOGRAPHER")) {
            document.querySelector(".photographer-section")?.asDynamic()?.style?.display = "block"
            loadPhotographer(user)
        }
        if (primaryRole == "TEMPORAL_HOME" || currentRoles.contains("TEMPORAL_HOME")) {
            document.querySelector(".temporal-home-section")?.asDynamic()?.style?.display = "block"
            loadTemporalHome()
        }
        if (primaryRole == "SHELTER" || currentRoles.contains("SHELTER")) {
            document.querySelector(".shelter-section")?.asDynamic()?.style?.display = "block"
            loadShelter()
        }
        if (primaryRole == "STERILIZATION_SERVICE" || currentRoles.contains("STERILIZATION_SERVICE")) {
            document.querySelector(".sterilization-section")?.asDynamic()?.style?.display = "block"
            loadSterilization()
        }
    }

    private fun loadPhotographer(user: dynamic) {
        (document.getElementById("photographerFee") as? HTMLInputElement)?.value = user.photographerFee?.toString() ?: "0"
        (document.getElementById("photographerCurrency") as? HTMLSelectElement)?.value = user.photographerCurrency ?: "USD"
        (document.getElementById("photographerCountry") as? HTMLSelectElement)?.value = user.photographerCountry ?: ""
        (document.getElementById("photographerState") as? HTMLInputElement)?.value = user.photographerState ?: ""
    }

    private fun loadTemporalHome() {
        ApiClient.getTemporalHome().then { th ->
            if (th != null) {
                (document.getElementById("th-alias") as? HTMLInputElement)?.value = th.alias ?: ""
                (document.getElementById("th-country") as? HTMLSelectElement)?.value = th.country ?: ""
                (document.getElementById("th-state") as? HTMLInputElement)?.value = th.state ?: ""
                (document.getElementById("th-city") as? HTMLInputElement)?.value = th.city ?: ""
                (document.getElementById("th-zip") as? HTMLInputElement)?.value = th.zip ?: ""
                (document.getElementById("th-neighborhood") as? HTMLInputElement)?.value = th.neighborhood ?: ""
            }
            undefined
        }
    }

    private fun loadShelter() {
        fetch("/api/users/shelter").then { res ->
            if (res.ok) {
                res.json().then { shelter ->
                    (document.getElementById("shelter-name") as? HTMLInputElement)?.value = shelter.name ?: ""
                    (document.getElementById("shelter-country") as? HTMLSelectElement)?.value = shelter.country ?: ""
                    (document.getElementById("shelter-state") as? HTMLInputElement)?.value = shelter.state ?: ""
                    (document.getElementById("shelter-city") as? HTMLInputElement)?.value = shelter.city ?: ""
                    (document.getElementById("shelter-address") as? HTMLInputElement)?.value = shelter.address ?: ""
                    (document.getElementById("shelter-zip") as? HTMLInputElement)?.value = shelter.zip ?: ""
                    (document.getElementById("shelter-phone") as? HTMLInputElement)?.value = shelter.phone ?: ""
                    (document.getElementById("shelter-email") as? HTMLInputElement)?.value = shelter.email ?: ""
                    (document.getElementById("shelter-website") as? HTMLInputElement)?.value = shelter.website ?: ""
                    (document.getElementById("shelter-description") as? HTMLTextAreaElement)?.value = shelter.description ?: ""
                    undefined
                }
            } else undefined
        }
    }

    private fun loadSterilization() {
        fetch("/api/users/sterilization-location").then { res ->
            if (res.ok) {
                res.json().then { loc ->
                    (document.getElementById("sterilization-name") as? HTMLInputElement)?.value = loc.name ?: ""
                    (document.getElementById("sterilization-country") as? HTMLSelectElement)?.value = loc.country ?: ""
                    (document.getElementById("sterilization-state") as? HTMLInputElement)?.value = loc.state ?: ""
                    (document.getElementById("sterilization-city") as? HTMLInputElement)?.value = loc.city ?: ""
                    (document.getElementById("sterilization-address") as? HTMLInputElement)?.value = loc.address ?: ""
                    (document.getElementById("sterilization-zip") as? HTMLInputElement)?.value = loc.zip ?: ""
                    (document.getElementById("sterilization-phone") as? HTMLInputElement)?.value = loc.phone ?: ""
                    (document.getElementById("sterilization-email") as? HTMLInputElement)?.value = loc.email ?: ""
                    (document.getElementById("sterilization-website") as? HTMLInputElement)?.value = loc.website ?: ""
                    (document.getElementById("sterilization-description") as? HTMLTextAreaElement)?.value = loc.description ?: ""
                    undefined
                }
            } else undefined
        }
    }

    private fun setupSaveButton() {
        (document.getElementById("save-profile-btn") as? HTMLElement)?.onclick = {
            saveProfile()
            undefined
        }
    }

    private fun saveProfile() {
        val msg = document.getElementById("message") as? HTMLElement ?: return
        val displayName = (document.getElementById("displayName") as? HTMLInputElement)?.value ?: ""
        val language = (document.getElementById("language") as? HTMLSelectElement)?.value ?: "en"

        if (displayName.trim().isEmpty()) {
            msg.className = "message error"
            msg.textContent = "Display name cannot be empty"
            return
        }

        ApiClient.updateProfile(displayName).then {
            ApiClient.updateLanguage(language)
            I18n.setLang(language)
            undefined
        }.then {
            saveRoles(msg)
            undefined
        }
    }

    private fun saveRoles(msg: HTMLElement) {
        val roleRescuer = (document.getElementById("role-rescuer") as? HTMLInputElement)?.checked ?: false
        val rolePhotographer = (document.getElementById("role-photographer") as? HTMLInputElement)?.checked ?: false
        val roleTemporalHome = (document.getElementById("role-temporal-home") as? HTMLInputElement)?.checked ?: false
        val roleShelter = (document.getElementById("role-shelter") as? HTMLInputElement)?.checked ?: false
        val roleSterilization = (document.getElementById("role-sterilization") as? HTMLInputElement)?.checked ?: false

        if (roleRescuer != currentRoles.contains("RESCUER")) {
            ApiClient.activateRescuer(roleRescuer)
        }
        if (rolePhotographer != currentRoles.contains("PHOTOGRAPHER")) {
            if (rolePhotographer) {
                val phCountry = (document.getElementById("photographerCountry") as? HTMLSelectElement)?.value ?: ""
                val phState = (document.getElementById("photographerState") as? HTMLInputElement)?.value ?: ""
                if (phCountry.isEmpty() || phState.isEmpty()) {
                    msg.className = "message error"
                    msg.textContent = "Please fill in country and state for photographer services"
                    return
                }
                ApiClient.activatePhotographer(true).then {
                    val phFee = (document.getElementById("photographerFee") as? HTMLInputElement)?.value?.toDoubleOrNull() ?: 0.0
                    val phCurrency = (document.getElementById("photographerCurrency") as? HTMLSelectElement)?.value ?: "USD"
                    ApiClient.updatePhotographerSettings(phFee, phCurrency, phCountry, phState)
                    undefined
                }
            } else {
                ApiClient.activatePhotographer(false)
            }
        }
        if (roleTemporalHome != currentRoles.contains("TEMPORAL_HOME")) {
            if (roleTemporalHome) {
                val thAlias = (document.getElementById("th-alias") as? HTMLInputElement)?.value?.trim() ?: ""
                val thCountry = (document.getElementById("th-country") as? HTMLSelectElement)?.value ?: ""
                val thCity = (document.getElementById("th-city") as? HTMLInputElement)?.value?.trim() ?: ""
                if (thAlias.isEmpty() || thCountry.isEmpty() || thCity.isEmpty()) {
                    msg.className = "message error"
                    msg.textContent = "Please fill in alias, country and city for temporal home"
                    return
                }
                val thData = js("({})")
                thData.alias = thAlias
                thData.country = thCountry
                thData.state = (document.getElementById("th-state") as? HTMLInputElement)?.value?.trim() ?: null
                thData.city = thCity
                thData.zip = (document.getElementById("th-zip") as? HTMLInputElement)?.value?.trim() ?: null
                thData.neighborhood = (document.getElementById("th-neighborhood") as? HTMLInputElement)?.value?.trim() ?: null

                if (hasTemporalHomeProfile) {
                    ApiClient.updateTemporalHome(thData)
                } else {
                    ApiClient.createTemporalHome(thData).then { hasTemporalHomeProfile = true; undefined }
                }
            } else {
                ApiClient.activateTemporalHome(false)
            }
        }

        msg.className = "message success"
        msg.textContent = "Profile updated!"
        window.location.reload()
    }

    private fun setupPasswordButton() {
        (document.getElementById("save-password-btn") as? HTMLElement)?.onclick = {
            savePassword()
            undefined
        }
    }

    private fun savePassword() {
        val currentPassword = (document.getElementById("current-password") as? HTMLInputElement)?.value ?: ""
        val newPassword = (document.getElementById("new-password") as? HTMLInputElement)?.value ?: ""
        val confirmPassword = (document.getElementById("confirm-password") as? HTMLInputElement)?.value ?: ""
        val msg = document.getElementById("password-message") as? HTMLElement ?: return
        val hasPassword = (document.getElementById("current-password") as? HTMLInputElement)?.disabled == false

        if (newPassword != confirmPassword) {
            msg.className = "message error"
            msg.textContent = I18n.t("passwordsDoNotMatch")
            return
        }

        if (hasPassword && currentPassword.isEmpty()) {
            msg.className = "message error"
            msg.textContent = I18n.t("currentPasswordRequired")
            return
        }

        msg.textContent = I18n.t("saving")
        msg.className = ""

        RsaCrypto.getPublicKey().then { key ->
            if (hasPassword) {
                RsaCrypto.encrypt(currentPassword, key.toString()).then { encCurrent ->
                    RsaCrypto.encrypt(newPassword, key.toString()).then { encNew ->
                        ApiClient.changePassword(encCurrent!!, encNew!!).then { result ->
                            if (result.success == true) {
                                msg.className = "message success"
                                msg.textContent = I18n.t("passwordChangeSuccess")
                                (document.getElementById("current-password") as? HTMLInputElement)?.value = ""
                                (document.getElementById("new-password") as? HTMLInputElement)?.value = ""
                                (document.getElementById("confirm-password") as? HTMLInputElement)?.value = ""
                            } else {
                                msg.className = "message error"
                                msg.textContent = result.error ?: I18n.t("passwordChangeFailed")
                            }
                            undefined
                        }
                    }
                }
            } else {
                RsaCrypto.encrypt(newPassword, key.toString()).then { encNew ->
                    ApiClient.setPassword(encNew!!).then { result ->
                        if (result.success == true) {
                            msg.className = "message success"
                            msg.textContent = I18n.t("passwordSetSuccess")
                            (document.getElementById("new-password") as? HTMLInputElement)?.value = ""
                            (document.getElementById("confirm-password") as? HTMLInputElement)?.value = ""
                            loadPasswordStatus()
                        } else {
                            msg.className = "message error"
                            msg.textContent = result.error ?: I18n.t("passwordSetFailed")
                        }
                        undefined
                    }
                }
            }
        }
    }

    private fun loadPasswordStatus() {
        fetch("/api/users/has-password").then { res ->
            res.json().then { data ->
                val passwordStatus = document.getElementById("password-status") as? HTMLElement
                val currentPasswordRow = document.getElementById("current-password-row") as? HTMLElement
                if (data.hasPassword == true) {
                    passwordStatus?.textContent = I18n.t("passwordSet")
                    currentPasswordRow?.classList?.remove("password-hidden")
                    (document.getElementById("current-password") as? HTMLInputElement)?.disabled = false
                } else {
                    passwordStatus?.textContent = I18n.t("noPassword")
                    currentPasswordRow?.classList?.add("password-hidden")
                    (document.getElementById("current-password") as? HTMLInputElement)?.disabled = true
                }
                undefined
            }
        }
    }

    private fun setupEmailChangeButton() {
        (document.getElementById("change-email-btn") as? HTMLElement)?.onclick = {
            requestEmailChange()
            undefined
        }
    }

    private fun requestEmailChange() {
        val newEmail = (document.getElementById("new-email") as? HTMLInputElement)?.value ?: ""
        val msg = document.getElementById("email-change-message") as? HTMLElement ?: return
        if (newEmail.isEmpty()) {
            msg.className = "message error"
            msg.textContent = I18n.t("emailRequired")
            return
        }
        msg.textContent = I18n.t("sending")
        msg.className = ""
        ApiClient.requestEmailChange(newEmail).then { result ->
            if (result.success == true) {
                msg.className = "message success"
                msg.textContent = I18n.t("emailChangeSent")
                (document.getElementById("new-email") as? HTMLInputElement)?.value = ""
            } else {
                msg.className = "message error"
                msg.textContent = result.error ?: I18n.t("emailChangeFailed")
            }
            undefined
        }
    }

    private fun setupPasskeyButton() {
        (document.getElementById("register-passkey-btn") as? HTMLElement)?.onclick = {
            registerPasskey()
            undefined
        }
    }

    private fun registerPasskey() {
        val msg = document.getElementById("passkey-message") as? HTMLElement ?: return
        val passkeyName = (document.getElementById("passkey-name") as? HTMLInputElement)?.value?.trim() ?: ""
        if (passkeyName.isEmpty()) {
            msg.className = "message error"
            msg.textContent = I18n.t("passkeyNameRequired")
            return
        }
        msg.textContent = I18n.t("registeringPasskey")
        msg.className = ""

        js("window.cachedUserData").let { user ->
            if (user == null) {
                ApiClient.me().then { u ->
                    doRegisterPasskey(u, passkeyName, msg)
                    undefined
                }
            } else {
                doRegisterPasskey(user, passkeyName, msg)
            }
        }
    }

    private fun doRegisterPasskey(user: dynamic, passkeyName: String, msg: HTMLElement) {
        fetch("/api/auth/registration-options-for-user", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email: user.email, displayName: user.displayName})})")).then { res ->
            res.json().then { options ->
                val credential = navigator.credentials.create(json("publicKey" to WebAuthn.parseCreationOptions(options))).awaitCommon()
                fetch("/api/auth/register-passkey", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({registrationResponse: JSON.stringify(credential), passkeyName: passkeyName})})")).then { regRes ->
                    regRes.json().then { result ->
                        if (result.success == true) {
                            msg.className = "message success"
                            msg.textContent = I18n.t("passkeyRegisteredSuccess")
                            (document.getElementById("passkey-name") as? HTMLInputElement)?.value = ""
                            loadPasskeyStatus()
                        } else {
                            msg.className = "message error"
                            msg.textContent = result.error ?: I18n.t("passkeyRegistrationFailed")
                        }
                        undefined
                    }
                }
            }
        }
    }

    private fun loadPasskeyStatus() {
        fetch("/api/auth/has-passkey").then { res ->
            res.json().then { data ->
                val passkeyStatus = document.getElementById("passkey-status") as? HTMLElement
                if (data.success == true) {
                    passkeyStatus?.textContent = I18n.t("passkeyRegistered")
                } else {
                    passkeyStatus?.textContent = I18n.t("noPasskey")
                }
                undefined
            }
        }
    }
}
