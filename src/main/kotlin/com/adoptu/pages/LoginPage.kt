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
const langEmojis = { en: '🇺🇸', es: '🇪🇸', fr: '🇫🇷', pt: '🇧🇷', zh: '🇨🇳' };
const langLabels = { en: 'English', es: 'Español', fr: 'Français', pt: 'Português', zh: '中文' };

function updateLangButton() {
    const lang = localStorage.getItem('lang') || 'en';
    const btn = document.querySelector('.lang-dropbtn');
    if (btn) {
        btn.innerHTML = langEmojis[lang] + ' ▼';
    }
}

async function checkProfileCompletion(user) {
    const roles = user.activeRoles || [];
    
    if (roles.includes('PHOTOGRAPHER')) {
        if (!user.photographerCountry || !user.photographerState) {
            return true;
        }
    }
    
    if (roles.includes('TEMPORAL_HOME')) {
        try {
            await api.getTemporalHome();
        } catch (e) {
            return true;
        }
    }
    
    return false;
}

document.getElementById('login-btn').onclick = async () => {
    const msg = document.getElementById('message');
    try {
        msg.textContent = 'Authenticating...';
        const ok = await webauthn.authenticate();
        if (ok) {
            const user = await api.me();
            if (user && user.authenticated) {
                if (user.language) {
                    i18n.setLang(user.language);
                    localStorage.setItem('lang', user.language);
                    updateLangButton();
                }
                const needsProfileCompletion = await checkProfileCompletion(user);
                if (needsProfileCompletion) {
                    msg.className = 'message success'; msg.textContent = 'Success! Redirecting...';
                    location.href = '/profile';
                } else {
                    msg.className = 'message success'; msg.textContent = 'Success! Redirecting...';
                    location.href = '/';
                }
            } else {
                msg.className = 'message error'; msg.textContent = 'Login failed.';
            }
        }
        else { msg.className = 'message error'; msg.textContent = 'Authentication failed.'; }
    } catch (e) { msg.className = 'message error'; msg.textContent = e.message || 'Authentication error.'; }
};
""".trimIndent()) } }
    }
}
