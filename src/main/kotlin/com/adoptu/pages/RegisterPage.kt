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
                            span { attributes["data-i18n"] = "provideTemporaryHome"; +"Provide temporary home for pets" }
                        }
                    }
                    
                    div(classes = "divider") {
                        p { +"Or create a password" }
                    }
                    
                    div(classes = "password-section") {
                        div(classes = "form-row") {
                            label { htmlFor = "password"; attributes["data-i18n"] = "password"; +"Password" }
                            input(InputType.password) { id = "password"; name = "password" }
                        }
                        div(classes = "form-row") {
                            label { htmlFor = "confirmPassword"; attributes["data-i18n"] = "confirmPassword"; +"Confirm Password" }
                            input(InputType.password) { id = "confirmPassword"; name = "confirmPassword" }
                        }
                        div(classes = "password-requirements") {
                            p { id = "password-requirements-text"; attributes["data-i18n"] = "passwordRequirements"; +"Password must be at least 8 characters and contain uppercase, lowercase, number, and symbol" }
                            div { id = "password-checks"; +"" }
                        }
                    }
                    
                    p { id = "message"; +"" }
                    button(classes = "btn", type = ButtonType.submit) {
                        id="register-button"
                        attributes["data-i18n"] = "register"; +"Register" }
                }
                div(classes = "security-info") {
                    p { style = "margin: 0 0 0.5rem 0; font-weight: bold;"; attributes["data-i18n"] = "securityInfo"; +"Security Information" }
                    ul {
                        style = "margin: 0; padding-left: 1.5rem;"
                        li { attributes["data-i18n"] = "passkeyMostSecure"; +"Passkey: Most secure, recommended" }
                        li { style = "margin-top: 0.25rem;"; attributes["data-i18n"] = "passkeyBrowserCompat"; +"Passkey may not work on all browsers" }
                        li { style = "margin-top: 0.25rem;"; attributes["data-i18n"] = "passwordLeastSecure"; +"Password: Less secure, not recommended" }
                    }
                }
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
        script(src = "/static/js/crypto.js") {}
        script { unsafe { raw("""
async function validatePassword(password) {
    const checks = [
        { valid: password.length >= 8, label: 'At least 8 characters' },
        { valid: /[A-Z]/.test(password), label: 'Contains uppercase letter' },
        { valid: /[a-z]/.test(password), label: 'Contains lowercase letter' },
        { valid: /[0-9]/.test(password), label: 'Contains number' },
        { valid: /[!@#\$%^&*(),.?\":{}|<>\\-_+=()\\[\\]\\\\|°º«»¿]/.test(password), label: 'Contains symbol' }
    ];
    
    let html = '';
    checks.forEach(check => {
        const color = check.valid ? '#28a745' : '#dc3545';
        html += '<div style="color:' + color + '; font-size: 0.85rem; margin: 0.2rem 0;">' + (check.valid ? '&#10003;' : '&#10007;') + ' ' + check.label + '</div>';
    });
    document.getElementById('password-checks').innerHTML = html;
    
    return checks.every(c => c.valid);
}

document.getElementById('password')?.addEventListener('input', (e) => {
    validatePassword(e.target.value);
});

document.getElementById('register-form').onsubmit = async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const displayName = document.getElementById('displayName').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const roles = ['ADOPTER'];
    if (document.getElementById('role-rescuer').checked) roles.push('RESCUER');
    if (document.getElementById('role-photographer').checked) roles.push('PHOTOGRAPHER');
    if (document.getElementById('role-temporal-home').checked) roles.push('TEMPORAL_HOME');
    
    const msg = document.getElementById('message');
    if (!email || !displayName) { msg.className = 'message error'; msg.textContent = 'Please fill in all required fields.'; return; }
    const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
    if (!emailRegex.test(email)) { msg.className = 'message error'; msg.textContent = 'Please enter a valid email address.'; return; }
    
    if (password && password !== confirmPassword) {
        msg.className = 'message error'; 
        msg.textContent = 'Passwords do not match.'; 
        return;
    }
    
    if (password) {
        const isValid = await validatePassword(password);
        if (!isValid) {
            msg.className = 'message error'; 
            msg.textContent = 'Password does not meet requirements.'; 
            return;
        }
    }
    
    try {
        if (password) {
            msg.textContent = 'Creating account with password...';
            const publicKey = await getPublicKey();
            const encryptedPassword = await rsaCrypto.encrypt(password, publicKey);
            
            const res = await fetch('/api/auth/register-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    email, 
                    displayName, 
                    roles: roles.join(','),
                    encryptedPassword 
                })
            });
            
            const result = await res.json();
            if (result.success) {
                msg.className = 'message success'; 
                msg.textContent = 'Account created! Check your email to verify your account.';
                setTimeout(() => location.href = '/login', 2000);
            } else {
                msg.className = 'message error'; 
                msg.textContent = result.error || 'Registration failed.';
            }
        } else {
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
            } else { 
                msg.className = 'message error'; 
                msg.textContent = 'Registration failed.'; 
            }
        }
    } catch (err) { 
        msg.className = 'message error'; 
        msg.textContent = err.message || 'Registration error.'; 
    }
};
""".trimIndent()) } }
    }
}
