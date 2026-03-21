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
            h1 { attributes["data-i18n"] = "profile"; +"Profile" }
            div { id = "message"; +"" }
            
            div(classes = "profile-section") {
                h2 { attributes["data-i18n"] = "accountSettings"; +"Account Settings" }
                form { id = "profile-form"
                    label { htmlFor = "username"; attributes["data-i18n"] = "username"; +"Username" }
                    input(InputType.text) { id = "username"; disabled = true }
                    
                    label { htmlFor = "displayName"; attributes["data-i18n"] = "displayName"; +"Display Name" }
                    input(InputType.text) { id = "displayName"; required = true }
                    
                    label { htmlFor = "email"; attributes["data-i18n"] = "email"; +"Email" }
                    input(InputType.email) { id = "email"; disabled = true }

                    label { htmlFor = "language"; attributes["data-i18n"] = "language"; +"Language" }
                    select { id = "language"; name = "language"
                        option { value = "en"; +"English 🇺🇸" }
                        option { value = "es"; +"Español 🇪🇸" }
                        option { value = "fr"; +"Français 🇫🇷" }
                        option { value = "pt"; +"Português 🇧🇷" }
                        option { value = "zh"; +"中文 🇨🇳" }
                    }
                    
                    label { attributes["data-i18n"] = "yourRoles"; +"Your Roles:" }
                    div(classes = "roles-section") {
                        div(classes = "checkbox-group") {
                            label {
                                attributes["data-i18n"] = "adopterRequired"
                                input(InputType.checkBox) { id = "role-adopter"; checked = true; disabled = true }
                                +"Adopter (required)"
                            }
                            label {
                                attributes["data-i18n"] = "publishPets"
                                input(InputType.checkBox) { id = "role-rescuer" }
                                +" Rescuer - Publish pets for adoption"
                            }
                            label {
                                attributes["data-i18n"] = "offerPhotography"
                                input(InputType.checkBox) { id = "role-photographer" }
                                +" Photographer - Offer photography services"
                            }
                            label {
                                attributes["data-i18n"] = "provideTemporaryHome"
                                input(InputType.checkBox) { id = "role-temporal-home" }
                                +" Temporal Home - Provide temporary home for pets"
                            }
                        }
                    }
                    div(classes = "form-actions") {
                        button(classes = "btn", type = ButtonType.button) { id = "save-profile-btn"; attributes["data-i18n"] = "save"; +"Save" }
                    }
                }
            }

            div(classes = "profile-section photographer-section") { style = "display: none;"
                h2 { attributes["data-i18n"] = "photographerSettings"; +"Photographer Settings" }
                form { id = "photographer-form"
                    label { htmlFor = "photographerFee"; attributes["data-i18n"] = "sessionFee"; +"Session Fee" }
                    div(classes = "fee-input-group") { style = "display: flex; gap: 0.5rem; align-items: flex-start;"

                        div(classes = "fee-inputs") { style = "flex: 1; display: flex; gap: 0.5rem;"
                            input(InputType.number) { id = "photographerFee"; step = "0.01"; value = "0"; style = "flex: 3;"; this.min = "0" }
                            select { id = "photographerCurrency"; style = "flex: 1;"
                                option { value = "USD"; +"$ USD" }
                                option { value = "EUR"; +"€ EUR" }
                                option { value = "GBP"; +"£ GBP" }
                                option { value = "CAD"; +"$ CAD" }
                                option { value = "AUD"; +"$ AUD" }
                            }
                        }
                        button(classes = "btn", type = ButtonType.button) { id = "save-photographer-btn"; attributes["data-i18n"] = "updateFee"; style = "flex: 0 0 auto;"; +"Update Fee" }
                    }
                }
            }

            div(classes = "profile-section temporal-home-section") { style = "display: none;"
                h2 { attributes["data-i18n"] = "temporalHomeSettings"; +"Temporal Home Settings" }
                form { id = "temporal-home-form"
                    label { htmlFor = "th-alias"; attributes["data-i18n"] = "alias"; +"Alias" }
                    input(InputType.text) { id = "th-alias" }
                    label { htmlFor = "th-country"; attributes["data-i18n"] = "country"; +"Country" }
                    input(InputType.text) { id = "th-country" }
                    label { htmlFor = "th-state"; attributes["data-i18n"] = "state"; +"State" }
                    input(InputType.text) { id = "th-state" }
                    label { htmlFor = "th-city"; attributes["data-i18n"] = "city"; +"City" }
                    input(InputType.text) { id = "th-city" }
                    label { htmlFor = "th-zip"; attributes["data-i18n"] = "zipCode"; +"Zip Code" }
                    input(InputType.text) { id = "th-zip" }
                    label { htmlFor = "th-neighborhood"; attributes["data-i18n"] = "neighborhood"; +"Neighborhood" }
                    input(InputType.text) { id = "th-neighborhood" }
                }
                div(classes = "form-actions") {
                    button(classes = "btn", type = ButtonType.button) {
                        id = "save-temporal-home-btn"; attributes["data-i18n"] = "save"; +"Save"
                    }
                }
            }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
