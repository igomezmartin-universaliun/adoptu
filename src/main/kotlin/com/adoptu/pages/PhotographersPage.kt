package com.adoptu.pages

import kotlinx.html.*

private const val SEARCH_FILTER = "search-filter"

fun HTML.photographersPage() {
    commonHead("Photographers - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "photographers"; +"Photographers" }
            p { attributes["data-i18n"] = "photographerDescription"; +"Professional photographers offering pet photo sessions" }
            div(classes = "temporal-search-form") {
                div(classes = "search-country") {
                    label { htmlFor = "search-country"; attributes["data-i18n"] = "countryLabel"; +"Country" }
                    select { id = "search-country"; onChange = "onCountryChange()"; countrySelect("search-country", false) }
                }
                div(classes = "search-filters") {
                    div(classes = SEARCH_FILTER) {
                        label { htmlFor = "search-state"; attributes["data-i18n"] = "state"; +"State" }
                        input(InputType.text) { id = "search-state"; disabled = true }
                    }
                }
                button(classes = "btn", type = ButtonType.button) { attributes["data-i18n"] = "search"; onClick = "searchPhotographers()"; +"Search" }
            }
            div { id = "photographers"; classes = setOf("photographer-grid"); +"" }
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

function searchPhotographers() {
    const country = document.getElementById('search-country').value;
    const state = document.getElementById('search-state').value.trim();
    
    if (!country) {
        document.getElementById('photographers').innerHTML = '<p>Please select a country.</p>';
        return;
    }
    
    const params = new URLSearchParams();
    params.append('country', country);
    if (state) params.append('state', state);
    
    loadPhotographers('/api/users/photographers?' + params.toString());
}

async function loadPhotographers(url) {
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load photographers');
        const photographers = await response.json();
        const container = document.getElementById('photographers');
        if (!photographers || photographers.length === 0) {
            container.innerHTML = '<p>'+t('noPhotographersAvailable')+'</p>';
            return;
        }
        container.innerHTML = photographers.map(p => {
            const fee = p.photographerFee ? p.photographerFee + ' ' + (p.photographerCurrency || 'USD') : 'Free';
            const location = [p.photographerState, p.photographerCountry].filter(Boolean).join(', ');
            return '<div class="photographer-card">' +
                '<div class="photographer-info">' +
                '<h3>'+p.displayName+'</h3>' +
                (location ? '<p class="photographer-location">'+location+'</p>' : '') +
                '<p class="photographer-fee">'+t('sessionFee')+': <strong>'+fee+'</strong></p>' +
                '</div>' +
                '<button class="btn request-btn" data-id="'+p.userId+'" data-name="'+p.displayName+'" data-fee="'+fee+'">'+t('requestPhotoSession')+'</button>' +
                '</div>';
        }).join('');
        
        document.querySelectorAll('.request-btn').forEach(btn => {
            btn.onclick = async () => {
                const photographerId = parseInt(btn.dataset.id);
                const photographerName = btn.dataset.name;
                const msg = t('enterMessage').replace('{name}', photographerName);
                const message = prompt(msg);
                if (message === null) return;
                try {
                    await api.createPhotographyRequest(photographerId, null, message || '');
                    alert(t('requestSentSuccessfully'));
                } catch (err) {
                    alert('Error: ' + err.message);
                }
            };
        });
    } catch (err) {
        document.getElementById('photographers').innerHTML = '<p>'+t('errorLoadingPhotographers')+'</p>';
    }
}
""".trimIndent()) } }
    }
}
