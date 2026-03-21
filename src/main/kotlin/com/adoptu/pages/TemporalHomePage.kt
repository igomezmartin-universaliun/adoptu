package com.adoptu.pages

import kotlinx.html.*

fun HTML.temporalHomeProfilePage() {
    commonHead("My Temporal Home - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "myTemporalHome"; +"My Temporal Home" }
            div { id = "message"; +"" }
            div(classes = "temporal-home-form") {
                h2 { attributes["data-i18n"] = "createTemporalHome"; +"Create Temporal Home Profile" }
                form { id = "temporal-home-form"
                    label { htmlFor = "alias"; attributes["data-i18n"] = "alias"; +"Alias" }; input(InputType.text) { name = "alias"; id = "alias"; required = true }
                    label { htmlFor = "country"; attributes["data-i18n"] = "countryLabel"; +"Country" }; input(InputType.text) { name = "country"; id = "country"; required = true }
                    label { htmlFor = "state"; attributes["data-i18n"] = "state"; +"State" }; input(InputType.text) { name = "state"; id = "state" }
                    label { htmlFor = "city"; attributes["data-i18n"] = "city"; +"City" }; input(InputType.text) { name = "city"; id = "city"; required = true }
                    label { htmlFor = "zip"; attributes["data-i18n"] = "zipCode"; +"Zip Code" }; input(InputType.text) { name = "zip"; id = "zip" }
                    label { htmlFor = "neighborhood"; attributes["data-i18n"] = "neighborhood"; +"Neighborhood" }; input(InputType.text) { name = "neighborhood"; id = "neighborhood" }
                    button(classes = "btn", type = ButtonType.submit) { attributes["data-i18n"] = "save"; +"Save" }
                }
            }
            div(classes = "requests-section") {
                h2 { attributes["data-i18n"] = "requestsFromRescuers"; +"Requests from Rescuers" }
                div { id = "requests-container"; +"" }
            }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
(async () => {
    try {
        const user = await api.me();
        if (user.authenticated === false) { location.href = '/login'; return; }
        if (!user.activeRoles?.includes('TEMPORAL_HOME') && !user.activeRoles?.includes('ADMIN')) { location.href = '/'; return; }
    } catch (e) { location.href = '/login'; return; }
})();

document.getElementById('temporal-home-form').onsubmit = async (e) => {
    e.preventDefault();
    const alias = document.getElementById('alias').value.trim();
    const country = document.getElementById('country').value.trim();
    const state = document.getElementById('state').value.trim();
    const city = document.getElementById('city').value.trim();
    const zip = document.getElementById('zip').value.trim();
    const neighborhood = document.getElementById('neighborhood').value.trim();
    const msg = document.getElementById('message');
    
    if (!alias || !country || !city) {
        msg.className = 'message error'; msg.textContent = 'Please fill in required fields.';
        return;
    }
    
    try {
        const response = await fetch('/api/users/temporal-home', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ alias, country, state: state || null, city, zip: zip || null, neighborhood: neighborhood || null })
        });
        if (!response.ok) throw new Error('Failed to create temporal home');
        msg.className = 'message success'; msg.textContent = 'Profile created!';
    } catch (err) { msg.className = 'message error'; msg.textContent = err.message; }
};

async function loadRequests() {
    try {
        const response = await fetch('/api/users/temporal-home/requests');
        if (!response.ok) throw new Error('Failed to load requests');
        const requests = await response.json();
        const container = document.getElementById('requests-container');
        if (!requests.length) { container.innerHTML = '<p>No requests yet.</p>'; return; }
        container.innerHTML = requests.map(r => '<div class="request-card"><p><strong>'+r.rescuerName+'</strong> wants help with '+(r.petName || 'a pet')+'</p><p>'+r.message+'</p><button class="btn btn-small" onclick="blockRescuer('+r.rescuerId+')">Block Rescuer</button></div>').join('');
    } catch (err) { console.error(err); }
}
loadRequests();

