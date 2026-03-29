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
            div {
                id="auth-form"
                classes = setOf("auth-form")
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
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "TEMPORAL_HOME"; id = "role-temporal-home"; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "offerTemporalHome"; +"Offer a temporary home" }
                        }
                    }
                    
                    div(classes = "temporal-home-fields") {
                        id = "temporal-home-section"
                        style = "display: none; margin-top: 1rem; padding: 1rem; background: #f5f5f5; border-radius: 8px;"
                        h3 { attributes["data-i18n"] = "temporalHomeProfile"; +"Temporal Home Profile" }
                        p { small { attributes["data-i18n"] = "temporalHomeDescription"; +"Provide your location details to help rescuers find temporary homes near them." } }
                        
                        label { htmlFor = "th-alias"; attributes["data-i18n"] = "homeAlias"; +"Home Name/Alias" }; input(InputType.text) { name = "thAlias"; id = "th-alias"; placeholder = "e.g., John's Home" }
                        label { htmlFor = "th-country"; attributes["data-i18n"] = "country"; +"Country" }; input(InputType.text) { name = "thCountry"; id = "th-country"; required = true }
                        label { htmlFor = "th-state"; attributes["data-i18n"] = "state"; +"State/Province" }; input(InputType.text) { name = "thState"; id = "th-state" }
                        label { htmlFor = "th-city"; attributes["data-i18n"] = "city"; +"City" }; input(InputType.text) { name = "thCity"; id = "th-city"; required = true }
                        label { htmlFor = "th-zip"; attributes["data-i18n"] = "zip"; +"ZIP/Postal Code" }; input(InputType.text) { name = "thZip"; id = "th-zip" }
                        label { htmlFor = "th-neighborhood"; attributes["data-i18n"] = "neighborhood"; +"Neighborhood" }; input(InputType.text) { name = "thNeighborhood"; id = "th-neighborhood" }
                    }
                    
                    p { id = "message"; +"" }
                    button(classes = "btn", type = ButtonType.submit) {
                        id="register-button"
                        attributes["data-i18n"] = "registerWithPasskey"; +"Register with Passkey" }
                }
                p { style = "margin-top: 1rem;"; small { attributes["data-i18n"] = "useFido"; +"Uses FIDO2 / WebAuthn - no password needed." } }
                div(classes = "form-actions") {
                    p { style = "margin-top: 1rem; text-align: center; width: 100%;"; attributes["data-i18n"] = "alreadyHaveAccount"; +"Already have an account?" }
                    a(href = "/login") { button(classes = "btn btn-secondary", type = ButtonType.button) {
                        id="register-page-login"
                        attributes["data-i18n"] = "login"; +"Login"
                    } }
                }
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
    if (document.getElementById('role-temporal-home').checked) roles.push('TEMPORAL_HOME');
    
    const temporalHomeSection = document.getElementById('temporal-home-section');
    let temporalHomeProfile = null;
    if (document.getElementById('role-temporal-home').checked) {
        temporalHomeSection.style.display = 'block';
        const thAlias = document.getElementById('th-alias').value.trim();
        const thCountry = document.getElementById('th-country').value.trim();
        const thState = document.getElementById('th-state').value.trim();
        const thCity = document.getElementById('th-city').value.trim();
        const thZip = document.getElementById('th-zip').value.trim();
        const thNeighborhood = document.getElementById('th-neighborhood').value.trim();
        
        if (!thCountry || !thCity) {
            const msg = document.getElementById('message');
            msg.className = 'message error';
            msg.textContent = 'Please fill in Country and City for temporal home profile.';
            return;
        }
        
        temporalHomeProfile = {
            alias: thAlias || thCity + ' Home',
            country: thCountry,
            state: thState || null,
            city: thCity,
            zip: thZip || null,
            neighborhood: thNeighborhood || null
        };
    } else {
        temporalHomeSection.style.display = 'none';
    }
    
    const msg = document.getElementById('message');
    if (!email || !displayName) { msg.className = 'message error'; msg.textContent = 'Please fill in all fields.'; return; }
    const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
    if (!emailRegex.test(email)) { msg.className = 'message error'; msg.textContent = 'Please enter a valid email address.'; return; }
    try {
        msg.textContent = 'Creating passkey...';
        const result = await webauthn.register(email, displayName, roles, temporalHomeProfile);
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
