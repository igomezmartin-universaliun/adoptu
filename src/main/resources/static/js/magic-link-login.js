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
