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
