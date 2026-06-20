package com.adoptu.frontend.pages

import com.adoptu.frontend.*
import kotlinx.browser.document
import kotlinx.browser.localStorage
import org.w3c.dom.*
import kotlin.js.json

object LoginPage {
    private var pendingEmail: String? = null

    suspend fun init() {
        document.querySelectorAll(".auth-form-container").forEach { f ->
            (f as HTMLElement).style.display = "block"
        }

        val hash = window.location.hash
        if (hash == "#magic-link") {
            (document.getElementById("tab-magic-link") as? HTMLElement)?.click()
        } else if (hash == "#password") {
            (document.getElementById("tab-password") as? HTMLElement)?.click()
        }

        (document.getElementById("login-btn") as? HTMLElement)?.onclick = {
            loginWithPasskey()
            undefined
        }

        (document.getElementById("resend-btn") as? HTMLElement)?.onclick = {
            resendVerification()
            undefined
        }

        (document.getElementById("magic-link-btn") as? HTMLElement)?.onclick = {
            sendMagicLink()
            undefined
        }

        (document.getElementById("password-login-btn") as? HTMLElement)?.onclick = {
            loginWithPassword()
            undefined
        }
    }

    private fun loginWithPasskey() {
        val msg = document.getElementById("passkey-message") as? HTMLElement ?: return
        msg.textContent = I18n.t("authenticating")
        msg.className = ""
        (document.getElementById("resend-btn") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("login-btn") as? HTMLElement)?.style?.display = "block"
        pendingEmail = null

        WebAuthn.authenticateWithResponse().then { result ->
            if (result.success == true) {
                ApiClient.me().then { user ->
                    if (user != null && user.authenticated == true) {
                        if (user.language != null) {
                            I18n.setLang(user.language.toString())
                            localStorage.setItem("lang", user.language.toString())
                            updateLangButton()
                        }
                        checkProfileCompletion(user).then { needs ->
                            msg.className = "message success"
                            msg.textContent = I18n.t("success") + " " + I18n.t("redirecting")
                            if (needs) {
                                window.location.href = "/profile"
                            } else {
                                window.location.href = "/"
                            }
                            undefined
                        }
                    } else {
                        msg.className = "message error"
                        msg.textContent = I18n.t("loginFailed")
                    }
                    undefined
                }
            } else {
                if (result.email != null) {
                    pendingEmail = result.email.toString()
                    msg.className = "message error"
                    msg.textContent = I18n.t("emailNotVerified")
                    showResendButton()
                } else {
                    msg.className = "message error"
                    msg.textContent = I18n.t("authFailed")
                }
            }
            undefined
        }.catch { e ->
            msg.className = "message error"
            msg.textContent = e.asDynamic().message ?: I18n.t("authError")
            undefined
        }
    }

    private fun resendVerification() {
        val msg = document.getElementById("passkey-message") as? HTMLElement ?: return
        if (pendingEmail != null) {
            val formData = js("new URLSearchParams()")
            formData.append("email", pendingEmail)
            fetch("/api/auth/resend-verification", js("({method: 'POST', headers: {'Content-Type': 'application/x-www-form-urlencoded'}, body: formData.toString()})")).then { res ->
                res.json().then { result ->
                    if (result.success == true) {
                        msg.className = "message success"
                        msg.textContent = I18n.t("verificationEmailSent")
                        (document.getElementById("resend-btn") as? HTMLElement)?.style?.display = "none"
                    } else {
                        msg.className = "message error"
                        msg.textContent = result.message ?: I18n.t("failedToSendEmail")
                    }
                    undefined
                }
            }.catch {
                msg.className = "message error"
                msg.textContent = I18n.t("failedToSendEmail")
                undefined
            }
        } else {
            msg.className = "message error"
            msg.textContent = I18n.t("loginRequired")
            (document.getElementById("resend-btn") as? HTMLElement)?.style?.display = "none"
            (document.getElementById("login-btn") as? HTMLElement)?.style?.display = "block"
        }
    }

