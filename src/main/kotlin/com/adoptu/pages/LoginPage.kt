package com.adoptu.pages

import kotlinx.html.*

fun HTML.loginPage() {
    commonHead("Login - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div { classes = setOf("auth-form")
                h1 { attributes["data-i18n"] = "loginWithPasskey"; +"Login with Passkey" }
                p { id = "message"; +"" }
                button(classes = "btn", type = ButtonType.button) { id = "login-btn"; attributes["data-i18n"] = "signInWithPasskey"; +"Sign in with Passkey" }
                p { style = "margin-top: 1rem;"; small { attributes["data-i18n"] = "usesFido"; +"Uses FIDO2 / WebAuthn for secure passwordless authentication." } }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/webauthn.js") {}
        script { unsafe { raw("""
document.getElementById('login-btn').onclick = async () => {
    const msg = document.getElementById('message');
    try {
        msg.textContent = 'Authenticating...';
        const ok = await webauthn.authenticate();
        if (ok) { msg.className = 'message success'; msg.textContent = 'Success! Redirecting...'; location.href = '/'; }
        else { msg.className = 'message error'; msg.textContent = 'Authentication failed.'; }
    } catch (e) { msg.className = 'message error'; msg.textContent = e.message || 'Authentication error.'; }
};
""".trimIndent()) } }
    }
}