let currentRoles = [];
async function load() {
    let user = window.cachedUserData;
    if (!user) {
        try {
            user = await api.me();
            window.cachedUserData = user;
        } catch (e) {
            console.error('Error loading profile:', e);
            return;
        }
    }
    console.log('User data:', user);
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
    
    if (rescuer) rescuer.checked = currentRoles.includes('RESCUER');
    if (photographer) photographer.checked = currentRoles.includes('PHOTOGRAPHER');
    if (temporalHome) temporalHome.checked = currentRoles.includes('TEMPORAL_HOME');
    
    const primaryRole = currentRoles[0] || 'ADOPTER';
    
    if (primaryRole === 'PHOTOGRAPHER' || primaryRole === 'ADMIN' || currentRoles.includes('PHOTOGRAPHER')) {
        document.querySelector('.photographer-section').style.display = 'block';
        if (user.photographerFee !== undefined) {
            document.getElementById('photographerFee').value = user.photographerFee || 0;
            document.getElementById('photographerCurrency').value = user.photographerCurrency || 'USD';
        }
    }
    
    if (primaryRole === 'TEMPORAL_HOME' || primaryRole === 'ADMIN' || currentRoles.includes('TEMPORAL_HOME')) {
        document.querySelector('.temporal-home-section').style.display = 'block';
        loadTemporalHome();
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
document.getElementById('save-profile-btn').onclick = async () => {
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
            await api.activatePhotographer(rolePhotographer);
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
                await api.createTemporalHome({
                    alias: thAlias,
                    country: thCountry,
                    state: document.getElementById('th-state').value.trim() || null,
                    city: thCity,
                    zip: document.getElementById('th-zip').value.trim() || null,
                    neighborhood: document.getElementById('th-neighborhood').value.trim() || null
                });
            } else {
                await api.activateTemporalHome(false);
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
document.getElementById('save-photographer-btn').onclick = async () => {
    const msg = document.getElementById('message');
    const fee = parseFloat(document.getElementById('photographerFee').value) || 0;
    const currency = document.getElementById('photographerCurrency').value;
    if (fee < 0) {
        msg.className = 'message error';
        msg.textContent = 'Fee must be zero or positive';
        return;
    }
    try {
        await api.updatePhotographerSettings(fee, currency);
        msg.className = 'message success';
        msg.textContent = 'Photographer settings updated!';
    } catch (err) {
        msg.className = 'message error';
        msg.textContent = err.message;
    }
};
document.getElementById('save-temporal-home-btn').onclick = async () => {
    const msg = document.getElementById('message');
    try {
        const th = await api.createTemporalHome({
            alias: document.getElementById('th-alias').value.trim(),
            country: document.getElementById('th-country').value.trim(),
            state: document.getElementById('th-state').value.trim() || null,
            city: document.getElementById('th-city').value.trim(),
            zip: document.getElementById('th-zip').value.trim() || null,
            neighborhood: document.getElementById('th-neighborhood').value.trim() || null
        });
        msg.className = 'message success';
        msg.textContent = 'Temporal home settings saved!';
    } catch (err) {
        msg.className = 'message error';
        msg.textContent = err.message;
    }
};
load();
document.getElementById('logout-link').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
""".trimIndent()) } }
    }
}
