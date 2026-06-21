let currentRoles = [];
let hasTemporalHomeProfile = false;
let hasShelterProfile = false;
let hasSterilizationProfile = false;
async function load() {
    let user = window.cachedUserData;
    if (!user && window.userDataPromise) {
        user = await window.userDataPromise;
    }
    if (!user) {
        try {
            user = await api.me();
            window.cachedUserData = user;
        } catch (e) {
            console.error('Error loading profile:', e);
            return;
        }
    }
    if (user.authenticated === false) { location.href = '/login'; return; }
    
    document.getElementById('email').value = user.email || '';
    document.getElementById('displayName').value = user.displayName || '';
    document.getElementById('language').value = user.language || 'en';
    currentRoles = user.activeRoles || [];
    console.log('Active roles:', currentRoles);
    
    const rescuer = document.getElementById('role-rescuer');
    const photographer = document.getElementById('role-photographer');
    const temporalHome = document.getElementById('role-temporal-home');
    const shelter = document.getElementById('role-shelter');
    const sterilization = document.getElementById('role-sterilization');
    console.log('Checkboxes found:', { rescuer, photographer, temporalHome, shelter, sterilization });
    
    try {
        await api.getTemporalHome();
        hasTemporalHomeProfile = true;
    } catch(e) { hasTemporalHomeProfile = false; }
    
    try {
        const shelterRes = await fetch('/api/users/shelter');
        if (shelterRes.ok) {
            hasShelterProfile = true;
        }
    } catch(e) { hasShelterProfile = false; }
    
    try {
        const sterilizationRes = await fetch('/api/users/sterilization-location');
        if (sterilizationRes.ok) {
            hasSterilizationProfile = true;
        }
    } catch(e) { hasSterilizationProfile = false; }
    
    if (rescuer) rescuer.checked = currentRoles.includes('RESCUER');
    if (photographer) photographer.checked = currentRoles.includes('PHOTOGRAPHER');
    if (temporalHome) temporalHome.checked = currentRoles.includes('TEMPORAL_HOME');
    if (shelter) shelter.checked = currentRoles.includes('SHELTER');
    if (sterilization) sterilization.checked = currentRoles.includes('STERILIZATION_SERVICE');
    
    if (photographer) {
        photographer.addEventListener('change', () => {
            document.querySelector('.photographer-section').style.display = photographer.checked ? 'block' : 'none';
        });
    }
    
    if (temporalHome) {
        temporalHome.addEventListener('change', () => {
            document.querySelector('.temporal-home-section').style.display = temporalHome.checked ? 'block' : 'none';
        });
    }
    
    if (shelter) {
        shelter.addEventListener('change', () => {
            document.querySelector('.shelter-section').style.display = shelter.checked ? 'block' : 'none';
        });
    }
    
    if (sterilization) {
        sterilization.addEventListener('change', () => {
            document.querySelector('.sterilization-section').style.display = sterilization.checked ? 'block' : 'none';
        });
    }
    
    const primaryRole = currentRoles[0] || 'ADOPTER';
    
    if (primaryRole === 'PHOTOGRAPHER' || primaryRole === 'ADMIN' || currentRoles.includes('PHOTOGRAPHER')) {
        document.querySelector('.photographer-section').style.display = 'block';
        loadPhotographer();
    }
    
    if (primaryRole === 'TEMPORAL_HOME' || primaryRole === 'ADMIN' || currentRoles.includes('TEMPORAL_HOME')) {
        document.querySelector('.temporal-home-section').style.display = 'block';
        loadTemporalHome();
    }
    
    if (primaryRole === 'SHELTER' || primaryRole === 'ADMIN' || currentRoles.includes('SHELTER')) {
        document.querySelector('.shelter-section').style.display = 'block';
        loadShelter();
    }
    
    if (primaryRole === 'STERILIZATION_SERVICE' || primaryRole === 'ADMIN' || currentRoles.includes('STERILIZATION_SERVICE')) {
        document.querySelector('.sterilization-section').style.display = 'block';
        loadSterilization();
    }
}
function loadPhotographer() {
    const user = window.cachedUserData;
    if (user) {
        document.getElementById('photographerFee').value = user.photographerFee || 0;
        document.getElementById('photographerCurrency').value = user.photographerCurrency || 'USD';
        document.getElementById('photographerCountry').value = user.photographerCountry || '';
        document.getElementById('photographerState').value = user.photographerState || '';
    }
}
async function loadTemporalHome() {
    try {
        const th = await api.getTemporalHome();
        if (th) {
            document.getElementById('th-alias').value = th.alias || '';
            document.getElementById('th-country').value = th.country || '';
            document.getElementById('th-state').value = th.state || '';
            document.getElementById('th-city').value = th.city || '';
            document.getElementById('th-zip').value = th.zip || '';
            document.getElementById('th-neighborhood').value = th.neighborhood || '';
        }
    } catch(e) { console.log('No temporal home profile yet'); }
}
async function loadShelter() {
    try {
        const res = await fetch('/api/users/shelter');
        if (res.ok) {
            const shelter = await res.json();
            document.getElementById('shelter-name').value = shelter.name || '';
            document.getElementById('shelter-country').value = shelter.country || '';
            document.getElementById('shelter-state').value = shelter.state || '';
            document.getElementById('shelter-city').value = shelter.city || '';
            document.getElementById('shelter-address').value = shelter.address || '';
            document.getElementById('shelter-zip').value = shelter.zip || '';
            document.getElementById('shelter-phone').value = shelter.phone || '';
            document.getElementById('shelter-email').value = shelter.email || '';
            document.getElementById('shelter-website').value = shelter.website || '';
            document.getElementById('shelter-description').value = shelter.description || '';
        }
    } catch(e) { console.log('No shelter profile yet'); }
}
async function loadSterilization() {
    try {
        const res = await fetch('/api/users/sterilization-location');
        if (res.ok) {
            const loc = await res.json();
            document.getElementById('sterilization-name').value = loc.name || '';
            document.getElementById('sterilization-country').value = loc.country || '';
            document.getElementById('sterilization-state').value = loc.state || '';
            document.getElementById('sterilization-city').value = loc.city || '';
            document.getElementById('sterilization-address').value = loc.address || '';
            document.getElementById('sterilization-zip').value = loc.zip || '';
            document.getElementById('sterilization-phone').value = loc.phone || '';
            document.getElementById('sterilization-email').value = loc.email || '';
            document.getElementById('sterilization-website').value = loc.website || '';
            document.getElementById('sterilization-description').value = loc.description || '';
        }
    } catch(e) { console.log('No sterilization location profile yet'); }
}
const saveBtn = document.getElementById('save-profile-btn');
if (saveBtn) {
    saveBtn.onclick = async () => {
        const msg = document.getElementById('message');
        const displayName = document.getElementById('displayName').value;
        const language = document.getElementById('language').value;
        if (!displayName.trim()) {
            msg.className = 'message error';
            msg.textContent = 'Display name cannot be empty';
            return;
        }
        try {
            await api.updateProfile(displayName);
            await api.updateLanguage(language);
            i18n.setLang(language);
        } catch (err) { throw err; }
        
        const roleRescuer = document.getElementById('role-rescuer').checked;
        const rolePhotographer = document.getElementById('role-photographer').checked;
        const roleTemporalHome = document.getElementById('role-temporal-home').checked;
        const roleShelter = document.getElementById('role-shelter').checked;
        const roleSterilization = document.getElementById('role-sterilization').checked;
        
        try {
            if (roleRescuer !== currentRoles.includes('RESCUER')) {
                await api.activateRescuer(roleRescuer);
            }
            if (rolePhotographer !== currentRoles.includes('PHOTOGRAPHER')) {
                if (rolePhotographer) {
                    const phCountry = document.getElementById('photographerCountry').value.trim();
                    const phState = document.getElementById('photographerState').value.trim();
                    if (!phCountry || !phState) {
                        msg.className = 'message error';
                        msg.textContent = 'Please fill in country and state for photographer services';
                        return;
                    }
                    await api.activatePhotographer(true);
                    const phFee = parseFloat(document.getElementById('photographerFee').value) || 0;
                    const phCurrency = document.getElementById('photographerCurrency').value;
                    await api.updatePhotographerSettings(phFee, phCurrency, phCountry, phState);
                } else {
                    await api.activatePhotographer(false);
                }
            } else if (rolePhotographer && currentRoles.includes('PHOTOGRAPHER')) {
                const phCountry = document.getElementById('photographerCountry').value.trim();
                const phState = document.getElementById('photographerState').value.trim();
                const phFee = parseFloat(document.getElementById('photographerFee').value) || 0;
                const phCurrency = document.getElementById('photographerCurrency').value;
                if (phCountry && phState) {
                    await api.updatePhotographerSettings(phFee, phCurrency, phCountry, phState);
                }
            }
            if (roleTemporalHome !== currentRoles.includes('TEMPORAL_HOME')) {
                if (roleTemporalHome) {
                    const thAlias = document.getElementById('th-alias').value.trim();
                    const thCountry = document.getElementById('th-country').value.trim();
                    const thCity = document.getElementById('th-city').value.trim();
                    if (!thAlias || !thCountry || !thCity) {
                        msg.className = 'message error';
                        msg.textContent = 'Please fill in alias, country and city for temporal home';
                        return;
                    }
                    const thData = {
                        alias: thAlias,
                        country: thCountry,
                        state: document.getElementById('th-state').value.trim() || null,
                        city: thCity,
                        zip: document.getElementById('th-zip').value.trim() || null,
                        neighborhood: document.getElementById('th-neighborhood').value.trim() || null
                    };
                    if (hasTemporalHomeProfile) {
                        await api.updateTemporalHome(thData);
                    } else {
                        await api.createTemporalHome(thData);
                        hasTemporalHomeProfile = true;
                    }
                } else {
                    await api.activateTemporalHome(false);
                }
            } else if (roleTemporalHome && currentRoles.includes('TEMPORAL_HOME')) {
                const thAlias = document.getElementById('th-alias').value.trim();
                const thCountry = document.getElementById('th-country').value.trim();
                const thCity = document.getElementById('th-city').value.trim();
                if (thAlias && thCountry && thCity) {
                    const thData = {
                        alias: thAlias,
                        country: thCountry,
                        state: document.getElementById('th-state').value.trim() || null,
                        city: thCity,
                        zip: document.getElementById('th-zip').value.trim() || null,
                        neighborhood: document.getElementById('th-neighborhood').value.trim() || null
                    };
                    if (hasTemporalHomeProfile) {
                        await api.updateTemporalHome(thData);
                    } else {
                        await api.createTemporalHome(thData);
                        hasTemporalHomeProfile = true;
                    }
                }
            }
            if (roleShelter !== currentRoles.includes('SHELTER')) {
                if (roleShelter) {
                    const shelterName = document.getElementById('shelter-name').value.trim();
                    const shelterCountry = document.getElementById('shelter-country').value.trim();
                    const shelterCity = document.getElementById('shelter-city').value.trim();
                    const shelterAddress = document.getElementById('shelter-address').value.trim();
                    if (!shelterName || !shelterCountry || !shelterCity || !shelterAddress) {
                        msg.className = 'message error';
                        msg.textContent = 'Please fill in name, country, city and address for shelter';
                        return;
                    }
                    const shelterData = {
                        name: shelterName,
                        country: shelterCountry,
                        state: document.getElementById('shelter-state').value.trim() || null,
                        city: shelterCity,
                        address: shelterAddress,
                        zip: document.getElementById('shelter-zip').value.trim() || null,
                        phone: document.getElementById('shelter-phone').value.trim() || null,
                        email: document.getElementById('shelter-email').value.trim() || null,
                        website: document.getElementById('shelter-website').value.trim() || null,
                        description: document.getElementById('shelter-description').value.trim() || null
                    };
                    if (hasShelterProfile) {
                        const res = await fetch('/api/users/shelter', {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(shelterData)
                        });
                        if (!res.ok) throw new Error('Failed to update shelter');
                    } else {
                        const res = await fetch('/api/users/shelter', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(shelterData)
                        });
                        if (!res.ok) throw new Error('Failed to create shelter');
                        hasShelterProfile = true;
                    }
                    await fetch('/api/users/shelter-profile', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ activate: true })
                    });
                } else {
                    await fetch('/api/users/shelter-profile', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ activate: false })
                    });
                    if (hasShelterProfile) {
                        await fetch('/api/users/shelter', { method: 'DELETE' });
                        hasShelterProfile = false;
                    }
                }
            } else if (roleShelter && currentRoles.includes('SHELTER')) {
                const shelterName = document.getElementById('shelter-name').value.trim();
                const shelterCountry = document.getElementById('shelter-country').value.trim();
                const shelterCity = document.getElementById('shelter-city').value.trim();
                const shelterAddress = document.getElementById('shelter-address').value.trim();
                if (shelterName && shelterCountry && shelterCity && shelterAddress) {
                    const shelterData = {
                        name: shelterName,
                        country: shelterCountry,
                        state: document.getElementById('shelter-state').value.trim() || null,
                        city: shelterCity,
                        address: shelterAddress,
                        zip: document.getElementById('shelter-zip').value.trim() || null,
                        phone: document.getElementById('shelter-phone').value.trim() || null,
                        email: document.getElementById('shelter-email').value.trim() || null,
                        website: document.getElementById('shelter-website').value.trim() || null,
                        description: document.getElementById('shelter-description').value.trim() || null
                    };
                    await fetch('/api/users/shelter', {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(shelterData)
                    });
                }
            }
            if (roleSterilization !== currentRoles.includes('STERILIZATION_SERVICE')) {
                if (roleSterilization) {
                    const sterilizationName = document.getElementById('sterilization-name').value.trim();
                    const sterilizationCountry = document.getElementById('sterilization-country').value.trim();
                    const sterilizationCity = document.getElementById('sterilization-city').value.trim();
                    const sterilizationAddress = document.getElementById('sterilization-address').value.trim();
                    if (!sterilizationName || !sterilizationCountry || !sterilizationCity || !sterilizationAddress) {
                        msg.className = 'message error';
                        msg.textContent = 'Please fill in name, country, city and address for sterilization service';
                        return;
                    }
                    const sterilizationData = {
                        name: sterilizationName,
                        country: sterilizationCountry,
                        state: document.getElementById('sterilization-state').value.trim() || null,
                        city: sterilizationCity,
                        address: sterilizationAddress,
                        zip: document.getElementById('sterilization-zip').value.trim() || null,
                        phone: document.getElementById('sterilization-phone').value.trim() || null,
                        email: document.getElementById('sterilization-email').value.trim() || null,
                        website: document.getElementById('sterilization-website').value.trim() || null,
                        description: document.getElementById('sterilization-description').value.trim() || null
                    };
                    if (hasSterilizationProfile) {
                        const res = await fetch('/api/users/sterilization-location', {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(sterilizationData)
                        });
                        if (!res.ok) throw new Error('Failed to update sterilization location');
                    } else {
                        const res = await fetch('/api/users/sterilization-location', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(sterilizationData)
                        });
                        if (!res.ok) throw new Error('Failed to create sterilization location');
                        hasSterilizationProfile = true;
                    }
                    await fetch('/api/users/sterilization-profile', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ activate: true })
                    });
                } else {
                    await fetch('/api/users/sterilization-profile', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ activate: false })
                    });
                    if (hasSterilizationProfile) {
                        await fetch('/api/users/sterilization-location', { method: 'DELETE' });
                        hasSterilizationProfile = false;
                    }
                }
            } else if (roleSterilization && currentRoles.includes('STERILIZATION_SERVICE')) {
                const sterilizationName = document.getElementById('sterilization-name').value.trim();
                const sterilizationCountry = document.getElementById('sterilization-country').value.trim();
                const sterilizationCity = document.getElementById('sterilization-city').value.trim();
                const sterilizationAddress = document.getElementById('sterilization-address').value.trim();
                if (sterilizationName && sterilizationCountry && sterilizationCity && sterilizationAddress) {
                    const sterilizationData = {
                        name: sterilizationName,
                        country: sterilizationCountry,
                        state: document.getElementById('sterilization-state').value.trim() || null,
                        city: sterilizationCity,
                        address: sterilizationAddress,
                        zip: document.getElementById('sterilization-zip').value.trim() || null,
                        phone: document.getElementById('sterilization-phone').value.trim() || null,
                        email: document.getElementById('sterilization-email').value.trim() || null,
                        website: document.getElementById('sterilization-website').value.trim() || null,
                        description: document.getElementById('sterilization-description').value.trim() || null
                    };
                    await fetch('/api/users/sterilization-location', {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(sterilizationData)
                    });
                }
            }
            msg.className = 'message success';
            msg.textContent = 'Profile updated!';
            location.reload();
        } catch (err) {
            msg.className = 'message error';
            msg.textContent = err.message;
        }
    };
}
load();

