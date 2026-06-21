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