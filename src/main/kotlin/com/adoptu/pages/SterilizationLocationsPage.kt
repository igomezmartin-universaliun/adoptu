package com.adoptu.pages

import kotlinx.html.*

fun HTML.sterilizationLocationsPage() {
    commonHead("Sterilization Locations - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "sterilizationLocations"; +"Sterilization Locations" }
            p { attributes["data-i18n"] = "sterilizationLocationsDescription"; +"Find places where you can take animals to be sterilized." }
            
            div(classes = "filter-buttons") {
                div(classes = "search-country") {
                    label { attributes["data-i18n"] = "selectCountry"; +"Select a country" }
                    select(classes = "filter-type") { id = "country-select"; name = "country" }
                }
                div(classes = "search-filter") {
                    label { attributes["data-i18n"] = "state"; +"State" }
                    select(classes = "filter-type") { id = "state-select"; name = "state" }
                }
                div(classes = "search-filter") {
                    label { attributes["data-i18n"] = "city"; +"City" }
                    select(classes = "filter-type") { id = "city-select"; name = "city" }
                }
            }

            div { id = "locations-container"; +"" }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
async function loadCountries() {
    const res = await fetch('/api/sterilization-locations/countries');
    const data = await res.json();
    const select = document.getElementById('country-select');
    select.innerHTML = '<option value="">' + t('selectCountry') + '</option>' + 
        data.countries.map(c => '<option value="'+c+'">'+c+'</option>').join('');
}

async function loadStates() {
    const country = document.getElementById('country-select').value;
    if (!country) {
        document.getElementById('state-select').innerHTML = '<option value="">' + t('state') + '</option>';
        document.getElementById('city-select').innerHTML = '<option value="">' + t('city') + '</option>';
        loadLocations();
        return;
    }
    const res = await fetch('/api/sterilization-locations/countries/'+encodeURIComponent(country)+'/states');
    const data = await res.json();
    const select = document.getElementById('state-select');
    select.innerHTML = '<option value="">' + t('allStates') + '</option>' + 
        data.states.map(s => '<option value="'+s+'">'+s+'</option>').join('');
    document.getElementById('city-select').innerHTML = '<option value="">' + t('city') + '</option>';
    loadLocations();
}

async function loadCities() {
    const country = document.getElementById('country-select').value;
    const state = document.getElementById('state-select').value;
    if (!country) {
        document.getElementById('city-select').innerHTML = '<option value="">' + t('city') + '</option>';
        loadLocations();
        return;
    }
    const url = '/api/sterilization-locations/countries/'+encodeURIComponent(country)+'/states/'+encodeURIComponent(state||'')+'/cities';
    const res = await fetch(url);
    const data = await res.json();
    const select = document.getElementById('city-select');
    select.innerHTML = '<option value="">' + t('allCities') + '</option>' + 
        data.cities.map(c => '<option value="'+c+'">'+c+'</option>').join('');
    loadLocations();
}

async function loadLocations() {
    const country = document.getElementById('country-select').value;
    const state = document.getElementById('state-select').value;
    const city = document.getElementById('city-select').value;
    let url = '/api/sterilization-locations?';
    if (country) url += 'country='+encodeURIComponent(country)+'&';
    if (state) url += 'state='+encodeURIComponent(state)+'&';
    if (city) url += 'city='+encodeURIComponent(city)+'&';
    const res = await fetch(url);
    const locations = await res.json();
    const container = document.getElementById('locations-container');
    if (locations.length === 0) {
        container.innerHTML = '<p>'+t('noLocationsFound')+'</p>';
        return;
    }
    container.innerHTML = '<div class="location-list">' + locations.map(loc => '
        <div class="location-card card-bg">
            <h3>'+loc.name+'</h3>
            <p class="location-address">'+loc.address+', '+loc.city+''+(loc.state ? ', '+loc.state : '')+', '+loc.country+'</p>
            '+(loc.phone ? '<p class="location-phone"><strong>'+t('phone')+':</strong> '+loc.phone+'</p>' : '')+'
            '+(loc.email ? '<p class="location-email"><strong>'+t('email')+':</strong> '+loc.email+'</p>' : '')+'
            '+(loc.website ? '<p class="location-website"><a href="'+loc.website+'" target="_blank">'+t('website')+'</a></p>' : '')+'
            '+(loc.description ? '<p class="location-description">'+loc.description+'</p>' : '')+'
        </div>
    ').join('') + '</div>';
}

document.getElementById('country-select').onchange = loadStates;
document.getElementById('state-select').onchange = loadCities;
document.getElementById('city-select').onchange = loadLocations;

loadCountries();
""".trimIndent()) } }
    }
}

fun HTML.adminSterilizationLocationsPage() {
    commonHead("Manage Sterilization Locations - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            div {
                h1 { attributes["data-i18n"] = "adminSterilizationLocations"; +"Manage Sterilization Locations" }
                a("/sterilization-locations") { attributes["data-i18n"] = "viewPublic"; +"View Public Page" }
            }
            div { id = "message"; +"" }
            
            button(classes = "btn") {
                id = "add-btn"
                attributes["data-i18n"] = "addLocation"
                +"Add Location"
                onClick = "showForm()"
            }
            
            div(classes = "form-modal") {
                id = "form-modal"
                style = "display: none;"
                div(classes = "form-modal-content card-bg") {
                    h2 { attributes["data-i18n"] = "addEditLocation"; +"Add/Edit Location" }
                    form(classes = "auth-form") {
                        id = "location-form"
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "name"; +"Name" }
                            input(type = InputType.text) { name = "name"; required = true }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "country"; +"Country" }
                            select { name = "country"; required = true; id = "form-country" }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "state"; +"State" }
                            input(type = InputType.text) { name = "state" }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "city"; +"City" }
                            input(type = InputType.text) { name = "city"; required = true }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "address"; +"Address" }
                            input(type = InputType.text) { name = "address"; required = true }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "zip"; +"ZIP" }
                            input(type = InputType.text) { name = "zip" }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "phone"; +"Phone" }
                            input(type = InputType.tel) { name = "phone" }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "email"; +"Email" }
                            input(type = InputType.email) { name = "email" }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "website"; +"Website" }
                            input(type = InputType.url) { name = "website" }
                        }
                        div(classes = "form-row") {
                            label { attributes["data-i18n"] = "description"; +"Description" }
                            textArea { name = "description"; rows = "4" }
                        }
                        div(classes = "form-actions") {
                            button(type = ButtonType.submit) {
                                classes = setOf("btn")
                                attributes["data-i18n"] = "save"
                                +"Save"
                            }
                            button(type = ButtonType.button) {
                                classes = setOf("btn", "btn-secondary")
                                attributes["data-i18n"] = "cancel"
                                +"Cancel"
                                onClick = "hideForm()"
                            }
                        }
                    }
                }
            }
            
            div { id = "locations-container"; +"" }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
let editingId = null;

async function loadCountries() {
    const res = await fetch('/api/sterilization-locations/countries');
    const data = await res.json();
    const select = document.getElementById('form-country');
    select.innerHTML = '<option value="">' + t('selectCountry') + '</option>' + 
        data.countries.map(c => '<option value="'+c+'">'+c+'</option>').join('');
}

async function loadLocations() {
    const res = await fetch('/api/admin/sterilization-locations');
    const locations = await res.json();
    const container = document.getElementById('locations-container');
    if (locations.length === 0) {
        container.innerHTML = '<p>'+t('noLocationsFound')+'</p>';
        return;
    }
    container.innerHTML = '<div class="location-list">' + locations.map(loc => '
        <div class="location-card card-bg">
            <h3>'+loc.name+'</h3>
            <p class="location-address">'+loc.address+', '+loc.city+''+(loc.state ? ', '+loc.state : '')+', '+loc.country+'</p>
            '+(loc.phone ? '<p class="location-phone"><strong>'+t('phone')+':</strong> '+loc.phone+'</p>' : '')+'
            '+(loc.email ? '<p class="location-email"><strong>'+t('email')+':</strong> '+loc.email+'</p>' : '')+'
            '+(loc.website ? '<p class="location-website"><a href="'+loc.website+'" target="_blank">'+t('website')+'</a></p>' : '')+'
            '+(loc.description ? '<p class="location-description">'+loc.description+'</p>' : '')+'
            <div class="pet-card-actions">
                <button class="btn" onclick="editLocation('+loc.id+')">'+t('edit')+'</button>
                <button class="btn btn-danger" onclick="deleteLocation('+loc.id+')">'+t('delete')+'</button>
            </div>
        </div>
    ').join('') + '</div>';
}

function showForm() {
    editingId = null;
    document.getElementById('location-form').reset();
    document.getElementById('form-modal').style.display = 'flex';
    loadCountries();
}

function hideForm() {
    document.getElementById('form-modal').style.display = 'none';
    editingId = null;
}

async function editLocation(id) {
    editingId = id;
    const res = await fetch('/api/sterilization-locations/'+id);
    const loc = await res.json();
    await loadCountries();
    document.getElementById('location-form').name.value = loc.name;
    document.getElementById('form-country').value = loc.country;
    document.getElementById('location-form').state.value = loc.state || '';
    document.getElementById('location-form').city.value = loc.city;
    document.getElementById('location-form').address.value = loc.address;
    document.getElementById('location-form').zip.value = loc.zip || '';
    document.getElementById('location-form').phone.value = loc.phone || '';
    document.getElementById('location-form').email.value = loc.email || '';
    document.getElementById('location-form').website.value = loc.website || '';
    document.getElementById('location-form').description.value = loc.description || '';
    document.getElementById('form-modal').style.display = 'flex';
}

async function deleteLocation(id) {
    if (!confirm(t('confirmDelete'))) return;
    await fetch('/api/admin/sterilization-locations/'+id, { method: 'DELETE' });
    loadLocations();
}

document.getElementById('location-form').onsubmit = async (e) => {
    e.preventDefault();
    const form = e.target;
    const data = {
        name: form.name.value,
        country: form.country.value,
        state: form.state.value || null,
        city: form.city.value,
        address: form.address.value,
        zip: form.zip.value || null,
        phone: form.phone.value || null,
        email: form.email.value || null,
        website: form.website.value || null,
        description: form.description.value || null
    };
    const method = editingId ? 'PUT' : 'POST';
    const url = editingId ? '/api/admin/sterilization-locations/'+editingId : '/api/admin/sterilization-locations';
    const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (res.ok) {
        hideForm();
        loadLocations();
    } else {
        document.getElementById('message').innerHTML = '<div class="message error">'+t('errorSaving')+'</div>';
    }
};

loadCountries();
loadLocations();
""".trimIndent()) } }
    }
}