async function loadPasswordStatus() {
    try {
        const res = await fetch('/api/users/has-password');
        const data = await res.json();
        const passwordStatus = document.getElementById('password-status');
        const currentPasswordRow = document.getElementById('current-password-row');
        
        if (data.hasPassword) {
            passwordStatus.textContent = i18n.t('passwordSet') || 'Password is set';
            currentPasswordRow.classList.remove('password-hidden');
            document.getElementById('current-password').disabled = false;
        } else {
            passwordStatus.textContent = i18n.t('noPassword') || 'No password set';
            currentPasswordRow.classList.add('password-hidden');
            document.getElementById('current-password').disabled = true;
        }
    } catch (e) {
        console.error('Error loading password status:', e);
    }
}

document.getElementById('save-password-btn')?.addEventListener('click', async () => {
    const currentPassword = document.getElementById('current-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;
    const msg = document.getElementById('password-message');
    const hasPassword = document.getElementById('current-password').disabled === false;
    
    if (!msg) return;
    
    const checks = [
        { valid: newPassword.length >= 8, key: 'password8Chars' },
        { valid: /[A-Z]/.test(newPassword), key: 'passwordUppercase' },
        { valid: /[a-z]/.test(newPassword), key: 'passwordLowercase' },
        { valid: /[0-9]/.test(newPassword), key: 'passwordNumber' },
        { valid: /[!@#$%^&*(),.?":{}|<>\-_+=/\[\]\\]/.test(newPassword), key: 'passwordSymbol' }
    ];
    
    const allValid = checks.every(c => c.valid);
    
    let html = '<div class="password-checks">';
    checks.forEach(check => {
        const label = i18n.t(check.key) || '';
        const color = check.valid ? '#28a745' : '#dc3545';
        html += '<div style="color:' + color + '; margin: 0.25rem 0;">' + (check.valid ? '✓' : '✗') + ' ' + label + '</div>';
    });
    html += '</div>';
    
    if (!allValid) {
        msg.className = 'message error';
        msg.innerHTML = html;
        return;
    }
    
    if (newPassword !== confirmPassword) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordsDoNotMatch') || 'Passwords do not match.';
        return;
    }
    
    if (hasPassword && !currentPassword) {
        msg.className = 'message error';
        msg.textContent = i18n.t('currentPasswordRequired') || 'Current password is required.';
        return;
    }
    
    msg.textContent = i18n.t('saving') || 'Saving...';
    msg.className = '';
    
    try {
        const publicKey = await getPublicKey();
        
        if (hasPassword) {
            const encryptedCurrent = await rsaCrypto.encrypt(currentPassword, publicKey);
            const encryptedNew = await rsaCrypto.encrypt(newPassword, publicKey);
            
            const res = await fetch('/api/users/password', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    encryptedCurrentPassword: encryptedCurrent,
                    encryptedNewPassword: encryptedNew
                })
            });
            
            const result = await res.json();
            if (result.success) {
                msg.className = 'message success';
                msg.textContent = i18n.t('passwordChangeSuccess') || 'Password changed successfully!';
                document.getElementById('current-password').value = '';
                document.getElementById('new-password').value = '';
                document.getElementById('confirm-password').value = '';
            } else {
                msg.className = 'message error';
                msg.textContent = result.error || i18n.t('passwordChangeFailed') || 'Failed to change password.';
            }
        } else {
            const encryptedPassword = await rsaCrypto.encrypt(newPassword, publicKey);
            
            const res = await fetch('/api/users/password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ encryptedPassword })
            });
            
            const result = await res.json();
            if (result.success) {
                msg.className = 'message success';
                msg.textContent = i18n.t('passwordSetSuccess') || 'Password set successfully!';
                document.getElementById('new-password').value = '';
                document.getElementById('confirm-password').value = '';
                loadPasswordStatus();
            } else {
                msg.className = 'message error';
                msg.textContent = result.error || i18n.t('passwordSetFailed') || 'Failed to set password.';
            }
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordSetFailed') || 'Failed to set password.';
    }
});