    private fun sendMagicLink() {
        val msg = document.getElementById("magic-link-message") as? HTMLElement ?: return
        val emailInput = document.getElementById("magic-email") as? HTMLInputElement ?: return
        val email = emailInput.value

        if (email.isEmpty()) {
            msg.className = "message error"
            msg.textContent = I18n.t("emailRequired")
            return
        }

        msg.textContent = I18n.t("sending")
        msg.className = ""

        RsaCrypto.getPublicKey().then { publicKey ->
            RsaCrypto.encrypt(email, publicKey.toString()).then { encrypted ->
                fetch("/api/auth/request-magic-link", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({encryptedData: encrypted})})")).then { res ->
                    res.json().then { result ->
                        if (result.success == true) {
                            msg.className = "message success"
                            msg.textContent = I18n.t("magicLinkSent")
                            emailInput.value = ""
                        } else {
                            msg.className = "message error"
                            msg.textContent = result.error ?: I18n.t("magicLinkFailed")
                        }
                        undefined
                    }
                }
            }
        }.catch {
            msg.className = "message error"
            msg.textContent = I18n.t("magicLinkFailed")
        }
    }

    private fun loginWithPassword() {
        val msg = document.getElementById("password-login-message") as? HTMLElement ?: return
        val emailInput = document.getElementById("password-email") as? HTMLInputElement ?: return
        val passwordInput = document.getElementById("password-password") as? HTMLInputElement ?: return
        val email = emailInput.value
        val password = passwordInput.value

        if (email.isEmpty() || password.isEmpty()) {
            msg.className = "message error"
            msg.textContent = I18n.t("emailPasswordRequired")
            return
        }

        msg.textContent = I18n.t("authenticating")
        msg.className = ""

        RsaCrypto.getPublicKey().then { publicKey ->
            RsaCrypto.encrypt(password, publicKey.toString()).then { encrypted ->
                fetch("/api/auth/login-with-password", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email: email, encryptedPassword: encrypted})})")).then { res ->
                    res.json().then { result ->
                        if (result.success == true) {
                            ApiClient.me().then { user ->
                                if (user != null && user.authenticated == true) {
                                    if (user.language != null) {
                                        I18n.setLang(user.language.toString())
                                        localStorage.setItem("lang", user.language.toString())
                                        updateLangButton()
                                    }
                                    checkProfileCompletion(user).then { needs ->
                                        msg.className = "message success"
                                        msg.textContent = I18n.t("success") + " " + I18n.t("redirecting")
                                        if (needs) {
                                            window.location.href = "/profile"
                                        } else {
                                            window.location.href = "/"
                                        }
                                        undefined
                                    }
                                }
                                undefined
                            }
                        } else {
                            msg.className = "message error"
                            msg.textContent = result.error ?: I18n.t("loginFailed")
                        }
                        undefined
                    }
                }
            }
        }.catch {
            msg.className = "message error"
            msg.textContent = I18n.t("loginFailed")
        }
    }

    private fun updateLangButton() {
        val btn = document.querySelector(".lang-dropbtn") as? HTMLElement ?: return
        val lang = localStorage.getItem("lang") ?: "en"
        val emojis = mapOf("en" to "\uD83C\uDDFA\uD83C\uDDF8", "es" to "\uD83C\uDDEA\uD83C\uDDF8", "fr" to "\uD83C\uDDEB\uD83C\uDDF7", "pt" to "\uD83C\uDDE7\uD83C\uDDF7", "zh" to "\uD83C\uDDE8\uD83C\uDDF3")
        btn.innerHTML = (emojis[lang] ?: "\uD83C\uDF10") + " \u25BC"
    }

    private fun showResendButton() {
        (document.getElementById("login-btn") as? HTMLElement)?.style?.display = "none"
        (document.getElementById("resend-btn") as? HTMLElement)?.let {
            it.style.display = "block"
            it.style.width = "100%"
        }
    }

    private suspend fun checkProfileCompletion(user: dynamic): Boolean {
        val roles = (user.activeRoles as? Array<*>)?.map { it.toString() } ?: emptyList()
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

    private fun Promise<*>.then(fn: (dynamic) -> dynamic): Promise<dynamic> = this.asDynamic().then(fn)
    private fun Promise<*>.catch(fn: (dynamic) -> dynamic): Promise<dynamic> = this.asDynamic().catch(fn)
}