window.blockRescuer = async (rescuerId) => {
    if (!confirm('Block this rescuer from sending you more requests?')) return;
    try {
        const response = await fetch('/api/temporal-homes/block', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rescuerId })
        });
        const result = await response.json();
        alert(result.blocked ? 'Rescuer blocked!' : 'Already blocked');
        loadRequests();
    } catch (err) { alert(err.message); }
};
""".trimIndent()) } }
    }
}

private const val SEARCH_FILTER = "search-filter"

fun HTML.temporalHomesSearchPage() {
    commonHead("Find Temporal Homes - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "findTemporalHome"; +"Find Temporal Homes" }
            div(classes = "temporal-search-form") {
                div(classes = "search-country") {
                    label { htmlFor = "country"; attributes["data-i18n"] = "countryLabel"; +"Country" }
                    select { id = "search-country"; name = "country"; onChange = "onCountryChange()"
                        option { value = ""; attributes["data-i18n"] = "selectCountry"; +"Select a country" }
                        option { value = "Afghanistan"; attributes["data-i18n"] = "country.afghanistan"; +"Afghanistan" }
                        option { value = "Albania"; attributes["data-i18n"] = "country.albania"; +"Albania" }
                        option { value = "Algeria"; attributes["data-i18n"] = "country.algeria"; +"Algeria" }
                        option { value = "Argentina"; attributes["data-i18n"] = "country.argentina"; +"Argentina" }
                        option { value = "Armenia"; attributes["data-i18n"] = "country.armenia"; +"Armenia" }
                        option { value = "Australia"; attributes["data-i18n"] = "country.australia"; +"Australia" }
                        option { value = "Austria"; attributes["data-i18n"] = "country.austria"; +"Austria" }
                        option { value = "Azerbaijan"; attributes["data-i18n"] = "country.azerbaijan"; +"Azerbaijan" }
                        option { value = "Bangladesh"; attributes["data-i18n"] = "country.bangladesh"; +"Bangladesh" }
                        option { value = "Belarus"; attributes["data-i18n"] = "country.belarus"; +"Belarus" }
                        option { value = "Belgium"; attributes["data-i18n"] = "country.belgium"; +"Belgium" }
                        option { value = "Bolivia"; attributes["data-i18n"] = "country.bolivia"; +"Bolivia" }
                        option { value = "Bosnia and Herzegovina"; attributes["data-i18n"] = "country.bosnia"; +"Bosnia and Herzegovina" }
                        option { value = "Brazil"; attributes["data-i18n"] = "country.brazil"; +"Brazil" }
                        option { value = "Bulgaria"; attributes["data-i18n"] = "country.bulgaria"; +"Bulgaria" }
                        option { value = "Cambodia"; attributes["data-i18n"] = "country.cambodia"; +"Cambodia" }
                        option { value = "Cameroon"; attributes["data-i18n"] = "country.cameroon"; +"Cameroon" }
                        option { value = "Canada"; attributes["data-i18n"] = "country.canada"; +"Canada" }
                        option { value = "Chile"; attributes["data-i18n"] = "country.chile"; +"Chile" }
                        option { value = "China"; attributes["data-i18n"] = "country.china"; +"China" }
                        option { value = "Colombia"; attributes["data-i18n"] = "country.colombia"; +"Colombia" }
                        option { value = "Costa Rica"; attributes["data-i18n"] = "country.costaRica"; +"Costa Rica" }
                        option { value = "Croatia"; attributes["data-i18n"] = "country.croatia"; +"Croatia" }
                        option { value = "Cuba"; attributes["data-i18n"] = "country.cuba"; +"Cuba" }
                        option { value = "Czech Republic"; attributes["data-i18n"] = "country.czechia"; +"Czech Republic" }
                        option { value = "Denmark"; attributes["data-i18n"] = "country.denmark"; +"Denmark" }
                        option { value = "Dominican Republic"; attributes["data-i18n"] = "country.dominicanRepublic"; +"Dominican Republic" }
                        option { value = "Ecuador"; attributes["data-i18n"] = "country.ecuador"; +"Ecuador" }
                        option { value = "Egypt"; attributes["data-i18n"] = "country.egypt"; +"Egypt" }
                        option { value = "El Salvador"; attributes["data-i18n"] = "country.elSalvador"; +"El Salvador" }
                        option { value = "Estonia"; attributes["data-i18n"] = "country.estonia"; +"Estonia" }
                        option { value = "Ethiopia"; attributes["data-i18n"] = "country.ethiopia"; +"Ethiopia" }
                        option { value = "Finland"; attributes["data-i18n"] = "country.finland"; +"Finland" }
                        option { value = "France"; attributes["data-i18n"] = "country.france"; +"France" }
                        option { value = "Georgia"; attributes["data-i18n"] = "country.georgia"; +"Georgia" }
                        option { value = "Germany"; attributes["data-i18n"] = "country.germany"; +"Germany" }
                        option { value = "Ghana"; attributes["data-i18n"] = "country.ghana"; +"Ghana" }
                        option { value = "Greece"; attributes["data-i18n"] = "country.greece"; +"Greece" }
                        option { value = "Guatemala"; attributes["data-i18n"] = "country.guatemala"; +"Guatemala" }
                        option { value = "Haiti"; attributes["data-i18n"] = "country.haiti"; +"Haiti" }
                        option { value = "Honduras"; attributes["data-i18n"] = "country.honduras"; +"Honduras" }
                        option { value = "Hungary"; attributes["data-i18n"] = "country.hungary"; +"Hungary" }
                        option { value = "Iceland"; attributes["data-i18n"] = "country.iceland"; +"Iceland" }
                        option { value = "India"; attributes["data-i18n"] = "country.india"; +"India" }
                        option { value = "Indonesia"; attributes["data-i18n"] = "country.indonesia"; +"Indonesia" }
                        option { value = "Iran"; attributes["data-i18n"] = "country.iran"; +"Iran" }
                        option { value = "Iraq"; attributes["data-i18n"] = "country.iraq"; +"Iraq" }
                        option { value = "Ireland"; attributes["data-i18n"] = "country.ireland"; +"Ireland" }
                        option { value = "Israel"; attributes["data-i18n"] = "country.israel"; +"Israel" }
                        option { value = "Italy"; attributes["data-i18n"] = "country.italy"; +"Italy" }
                        option { value = "Jamaica"; attributes["data-i18n"] = "country.jamaica"; +"Jamaica" }
                        option { value = "Japan"; attributes["data-i18n"] = "country.japan"; +"Japan" }
                        option { value = "Jordan"; attributes["data-i18n"] = "country.jordan"; +"Jordan" }
                        option { value = "Kazakhstan"; attributes["data-i18n"] = "country.kazakhstan"; +"Kazakhstan" }
                        option { value = "Kenya"; attributes["data-i18n"] = "country.kenya"; +"Kenya" }
                        option { value = "Kuwait"; attributes["data-i18n"] = "country.kuwait"; +"Kuwait" }
                        option { value = "Latvia"; attributes["data-i18n"] = "country.latvia"; +"Latvia" }
                        option { value = "Lebanon"; attributes["data-i18n"] = "country.lebanon"; +"Lebanon" }
                        option { value = "Libya"; attributes["data-i18n"] = "country.libya"; +"Libya" }
                        option { value = "Lithuania"; attributes["data-i18n"] = "country.lithuania"; +"Lithuania" }
                        option { value = "Luxembourg"; attributes["data-i18n"] = "country.luxembourg"; +"Luxembourg" }
                        option { value = "Malaysia"; attributes["data-i18n"] = "country.malaysia"; +"Malaysia" }
                        option { value = "Mexico"; attributes["data-i18n"] = "country.mexico"; +"Mexico" }
                        option { value = "Moldova"; attributes["data-i18n"] = "country.moldova"; +"Moldova" }
                        option { value = "Mongolia"; attributes["data-i18n"] = "country.mongolia"; +"Mongolia" }
                        option { value = "Montenegro"; attributes["data-i18n"] = "country.montenegro"; +"Montenegro" }
                        option { value = "Morocco"; attributes["data-i18n"] = "country.morocco"; +"Morocco" }
                        option { value = "Myanmar"; attributes["data-i18n"] = "country.myanmar"; +"Myanmar" }
                        option { value = "Nepal"; attributes["data-i18n"] = "country.nepal"; +"Nepal" }
                        option { value = "Netherlands"; attributes["data-i18n"] = "country.netherlands"; +"Netherlands" }
                        option { value = "New Zealand"; attributes["data-i18n"] = "country.newZealand"; +"New Zealand" }
                        option { value = "Nicaragua"; attributes["data-i18n"] = "country.nicaragua"; +"Nicaragua" }
                        option { value = "Nigeria"; attributes["data-i18n"] = "country.nigeria"; +"Nigeria" }
                        option { value = "North Korea"; attributes["data-i18n"] = "country.northKorea"; +"North Korea" }
                        option { value = "Norway"; attributes["data-i18n"] = "country.norway"; +"Norway" }
                        option { value = "Pakistan"; attributes["data-i18n"] = "country.pakistan"; +"Pakistan" }
                        option { value = "Panama"; attributes["data-i18n"] = "country.panama"; +"Panama" }
                        option { value = "Paraguay"; attributes["data-i18n"] = "country.paraguay"; +"Paraguay" }
                        option { value = "Peru"; attributes["data-i18n"] = "country.peru"; +"Peru" }
                        option { value = "Philippines"; attributes["data-i18n"] = "country.philippines"; +"Philippines" }
                        option { value = "Poland"; attributes["data-i18n"] = "country.poland"; +"Poland" }
                        option { value = "Portugal"; attributes["data-i18n"] = "country.portugal"; +"Portugal" }
                        option { value = "Puerto Rico"; attributes["data-i18n"] = "country.puertoRico"; +"Puerto Rico" }
                        option { value = "Qatar"; attributes["data-i18n"] = "country.qatar"; +"Qatar" }
                        option { value = "Romania"; attributes["data-i18n"] = "country.romania"; +"Romania" }
                        option { value = "Russia"; attributes["data-i18n"] = "country.russia"; +"Russia" }
                        option { value = "Saudi Arabia"; attributes["data-i18n"] = "country.saudiArabia"; +"Saudi Arabia" }
                        option { value = "Serbia"; attributes["data-i18n"] = "country.serbia"; +"Serbia" }
                        option { value = "Singapore"; attributes["data-i18n"] = "country.singapore"; +"Singapore" }
                        option { value = "Slovakia"; attributes["data-i18n"] = "country.slovakia"; +"Slovakia" }
                        option { value = "Slovenia"; attributes["data-i18n"] = "country.slovenia"; +"Slovenia" }
                        option { value = "South Africa"; attributes["data-i18n"] = "country.southAfrica"; +"South Africa" }
                        option { value = "South Korea"; attributes["data-i18n"] = "country.southKorea"; +"South Korea" }
                        option { value = "Spain"; attributes["data-i18n"] = "country.spain"; +"Spain" }
                        option { value = "Sri Lanka"; attributes["data-i18n"] = "country.sriLanka"; +"Sri Lanka" }
                        option { value = "Sudan"; attributes["data-i18n"] = "country.sudan"; +"Sudan" }
                        option { value = "Sweden"; attributes["data-i18n"] = "country.sweden"; +"Sweden" }
                        option { value = "Switzerland"; attributes["data-i18n"] = "country.switzerland"; +"Switzerland" }
                        option { value = "Syria"; attributes["data-i18n"] = "country.syria"; +"Syria" }
                        option { value = "Taiwan"; attributes["data-i18n"] = "country.taiwan"; +"Taiwan" }
                        option { value = "Thailand"; attributes["data-i18n"] = "country.thailand"; +"Thailand" }
                        option { value = "Tunisia"; attributes["data-i18n"] = "country.tunisia"; +"Tunisia" }
                        option { value = "Turkey"; attributes["data-i18n"] = "country.turkey"; +"Turkey" }
                        option { value = "Ukraine"; attributes["data-i18n"] = "country.ukraine"; +"Ukraine" }
                        option { value = "United Arab Emirates"; attributes["data-i18n"] = "country.uae"; +"United Arab Emirates" }
                        option { value = "United Kingdom"; attributes["data-i18n"] = "country.uk"; +"United Kingdom" }
                        option { value = "United States"; attributes["data-i18n"] = "country.usa"; +"United States" }
                        option { value = "Uruguay"; attributes["data-i18n"] = "country.uruguay"; +"Uruguay" }
                        option { value = "Uzbekistan"; attributes["data-i18n"] = "country.uzbekistan"; +"Uzbekistan" }
                        option { value = "Venezuela"; attributes["data-i18n"] = "country.venezuela"; +"Venezuela" }
                        option { value = "Vietnam"; attributes["data-i18n"] = "country.vietnam"; +"Vietnam" }
                        option { value = "Yemen"; attributes["data-i18n"] = "country.yemen"; +"Yemen" }
                    }
                }
                div(classes = "search-filters") {
                    div(classes = SEARCH_FILTER){
                        label { htmlFor = "state"; attributes["data-i18n"] = "state"; +"State" }
                        input(InputType.text) { name = "state"; id = "search-state"; disabled = true }
                    }
                    div(classes = SEARCH_FILTER){
                        label { htmlFor = "city"; attributes["data-i18n"] = "city"; +"City" }
                        input(InputType.text) { name = "city"; id = "search-city"; disabled = true }
                    }
                    div(classes = SEARCH_FILTER){
                        label { htmlFor = "zip"; attributes["data-i18n"] = "zipCode"; +"Zip" }
                        input(InputType.text) { name = "zip"; id = "search-zip"; disabled = true; maxLength = "7" }
                    }
                    div(classes = SEARCH_FILTER){
                        label { htmlFor = "neighborhood"; attributes["data-i18n"] = "neighborhood"; +"Neighborhood" }
                        input(InputType.text) { name = "neighborhood"; id = "search-neighborhood"; disabled = true }
                    }
                }
                button(classes = "btn", type = ButtonType.button) { attributes["data-i18n"] = "search"; onClick = "searchTemporalHomes()"; +"Search" }
            }
            div { id = "results-container"; classes = setOf("temporal-homes-grid"); +"" }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
function onCountryChange() {
    const hasCountry = document.getElementById('search-country').value !== '';
    const filters = document.querySelectorAll('.search-filters input');
    filters.forEach(input => {
        input.disabled = !hasCountry;
        if (!hasCountry) input.value = '';
    });
    if (!hasCountry) {
        document.getElementById('results-container').innerHTML = '';
    }
}

function searchTemporalHomes() {
    const params = new URLSearchParams();
    const country = document.getElementById('search-country').value;
    const state = document.getElementById('search-state').value.trim();
    const city = document.getElementById('search-city').value.trim();
    const zip = document.getElementById('search-zip').value.trim();
    const neighborhood = document.getElementById('search-neighborhood').value.trim();
    
    if (!country) {
        document.getElementById('results-container').innerHTML = '<p>Please select a country.</p>';
        return;
    }
    
    params.append('country', country);
    if (state) params.append('state', state);
    if (city) params.append('city', city);
    if (zip) params.append('zip', zip);
    if (neighborhood) params.append('neighborhood', neighborhood);

    fetch('/api/temporal-homes?'+params.toString())
        .then(r => r.json())
        .then(homes => {
            const container = document.getElementById('results-container');
            if (!homes.length) { container.innerHTML = '<p>No temporal homes found.</p>'; return; }
            container.innerHTML = homes.map(h => '<div class="temporal-home-card"><h3>'+h.alias+'</h3><p>'+h.city+', '+h.country+'</p>'+(h.neighborhood ? '<p>'+h.neighborhood+'</p>' : '')+'</div>').join('');
        })
        .catch(err => console.error(err));
}
""".trimIndent()) } }
    }
}