document.getElementById('change-email-btn')?.addEventListener('click', async () => {
    const newEmail = document.getElementById('new-email').value;
    const msg = document.getElementById('email-change-message');
    
    if (!msg) return;
    
    if (!newEmail) {
        msg.className = 'message error';
        msg.textContent = i18n.t('emailRequired') || 'Email is required';
        return;
    }
    
    const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
    if (!emailRegex.test(newEmail)) {
        msg.className = 'message error';
        msg.textContent = i18n.t('invalidEmail') || 'Invalid email format';
        return;
    }
    
    msg.textContent = i18n.t('sending') || 'Sending...';
    msg.className = '';
    
    try {
        const res = await fetch('/api/users/request-email-change', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newEmail })
        });
        
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = i18n.t('emailChangeSent') || 'Verification email sent to new address!';
            document.getElementById('new-email').value = '';
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || i18n.t('emailChangeFailed') || 'Failed to request email change.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('emailChangeFailed') || 'Failed to request email change.';
    }
});

loadPasswordStatus();

async function loadPasskeyStatus() {
    try {
        const res = await fetch('/api/auth/has-passkey');
        const data = await res.json();
        const passkeyStatus = document.getElementById('passkey-status');
        if (data.success) {
            passkeyStatus.textContent = i18n.t('passkeyRegistered') || 'You have a passkey registered.';
        } else {
            passkeyStatus.textContent = i18n.t('noPasskey') || 'No passkey registered yet.';
        }
    } catch (e) {
        console.error('Error loading passkey status:', e);
        const passkeyStatus = document.getElementById('passkey-status');
        passkeyStatus.textContent = i18n.t('noPasskey') || 'No passkey registered yet.';
    }
}

