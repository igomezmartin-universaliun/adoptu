package com.adoptu.pages

import kotlinx.html.*

fun HTML.registerPage() {
    commonHead("Register - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div { classes = setOf("auth-form")
                h1 { attributes["data-i18n"] = "registerNewAccount"; +"Create Account" }
                form { id = "register-form"
                    label { htmlFor = "email"; attributes["data-i18n"] = "email"; +"Email" }; input(InputType.email) { name = "email"; id = "email"; required = true }
                    label { htmlFor = "displayName"; attributes["data-i18n"] = "displayName"; +"Display Name" }; input(InputType.text) { name = "displayName"; id = "displayName"; required = true }
                    
                    label { attributes["data-i18n"] = "selectAdditionalRoles"; +"Select additional roles:" }
                    div(classes = "checkbox-group") {
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "ADOPTER"; id = "role-adopter"; checked = true; disabled = true; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "adoptPet"; +"Adopt a pet" }
                            +" (required)"
                        }
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "RESCUER"; id = "role-rescuer"; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "publishPets"; +"Publish pets for adoption" }
                        }
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "PHOTOGRAPHER"; id = "role-photographer"; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "offerPhotography"; +"Offer photography services" }
                        }
                    }
                    
                    p { id = "message"; +"" }
                    button(classes = "btn", type = ButtonType.submit) { attributes["data-i18n"] = "registerWithPasskey"; +"Register with Passkey" }
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
    const email = document.getElementById('email').value.trim();
    const displayName = document.getElementById('displayName').value.trim();
    const roles = ['ADOPTER'];
    if (document.getElementById('role-rescuer').checked) roles.push('RESCUER');
    if (document.getElementById('role-photographer').checked) roles.push('PHOTOGRAPHER');
    const msg = document.getElementById('message');
    if (!email || !displayName) { msg.className = 'message error'; msg.textContent = 'Please fill in all fields.'; return; }
    const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
    if (!emailRegex.test(email)) { msg.className = 'message error'; msg.textContent = 'Please enter a valid email address.'; return; }
    try {
        msg.textContent = 'Creating passkey...';
        const result = await webauthn.register(email, displayName, roles);
        if (result) { 
            msg.className = 'message success'; 
            msg.textContent = 'Success! Redirecting...';
            if (result.needsProfileCompletion) {
                location.href = '/profile';
            } else {
                location.href = '/';
            }
        }
        else { msg.className = 'message error'; msg.textContent = 'Registration failed.'; }
    } catch (err) { msg.className = 'message error'; msg.textContent = err.message || 'Registration error.'; }
};
""".trimIndent()) } }
    }
}
