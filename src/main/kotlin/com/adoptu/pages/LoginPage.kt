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
            div {
                id="auth-form"
                classes = setOf("auth-form")
                h1 { attributes["data-i18n"] = "loginWithPasskey"; +"Login with Passkey" }
                p { id = "message"; +"" }
                button(classes = "btn", type = ButtonType.button) { id = "login-btn"; attributes["data-i18n"] = "signInWithPasskey"; +"Sign in with Passkey" }
                button(classes = "btn btn-secondary", type = ButtonType.button) { id = "resend-btn"; style = "display: none; margin-top: 0.5rem;"; attributes["data-i18n"] = "resendVerificationEmail"; +"Resend Verification Email" }
                p { style = "margin-top: 1rem;"; small { attributes["data-i18n"] = "usesFido"; +"Uses FIDO2 / WebAuthn for secure passwordless authentication." } }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/webauthn.js") {}
        script { unsafe { raw("""

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

function showResendButton() {
    document.getElementById('login-btn').style.display = 'none';
    document.getElementById('resend-btn').style.display = 'block';
}

let pendingEmail = null;

document.getElementById('login-btn').onclick = async () => {
    const msg = document.getElementById('message');
    msg.textContent = i18n.t('authenticating') || 'Authenticating...';
    msg.className = '';
    document.getElementById('resend-btn').style.display = 'none';
    document.getElementById('login-btn').style.display = 'block';
    pendingEmail = null;
    
    try {
        const result = await webauthn.authenticateWithResponse();
        if (result.success) {
            const user = await api.me();
            if (user && user.authenticated) {
                if (user.language) {
                    i18n.setLang(user.language);
                    localStorage.setItem('lang', user.language);
                    updateLangButton();
                }
                const needsProfileCompletion = await checkProfileCompletion(user);
                if (needsProfileCompletion) {
                    msg.className = 'message success'; msg.textContent = i18n.t('success') + ' ' + i18n.t('redirecting') || 'Success! Redirecting...';
                    location.href = '/profile';
                } else {
                    msg.className = 'message success'; msg.textContent = i18n.t('success') + ' ' + i18n.t('redirecting') || 'Success! Redirecting...';
                    location.href = '/';
                }
            } else {
                msg.className = 'message error'; msg.textContent = i18n.t('loginFailed') || 'Login failed.';
            }
        } else {
            if (result.email) {
                pendingEmail = result.email;
                msg.className = 'message error'; 
                msg.textContent = i18n.t('emailNotVerified');
                showResendButton();
            } else {
                msg.className = 'message error'; msg.textContent = i18n.t('authFailed') || 'Authentication failed.';
            }
        }
    } catch (e) { 
        msg.className = 'message error'; 
        msg.textContent = e.message || i18n.t('authError') || 'Authentication error.';
    }
};

document.getElementById('resend-btn').onclick = async () => {
    const msg = document.getElementById('message');
    try {
        if (pendingEmail) {
            const formData = new URLSearchParams();
            formData.append('email', pendingEmail);
            const res = await fetch('/api/auth/resend-verification', { 
                method: 'POST', 
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData 
            });
            const result = await res.json();
            if (result.success) {
                msg.className = 'message success';
                msg.textContent = i18n.t('verificationEmailSent') || 'Verification email sent. Please check your inbox.';
                document.getElementById('resend-btn').style.display = 'none';
            } else {
                msg.className = 'message error';
                msg.textContent = result.message || i18n.t('failedToSendEmail') || 'Failed to send verification email.';
            }
        } else {
            msg.className = 'message error';
            msg.textContent = i18n.t('loginRequired') || 'Please login first.';
            document.getElementById('resend-btn').style.display = 'none';
            document.getElementById('login-btn').style.display = 'block';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('failedToSendEmail') || 'Failed to send verification email.';
    }
};
""".trimIndent()) } }
    }
}
