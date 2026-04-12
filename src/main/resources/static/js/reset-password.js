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
