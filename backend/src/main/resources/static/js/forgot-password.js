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