document.getElementById('register-passkey-btn')?.addEventListener('click', async () => {
    const msg = document.getElementById('passkey-message');
    const passkeyName = document.getElementById('passkey-name').value.trim();
    
    if (!passkeyName) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passkeyNameRequired') || 'Please enter a name for this passkey.';
        return;
    }
    
    msg.textContent = i18n.t('registeringPasskey') || 'Registering passkey...';
    msg.className = '';
    
    try {
        const user = window.cachedUserData || await api.me();
        if (!user) {
            msg.className = 'message error';
            msg.textContent = i18n.t('notAuthenticated') || 'Not authentication.';
            return;
        }
        
        const optsRes = await fetch('/api/auth/registration-options-for-user', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: user.email, displayName: user.displayName })
        });
        
        if (!optsRes.ok) {
            throw new Error('Failed to get registration options');
        }
        
        const options = await optsRes.json();
        
        const credential = await navigator.credentials.create({
            publicKey: webauthn.parseCreationOptions(options)
        });
        
        const regRes = await fetch('/api/auth/register-passkey', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                registrationResponse: JSON.stringify(credential),
                passkeyName: passkeyName
            })
        });
        
        const result = await regRes.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = i18n.t('passkeyRegisteredSuccess') || 'Passkey registered successfully!';
            document.getElementById('passkey-name').value = '';
            loadPasskeyStatus();
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || i18n.t('passkeyRegistrationFailed') || 'Failed to register passkey.';
        }
    } catch (e) {
        console.error('Passkey registration error:', e);
        msg.className = 'message error';
        msg.textContent = i18n.t('passkeyRegistrationFailed') || 'Failed to register passkey.';
    }
});

loadPasskeyStatus();

const logoutLink = document.getElementById('logout-link');
if (logoutLink) {
    logoutLink.onclick = async (e) => { e.preventDefault(); await api.logout(); window.location.href = '/'; };
}