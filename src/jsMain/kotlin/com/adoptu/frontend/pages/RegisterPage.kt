package com.adoptu.frontend.pages

import com.adoptu.frontend.*
import kotlinx.browser.document
import org.w3c.dom.*
import kotlin.js.json

object RegisterPage {
    suspend fun init() {
        (document.getElementById("password") as? HTMLInputElement)?.addEventListener("input", { e ->
            validatePassword((e as InputEvent).target.asDynamic().value.toString())
            undefined
        })

        (document.getElementById("register-form") as? HTMLFormElement)?.onsubmit = { e ->
            e.preventDefault()
            handleRegister()
            undefined
        }
    }

    private fun validatePassword(password: String): Boolean {
        val checks = listOf(
            password.length >= 8 to "At least 8 characters",
            Regex("[A-Z]").containsMatchIn(password) to "Contains uppercase letter",
            Regex("[a-z]").containsMatchIn(password) to "Contains lowercase letter",
            Regex("[0-9]").containsMatchIn(password) to "Contains number",
            Regex("[!@#\$%^&*(),.?\":{}|<>\\-_+=()\\[\\]\\\\|°º«»¿]").containsMatchIn(password) to "Contains symbol"
        )

        var html = ""
        checks.forEach { (valid, label) ->
            val color = if (valid) "#28a745" else "#dc3545"
            val icon = if (valid) "&#10003;" else "&#10007;"
            html += "<div style=\"color:$color; font-size: 0.85rem; margin: 0.2rem 0;\">$icon $label</div>"
        }
        (document.getElementById("password-checks") as? HTMLElement)?.innerHTML = html
        return checks.all { it.first }
    }

    private fun handleRegister() {
        val email = (document.getElementById("email") as? HTMLInputElement)?.value?.trim() ?: ""
        val displayName = (document.getElementById("displayName") as? HTMLInputElement)?.value?.trim() ?: ""
        val password = (document.getElementById("password") as? HTMLInputElement)?.value ?: ""
        val confirmPassword = (document.getElementById("confirmPassword") as? HTMLInputElement)?.value ?: ""
        val msg = document.getElementById("message") as? HTMLElement ?: return

        val roles = mutableListOf("ADOPTER")
        if ((document.getElementById("role-rescuer") as? HTMLInputElement)?.checked == true) roles.add("RESCUER")
        if ((document.getElementById("role-photographer") as? HTMLInputElement)?.checked == true) roles.add("PHOTOGRAPHER")
        if ((document.getElementById("role-temporal-home") as? HTMLInputElement)?.checked == true) roles.add("TEMPORAL_HOME")

        if (email.isEmpty() || displayName.isEmpty()) {
            msg.className = "message error"
            msg.textContent = "Please fill in all required fields."
            return
        }

        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) {
            msg.className = "message error"
            msg.textContent = "Please enter a valid email address."
            return
        }

        if (password.isNotEmpty() && password != confirmPassword) {
            msg.className = "message error"
            msg.textContent = "Passwords do not match."
            return
        }

        if (password.isNotEmpty()) {
            val isValid = validatePassword(password)
            if (!isValid) {
                msg.className = "message error"
                msg.textContent = "Password does not meet requirements."
                return
            }
        }

        if (password.isNotEmpty()) {
            msg.textContent = "Creating account with password..."
            RsaCrypto.getPublicKey().then { key ->
                RsaCrypto.encrypt(password, key.toString()).then { encrypted ->
                    fetch("/api/auth/register-password", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email: email, displayName: displayName, roles: roles.join(','), encryptedPassword: encrypted})})")).then { res ->
                        res.json().then { result ->
                            if (result.success == true) {
                                msg.className = "message success"
                                msg.textContent = "Account created! Check your email to verify your account."
                                js("setTimeout")(suspend { window.location.href = "/login" }, 2000)
                            } else {
                                msg.className = "message error"
                                msg.textContent = result.error ?: "Registration failed."
                            }
                            undefined
                        }
                    }
                }
            }.catch { err ->
                msg.className = "message error"
                msg.textContent = err.asDynamic().message ?: "Registration error."
                undefined
            }
        } else {
            msg.textContent = "Creating passkey..."
            WebAuthn.register(email, displayName, roles).then { result ->
                if (result != null) {
                    msg.className = "message success"
                    msg.textContent = "Success! Redirecting..."
                    if (result.needsProfileCompletion == true) {
                        window.location.href = "/profile"
                    } else {
                        window.location.href = "/"
                    }
                } else {
                    msg.className = "message error"
                    msg.textContent = "Registration failed."
                }
                undefined
            }.catch { err ->
                msg.className = "message error"
                msg.textContent = err.asDynamic().message ?: "Registration error."
                undefined
            }
        }
    }
}
