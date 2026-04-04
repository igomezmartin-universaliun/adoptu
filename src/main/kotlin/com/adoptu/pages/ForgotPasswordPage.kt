package com.adoptu.pages

import kotlinx.html.*

fun HTML.forgotPasswordPage() {
    commonHead("Forgot Password - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div {
                id = "auth-form"
                classes = setOf("auth-form")
                h1 { +"Forgot Password" }
                p { +"Enter your email and we'll send you a link to reset your password." }
                div(classes = "form-row") {
                    label { htmlFor = "email"; +"Email" }
                    input(InputType.email) { id = "email"; required = true }
                }
                p { id = "message"; +"" }
                button(classes = "btn", type = ButtonType.button) { id = "submit-btn"; +"Send Reset Link" }
                p { }
                a(href = "/login") { +"Back to Login" }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/crypto.js") {}
        script { unsafe { raw("""
document.getElementById('submit-btn').onclick = async () => {
    const msg = document.getElementById('message');
    const email = document.getElementById('email').value;
    if (!email) {
        msg.className = 'message error';
        msg.textContent = 'Email is required';
        return;
    }
    msg.textContent = 'Sending...';
    msg.className = '';
    try {
        const publicKey = await getPublicKey();
        const encryptedEmail = await rsaCrypto.encrypt(email, publicKey);
        const res = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ encryptedData: encryptedEmail })
        });
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = 'Password reset link sent! Check your email.';
            document.getElementById('email').value = '';
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || 'Failed to send reset link.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = 'Failed to send reset link.';
    }
};
""") } }
    }
}

fun HTML.resetPasswordPage() {
    commonHead("Reset Password - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div {
                id = "auth-form"
                classes = setOf("auth-form")
                h1 { +"Reset Password" }
                p { +"Enter your new password below." }
                div(classes = "form-row") {
                    label { htmlFor = "password"; +"New Password" }
                    input(InputType.password) { id = "password"; required = true; minLength = "8" }
                }
                div(classes = "form-row") {
                    label { htmlFor = "confirm-password"; +"Confirm Password" }
                    input(InputType.password) { id = "confirm-password"; required = true; minLength = "8" }
                }
                p { id = "message"; +"" }
                button(classes = "btn", type = ButtonType.button) { id = "submit-btn"; +"Reset Password" }
                p { }
                a(href = "/login") { +"Back to Login" }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/crypto.js") {}
        script { unsafe { raw("""
function getTokenFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('token');
}
document.getElementById('submit-btn').onclick = async () => {
    const msg = document.getElementById('message');
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirm-password').value;
    const token = getTokenFromUrl();
    if (!token) {
        msg.className = 'message error';
        msg.textContent = 'Invalid or missing token.';
        return;
    }
    if (!password || password.length < 8) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordTooShort') || 'Password must be at least 8 characters.';
        return;
    }
    if (!/[A-Z]/.test(password)) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordNeedUppercase') || 'Password needs at least 1 uppercase letter.';
        return;
    }
    if (!/[a-z]/.test(password)) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordNeedLowercase') || 'Password needs at least 1 lowercase letter.';
        return;
    }
    if (!/[0-9]/.test(password)) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordNeedNumber') || 'Password needs at least 1 number.';
        return;
    }
    if (!/[!@#$%^&*(),.?":{}|<>\-_+=/\[\]\\|°º«»¿]/.test(password)) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordNeedSymbol') || 'Password needs at least 1 symbol.';
        return;
    }
    if (password !== confirmPassword) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordsDoNotMatch') || 'Passwords do not match.';
        return;
    }
    msg.textContent = 'Resetting password...';
    msg.className = '';
    try {
        const publicKey = await getPublicKey();
        const encryptedPassword = await rsaCrypto.encrypt(password, publicKey);
        const res = await fetch('/api/auth/reset-password?token=' + encodeURIComponent(token), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ encryptedData: encryptedPassword })
        });
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = 'Password reset successfully! You can now login.';
            document.getElementById('password').value = '';
            document.getElementById('confirm-password').value = '';
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || 'Failed to reset password.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = 'Failed to reset password.';
    }
};
const token = getTokenFromUrl();
if (!token) {
    document.getElementById('message').className = 'message error';
    document.getElementById('message').textContent = 'Invalid or missing token.';
    document.getElementById('submit-btn').disabled = true;
}
""") } }
    }
}

fun HTML.magicLinkLoginPage() {
    commonHead("Magic Link Login - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div {
                id = "auth-form"
                classes = setOf("auth-form")
                h1 { +"Email Link Login" }
                p { id = "message"; +"Verifying..." }
            }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
async function checkLogin() {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const msg = document.getElementById('message');
    if (!token) {
        msg.className = 'message error';
        msg.textContent = 'Invalid or missing token.';
        return;
    }
    try {
        const res = await fetch('/api/auth/magic-link-login?token=' + encodeURIComponent(token));
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = 'Login successful! Redirecting...';
            setTimeout(() => { location.href = '/'; }, 1000);
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || 'Login failed. The link may be invalid or expired.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = 'Login failed.';
    }
}
checkLogin();
""") } }
    }
}

fun HTML.emailChangeVerificationPage() {
    commonHead("Email Change - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div {
                id = "auth-form"
                classes = setOf("auth-form")
                h1 { +"Email Change" }
                p { id = "message"; +"Verifying..." }
            }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
async function checkEmailChange() {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const msg = document.getElementById('message');
    if (!token) {
        msg.className = 'message error';
        msg.textContent = 'Invalid or missing token.';
        return;
    }
    try {
        const res = await fetch('/api/users/verify-email-change?token=' + encodeURIComponent(token));
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = result.message || 'Email changed successfully!';
        } else {
            msg.className = 'message error';
            msg.textContent = result.message || 'Failed to change email. The link may be invalid or expired.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = 'Failed to change email.';
    }
}
checkEmailChange();
""") } }
    }
}
