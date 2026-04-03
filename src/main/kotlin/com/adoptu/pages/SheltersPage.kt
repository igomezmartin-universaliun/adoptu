package com.adoptu.pages

import kotlinx.html.*

fun HTML.sheltersPage() {
    commonHead("Animal Shelters - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "animalShelters"; +"Animal Shelters" }
            p { attributes["data-i18n"] = "sheltersDescription"; +"Find animal shelters where you can make donations" }
            div(classes = "temporal-search-form") {
                div(classes = "search-country") {
                    label { htmlFor = "search-country"; attributes["data-i18n"] = "countryLabel"; +"Country" }
                    select { id = "search-country"; name = "country"; onChange = "onCountryChange()"; countrySelect("search-country", false) }
                }
                div(classes = "search-filters") {
                    div(classes = "search-filter") {
                        label { htmlFor = "search-state"; attributes["data-i18n"] = "state"; +"State" }
                        input(InputType.text) { id = "search-state"; disabled = true }
                    }
                }
                button(classes = "btn", type = ButtonType.button) { id = "search-btn"; attributes["data-i18n"] = "searchShelters"; onClick = "searchShelters()"; +"Search" }
            }
            div { id = "shelters-error"; classes = setOf("error-message"); style = "display:none" }
            div { id = "shelters"; classes = setOf("shelter-grid"); +"" }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
function onCountryChange() {
    const hasCountry = document.getElementById('search-country').value !== '';
    const stateInput = document.getElementById('search-state');
    stateInput.disabled = !hasCountry;
    if (!hasCountry) stateInput.value = '';
}

async function searchShelters() {
    const country = document.getElementById('search-country').value;
    const state = document.getElementById('search-state').value.trim();
    const errorDiv = document.getElementById('shelters-error');
    const container = document.getElementById('shelters');
    
    errorDiv.style.display = 'none';
    errorDiv.textContent = '';
    container.innerHTML = '<p>' + t('loading') + '</p>';
    
    if (!country) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = t('countryRequired');
        container.innerHTML = '';
        return;
    }
    
    const params = new URLSearchParams();
    params.append('country', country);
    if (state) params.append('state', state);
    
    try {
        const output = await fetch('/api/shelters?' + params.toString());
        if (!output.ok) throw new Error('Failed to load shelters');
        const shelters = await output.json();
        
        if (!shelters || shelters.length === 0) {
            container.innerHTML = '<p>' + t('noSheltersFound') + '</p>';
            return;
        }
        
        container.innerHTML = shelters.map(shelter => {
            const location = [shelter.city, shelter.state, shelter.country].filter(Boolean).join(', ');
            const hasDonationInfo = shelter.bankName || shelter.accountNumber || shelter.iban;
            
            return '<div class="shelter-card">' +
                '<h3>' + shelter.name + '</h3>' +
                (location ? '<p class="shelter-location">' + location + '</p>' : '') +
                '<p class="shelter-address">' + shelter.address + (shelter.zip ? ', ' + shelter.zip : '') + '</p>' +
                (shelter.phone ? '<p class="shelter-phone"><strong>' + t('phone') + ':</strong> ' + shelter.phone + '</p>' : '') +
                (shelter.email ? '<p class="shelter-email"><strong>' + t('email') + ':</strong> <a href="mailto:' + shelter.email + '">' + shelter.email + '</a></p>' : '') +
                (shelter.website ? '<p class="shelter-website"><a href="' + shelter.website + '" target="_blank">' + t('visitWebsite') + '</a></p>' : '') +
                (shelter.fiscalId ? '<p class="shelter-fiscal-id"><strong>' + t('fiscalId') + ':</strong> ' + shelter.fiscalId + '</p>' : '') +
                (hasDonationInfo ? '<div class="shelter-donation-info"><h4>' + t('donationInformation') + '</h4>' +
                    (shelter.bankName ? '<p><strong>' + t('bankName') + ':</strong> ' + shelter.bankName + '</p>' : '') +
                    (shelter.accountHolderName ? '<p><strong>' + t('accountHolder') + ':</strong> ' + shelter.accountHolderName + '</p>' : '') +
                    (shelter.accountNumber ? '<p><strong>' + t('accountNumber') + ':</strong> ' + shelter.accountNumber + '</p>' : '') +
                    (shelter.iban ? '<p><strong>IBAN:</strong> ' + shelter.iban + '</p>' : '') +
                    (shelter.swiftBic ? '<p><strong>SWIFT/BIC:</strong> ' + shelter.swiftBic + '</p>' : '') +
                    (shelter.currency ? '<p><strong>' + t('currency') + ':</strong> ' + shelter.currency + '</p>' : '') +
                    '</div>' : '') +
                (shelter.description ? '<p class="shelter-description">' + shelter.description + '</p>' : '') +
                '</div>';
        }).join('');
    } catch (err) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = t('errorLoadingShelters');
        container.innerHTML = '';
    }
}

// Initial state setup
document.addEventListener('DOMContentLoaded', () => {
    const stateInput = document.getElementById('search-state');
    stateInput.disabled = true;
});
""".trimIndent()) } }
    }
}
