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
            div(classes = "auth-form-container") {
                id = "passkey-form"
                div(classes = "auth-form") {
                    h1 { attributes["data-i18n"] = "loginWithPasskey"; +"Login with Passkey" }
                    p { id = "passkey-message"; +"" }
                    button(classes = "btn", type = ButtonType.button) { id = "login-btn"; attributes["data-i18n"] = "signInWithPasskey"; style = "width: 100%; display: block; margin-bottom: 8px;"; +"Sign in with Passkey" }
                    button(classes = "btn btn-secondary", type = ButtonType.button) { id = "resend-btn"; attributes["data-i18n"] = "resendVerificationEmail"; style = "width: 100%; display: none;"; onClick = "this.style.display='none'"; +"Resend Verification Email" }
                    p { small { attributes["data-i18n"] = "usesFido"; +"Uses FIDO2 / WebAuthn for secure passwordless authentication." } }
                }
            }

            div(classes = "auth-form-container") {
                id = "magic-link-form"
                div(classes = "auth-form") {
                    h1 { attributes["data-i18n"] = "loginWithMagicLink"; +"Login with Email Link" }
                    p { attributes["data-i18n"] = "magicLinkDesc"; +"We'll send you a login link to your email (valid for 5 minutes)." }
                    div(classes = "form-row") {
                        label { htmlFor = "magic-email"; attributes["data-i18n"] = "email"; +"Email" }
                        input(InputType.email) { id = "magic-email"; required = true }
                    }
                    p { id = "magic-link-message"; +"" }
                    button(classes = "btn", type = ButtonType.button) { id = "magic-link-btn"; attributes["data-i18n"] = "sendMagicLink"; +"Send Login Link" }
                }
            }

            div(classes = "auth-form-container") {
                id = "password-form"
                div(classes = "auth-form") {
                    h1 { attributes["data-i18n"] = "loginWithPassword"; +"Login with Password" }
                    div(classes = "form-row") {
                        label { htmlFor = "password-email"; attributes["data-i18n"] = "email"; +"Email" }
                        input(InputType.email) { id = "password-email"; required = true }
                    }
                    div(classes = "form-row") {
                        label { htmlFor = "password-password"; attributes["data-i18n"] = "password"; +"Password" }
                        input(InputType.password) { id = "password-password"; required = true }
                    }
                    p { id = "password-login-message"; +"" }
                    button(classes = "btn", type = ButtonType.button) { id = "password-login-btn"; attributes["data-i18n"] = "signIn"; +"Sign In" }
                    p { }
                    a(href = "/forgot-password") { attributes["data-i18n"] = "forgotPassword"; +"Forgot Password?" }
                }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/webauthn.js") {}
        script(src = "/static/js/crypto.js") {}
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
    document.getElementById('resend-btn').style.width = '100%';
}

let pendingEmail = null;

document.getElementById('login-btn').onclick = async () => {
    const msg = document.getElementById('passkey-message');
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
    const msg = document.getElementById('passkey-message');
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

document.getElementById('magic-link-btn').onclick = async () => {
    const msg = document.getElementById('magic-link-message');
    const email = document.getElementById('magic-email').value;
    
    if (!email) {
        msg.className = 'message error';
        msg.textContent = i18n.t('emailRequired') || 'Email is required';
        return;
    }
    
    msg.textContent = i18n.t('sending') || 'Sending...';
    msg.className = '';
    
    try {
        const publicKey = await getPublicKey();
        const encryptedEmail = await rsaCrypto.encrypt(email, publicKey);
        
        const res = await fetch('/api/auth/request-magic-link', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ encryptedData: encryptedEmail })
        });
        
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = i18n.t('magicLinkSent') || 'Check your inbox! We sent you a login link (valid for 5 minutes).';
            document.getElementById('magic-email').value = '';
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || i18n.t('magicLinkFailed') || 'Failed to send login link.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('magicLinkFailed') || 'Failed to send login link.';
    }
};

document.getElementById('password-login-btn').onclick = async () => {
    const msg = document.getElementById('password-login-message');
    const email = document.getElementById('password-email').value;
    const password = document.getElementById('password-password').value;
    
    if (!email || !password) {
        msg.className = 'message error';
        msg.textContent = i18n.t('emailPasswordRequired') || 'Email and password are required';
        return;
    }
    
    msg.textContent = i18n.t('authenticating') || 'Authenticating...';
    msg.className = '';
    
    try {
        const publicKey = await getPublicKey();
        const encryptedPassword = await rsaCrypto.encrypt(password, publicKey);
        
        const res = await fetch('/api/auth/login-with-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                email: email,
                encryptedPassword: encryptedPassword
            })
        });
        
        const result = await res.json();
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
            }
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || i18n.t('loginFailed') || 'Invalid credentials.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('loginFailed') || 'Login failed.';
    }
};

document.querySelectorAll('.auth-form-container').forEach(f => f.style.display = 'block');

if (window.location.hash === '#magic-link') {
    document.getElementById('tab-magic-link').click();
} else if (window.location.hash === '#password') {
    document.getElementById('tab-password').click();
}
""".trimIndent()) } }
    }
}
