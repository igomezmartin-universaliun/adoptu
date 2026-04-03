package com.adoptu.pages

import kotlinx.html.*

fun HTML.profilePage() {
    commonHead("Profile - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            div(classes = "profile-header card-bg sticky-section") {
                h1 { attributes["data-i18n"] = "profile"; +"Profile" }
                button(classes = "btn", type = ButtonType.button) { id = "save-profile-btn"; attributes["data-i18n"] = "save"; +"Save" }
            }
            div { id = "message"; +"" }
            
            div(classes = "card-bg profile-section") {
                h2 { attributes["data-i18n"] = "accountSettings"; +"Account Settings" }
                div(classes = "form-row") {
                    label { htmlFor = "email"; attributes["data-i18n"] = "email"; +"Email" }
                    input(InputType.email) { id = "email"; disabled = true }
                }
                div(classes = "form-row") {
                    label { htmlFor = "displayName"; attributes["data-i18n"] = "displayName"; +"Display Name" }
                    input(InputType.text) { id = "displayName"; required = true }
                }
                div(classes = "form-row") {
                    label { htmlFor = "language"; attributes["data-i18n"] = "language"; +"Language" }
                    select { id = "language"; name = "language"
                        option { value = "en"; +"English 🇺🇸" }
                        option { value = "es"; +"Español 🇪🇸" }
                        option { value = "fr"; +"Français 🇫🇷" }
                        option { value = "pt"; +"Português 🇧🇷" }
                        option { value = "zh"; +"中文 🇨🇳" }
                    }
                }
                div(classes = "form-row") {
                    label { attributes["data-i18n"] = "yourRoles"; +"Your Roles:" }
                    div(classes = "roles-section") {
                        div(classes = "checkbox-group") {
                            div {
                                style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                                input(InputType.checkBox) { id = "role-adopter"; checked = true; disabled = true }
                                span { attributes["data-i18n"] = "adopterRequired"; +"Adopter (required)" }
                            }
                            div {
                                style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                                input(InputType.checkBox) { id = "role-rescuer" }
                                span { attributes["data-i18n"] = "publishPets"; +"Rescuer - Publish pets for adoption" }
                            }
                            div {
                                style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                                input(InputType.checkBox) { id = "role-photographer" }
                                span { attributes["data-i18n"] = "offerPhotography"; +"Photographer - Offer photography services" }
                            }
                            div {
                                style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                                input(InputType.checkBox) { id = "role-temporal-home" }
                                span { attributes["data-i18n"] = "provideTemporaryHome"; +"Temporal Home - Provide temporary home for pets" }
                            }
                        }
                    }
                }
            }

            div(classes = "card-bg profile-section photographer-section") { style = "display: none;"
                h2 { attributes["data-i18n"] = "photographerSettings"; +"Photographer Settings" }
                div(classes = "form-row") {
                    label { htmlFor = "photographerCountry"; attributes["data-i18n"] = "countryLabel"; +"Country" }
                    select { id = "photographerCountry"; countrySelect("photographerCountry", false) }
                }
                div(classes = "form-row") {
                    label { htmlFor = "photographerState"; attributes["data-i18n"] = "state"; +"State" }
                    input(InputType.text) { id = "photographerState" }
                }
                div(classes = "form-row") {
                    label { htmlFor = "photographerFee"; attributes["data-i18n"] = "sessionFee"; +"Session Fee" }
                    div(classes = "fee-input-group") {
                        input(InputType.number) { id = "photographerFee"; step = "0.01"; value = "0"; this.min = "0" }
                        select { id = "photographerCurrency"
                            option { value = "USD"; +"$ USD" }
                            option { value = "EUR"; +"€ EUR" }
                            option { value = "GBP"; +"£ GBP" }
                            option { value = "CAD"; +"$ CAD" }
                            option { value = "AUD"; +"$ AUD" }
                        }
                    }
                }
            }

            div(classes = "card-bg profile-section temporal-home-section") { style = "display: none;"
                h2 { attributes["data-i18n"] = "temporalHomeSettings"; +"Temporal Home Settings" }
                div(classes = "form-row") {
                    label { htmlFor = "th-alias"; attributes["data-i18n"] = "alias"; +"Alias" }
                    input(InputType.text) { id = "th-alias" }
                }
                div(classes = "form-row") {
                    label { htmlFor = "th-country"; attributes["data-i18n"] = "countryLabel"; +"Country" }
                    select { id = "th-country"; countrySelect("th-country", false) }
                }
                div(classes = "form-row-two-col") {
                    div {
                        label { htmlFor = "th-state"; attributes["data-i18n"] = "state"; +"State" }
                        input(InputType.text) { id = "th-state" }
                    }
                    div {
                        label { htmlFor = "th-city"; attributes["data-i18n"] = "city"; +"City" }
                        input(InputType.text) { id = "th-city" }
                    }
                }
                div(classes = "form-row-two-col") {
                    div {
                        label { htmlFor = "th-zip"; attributes["data-i18n"] = "zipCode"; +"Zip Code" }
                        input(InputType.text) { id = "th-zip" }
                    }
                    div {
                        label { htmlFor = "th-neighborhood"; attributes["data-i18n"] = "neighborhood"; +"Neighborhood" }
                        input(InputType.text) { id = "th-neighborhood" }
                    }
                }
            }

            div(classes = "card-bg profile-section") {
                h2 { attributes["data-i18n"] = "securitySettings"; +"Security Settings" }
                div(classes = "form-row") {
                    id = "password-section"
                    div {
                        p { id = "password-status"; +"" }
                        button(classes = "btn btn-secondary", type = ButtonType.button) { id = "set-password-btn"; attributes["data-i18n"] = "setPassword"; +"Set Password" }
                    }
                }
                div(classes = "form-row") {
                    id = "change-password-section"
                    button(classes = "btn btn-secondary", type = ButtonType.button) { id = "change-password-btn"; attributes["data-i18n"] = "changePassword"; +"Change Password" }
                }
            }

            div(classes = "card-bg profile-section") {
                h2 { attributes["data-i18n"] = "emailChange"; +"Change Email" }
                p { attributes["data-i18n"] = "emailChangeDesc"; +"Update your email address. A verification link will be sent to the new email." }
                div(classes = "form-row") {
                    label { htmlFor = "new-email"; attributes["data-i18n"] = "newEmail"; +"New Email" }
                    input(InputType.email) { id = "new-email"; required = true }
                }
                p { id = "email-change-message"; +"" }
                button(classes = "btn btn-secondary", type = ButtonType.button) { id = "change-email-btn"; attributes["data-i18n"] = "requestEmailChange"; +"Request Email Change" }
            }
        }
        footer()
        commonScripts()
        script(src = "/static/js/crypto.js") {}
        script { unsafe { raw("""
let currentRoles = [];
let hasTemporalHomeProfile = false;
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
    console.log('Checkboxes found:', { rescuer, photographer, temporalHome });
    
    try {
        await api.getTemporalHome();
        hasTemporalHomeProfile = true;
    } catch(e) { hasTemporalHomeProfile = false; }
    
    if (rescuer) rescuer.checked = currentRoles.includes('RESCUER');
    if (photographer) photographer.checked = currentRoles.includes('PHOTOGRAPHER');
    if (temporalHome) temporalHome.checked = currentRoles.includes('TEMPORAL_HOME');
    
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
    
    const primaryRole = currentRoles[0] || 'ADOPTER';
    
    if (primaryRole === 'PHOTOGRAPHER' || primaryRole === 'ADMIN' || currentRoles.includes('PHOTOGRAPHER')) {
        document.querySelector('.photographer-section').style.display = 'block';
        loadPhotographer();
    }
    
    if (primaryRole === 'TEMPORAL_HOME' || primaryRole === 'ADMIN' || currentRoles.includes('TEMPORAL_HOME')) {
        document.querySelector('.temporal-home-section').style.display = 'block';
        loadTemporalHome();
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
        const setPasswordBtn = document.getElementById('set-password-btn');
        const changePasswordBtn = document.getElementById('change-password-btn');
        
        if (data.hasPassword) {
            passwordStatus.textContent = i18n.t('passwordSet') || 'Password is set';
            setPasswordBtn.style.display = 'none';
            changePasswordBtn.style.display = 'block';
        } else {
            passwordStatus.textContent = i18n.t('noPassword') || 'No password set';
            setPasswordBtn.style.display = 'block';
            changePasswordBtn.style.display = 'none';
        }
    } catch (e) {
        console.error('Error loading password status:', e);
    }
}

document.getElementById('set-password-btn')?.addEventListener('click', async () => {
    const password = prompt(i18n.t('enterNewPassword') || 'Enter new password (min 8 characters):');
    if (!password || password.length < 8) {
        alert(i18n.t('passwordTooShort') || 'Password must be at least 8 characters.');
        return;
    }
    
    const msg = document.getElementById('message');
    msg.textContent = i18n.t('saving') || 'Saving...';
    msg.className = '';
    
    try {
        const publicKey = await getPublicKey();
        const encryptedPassword = await rsaCrypto.encrypt(password, publicKey);
        
        const res = await fetch('/api/users/password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ encryptedPassword })
        });
        
        const result = await res.json();
        if (result.success) {
            msg.className = 'message success';
            msg.textContent = i18n.t('passwordSetSuccess') || 'Password set successfully!';
            loadPasswordStatus();
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || i18n.t('passwordSetFailed') || 'Failed to set password.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordSetFailed') || 'Failed to set password.';
    }
});

document.getElementById('change-password-btn')?.addEventListener('click', async () => {
    const currentPassword = prompt(i18n.t('enterCurrentPassword') || 'Enter current password:');
    if (!currentPassword) return;
    
    const newPassword = prompt(i18n.t('enterNewPassword') || 'Enter new password (min 8 characters):');
    if (!newPassword || newPassword.length < 8) {
        alert(i18n.t('passwordTooShort') || 'Password must be at least 8 characters.');
        return;
    }
    
    const msg = document.getElementById('message');
    msg.textContent = i18n.t('saving') || 'Saving...';
    msg.className = '';
    
    try {
        const publicKey = await getPublicKey();
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
        } else {
            msg.className = 'message error';
            msg.textContent = result.error || i18n.t('passwordChangeFailed') || 'Failed to change password.';
        }
    } catch (e) {
        msg.className = 'message error';
        msg.textContent = i18n.t('passwordChangeFailed') || 'Failed to change password.';
    }
});

document.getElementById('change-email-btn')?.addEventListener('click', async () => {
    const newEmail = document.getElementById('new-email').value;
    const msg = document.getElementById('email-change-message');
    
    if (!newEmail) {
        msg.className = 'message error';
        msg.textContent = i18n.t('emailRequired') || 'Email is required';
        return;
    }
    
    const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
    if (!emailRegex.matches(newEmail)) {
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

const logoutLink = document.getElementById('logout-link');
if (logoutLink) {
    logoutLink.onclick = async (e) => { e.preventDefault(); await api.logout(); window.location.href = '/'; };
}
""".trimIndent()) } }
    }
}
