package com.adoptu.pages

import kotlinx.html.*

fun HTML.registerPage() {
    commonHead("Register - Adopt-U")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { attributes["data-i18n"] = "login"; +"Login" }
                languageDropdown()
            }
        }
        main {
            div { classes = setOf("auth-form")
                h1 { attributes["data-i18n"] = "registerNewAccount"; +"Create Account" }
                form { id = "register-form"
                    label { htmlFor = "username"; attributes["data-i18n"] = "username"; +"Username" }; input(InputType.text) { name = "username"; id = "username"; required = true }
                    label { htmlFor = "displayName"; attributes["data-i18n"] = "displayName"; +"Display Name" }; input(InputType.text) { name = "displayName"; id = "displayName"; required = true }
                    label { htmlFor = "role"; +"I want to" }
                    select { id = "role"; name = "role"
                        option { value = "ADOPTER"; attributes["data-i18n"] = "adoptPet"; +"Adopt a pet" }
                        option { value = "RESCUER"; attributes["data-i18n"] = "publishPets"; +"Publish pets for adoption" }
                    }
                    p { id = "message"; +"" }
                    button(classes = "btn", type = ButtonType.submit) { +"Register with Passkey" }
                }
                p { style = "margin-top: 1rem;"; small { attributes["data-i18n"] = "useFido"; +"Uses FIDO2 / WebAuthn - no password needed." } }
                p { style = "margin-top: 1rem;"; attributes["data-i18n"] = "alreadyHaveAccount"; +"Already have an account? "; a("/login") { attributes["data-i18n"] = "login"; +"Login" } }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/webauthn.js") {}
        script { unsafe { raw("""
document.getElementById('register-form').onsubmit = async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const displayName = document.getElementById('displayName').value.trim();
    const role = document.getElementById('role').value;
    const msg = document.getElementById('message');
    if (!username || !displayName) { msg.className = 'message error'; msg.textContent = 'Please fill in all fields.'; return; }
    try {
        msg.textContent = 'Creating passkey...';
        const ok = await webauthn.register(username, displayName, role);
        if (ok) { msg.className = 'message success'; msg.textContent = 'Success! Redirecting...'; location.href = '/'; }
        else { msg.className = 'message error'; msg.textContent = 'Registration failed.'; }
    } catch (err) { msg.className = 'message error'; msg.textContent = err.message || 'Registration error.'; }
};
""".trimIndent()) } }
    }
}
