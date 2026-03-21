package com.adoptu.pages

import kotlinx.html.*

fun HTML.commonHead(title: String) {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        this.title { +title }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
}

fun A.commonLogo() {
    classes = setOf("logo")
    img(src = "https://d1i54dtpue2yh6.cloudfront.net/logo.svg", alt = "Adopt-U Logo")
    span { +"Adopt-U" }
}

fun BODY.commonScripts() {
    script(src = "/static/js/api.js") {}
    script(src = "/static/js/i18n.js") {}
    script { unsafe { raw("""
const langEmojis = { en: '🇺🇸', es: '🇪🇸', fr: '🇫🇷', pt: '🇧🇷', zh: '🇨🇳' };
const langLabels = { en: 'English', es: 'Español', fr: 'Français', pt: 'Português', zh: '中文' };

function updateLangButton() {
    const lang = localStorage.getItem('lang') || 'en';
    const btn = document.querySelector('.lang-dropbtn');
    if (btn) {
        btn.innerHTML = langEmojis[lang] + ' ▼';
    }
}

document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); updateLangButton(); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.querySelector('.user-avatar')?.addEventListener('click', (e) => {
    e.stopPropagation();
    const dropdown = document.getElementById('user-dropdown');
    dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
});
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
    const userDropdown = document.getElementById('user-dropdown');
    if (userDropdown && userDropdown.style.display !== 'none' && !e.target.closest('.user-menu')) {
        userDropdown.style.display = 'none';
    }
});
i18n.updatePage();
updateLangButton();
window.userDataPromise = (async () => {
    if (window.cachedUserData) return window.cachedUserData;
    try {
        const user = await api.me();
        window.cachedUserData = user;
        return user;
    } catch (e) { return null; }
})();

async function checkProfileCompletion(user) {
    const roles = user.activeRoles || [];
    const needsRedirect = [];
    
    if (roles.includes('PHOTOGRAPHER')) {
        if (!user.photographerCountry || !user.photographerState) {
            needsRedirect.push('PHOTOGRAPHER');
        }
    }
    
    if (roles.includes('TEMPORAL_HOME')) {
        try {
            await api.getTemporalHome();
        } catch (e) {
            needsRedirect.push('TEMPORAL_HOME');
        }
    }
    
    return needsRedirect.length > 0;
}

window.userDataPromise.then(async user => {
    if (user && user.authenticated !== false) {
        const roles = user.activeRoles || [];
        if (roles.includes('PHOTOGRAPHER') || roles.includes('TEMPORAL_HOME')) {
            const currentPath = window.location.pathname;
            if (currentPath !== '/profile') {
                const needsCompletion = await checkProfileCompletion(user);
                if (needsCompletion) {
                    window.location.href = '/profile';
                    return;
                }
            }
        }
        if (user.language) {
            i18n.setLang(user.language, false);
        }
        document.getElementById('nav-login')?.style?.setProperty('display', 'none');
        document.getElementById('nav-register')?.style?.setProperty('display', 'none');
        document.getElementById('nav-photographers')?.style?.setProperty('display', 'inline');
        document.getElementById('nav-temporal-homes')?.style?.setProperty('display', 'inline');
        document.getElementById('user-avatar')?.style?.setProperty('display', 'flex');
        document.getElementById('user-dropdown')?.style?.setProperty('display', 'none');
        document.getElementById('dropdown-profile')?.style?.setProperty('display', 'flex');
        document.getElementById('dropdown-mypets')?.style?.setProperty('display', 'flex');
        if (user.activeRoles?.includes('RESCUER') || user.activeRoles?.includes('ADMIN')) {
            document.getElementById('dropdown-mypets')?.style?.setProperty('display', 'flex');
        }
        if (user.activeRoles?.includes('TEMPORAL_HOME') || user.activeRoles?.includes('ADMIN')) {
            document.getElementById('dropdown-temporal-home')?.style?.setProperty('display', 'flex');
        }
        document.getElementById('dropdown-logout')?.style?.setProperty('display', 'flex');
        if (user.activeRoles?.includes('ADMIN')) {
            document.getElementById('dropdown-admin')?.style?.setProperty('display', 'block');
        }
        const logoutLink = document.getElementById('dropdown-logout');
        if (logoutLink) {
            logoutLink.onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
        }
        const INACTIVITY_TIMEOUT = 5 * 60 * 1000;
        let inactivityTimer;
        const resetInactivityTimer = () => {
            clearTimeout(inactivityTimer);
            inactivityTimer = setTimeout(async () => {
                await api.logout();
                location.reload();
            }, INACTIVITY_TIMEOUT);
        };
        ['click', 'keypress', 'mousemove', 'scroll', 'touchstart'].forEach(event => {
            document.addEventListener(event, resetInactivityTimer, { passive: true });
        });
        resetInactivityTimer();
    }
});
""") } }
}

fun DIV.languageDropdown() {
    div(classes = "lang-dropdown") {
        button(classes = "lang-dropbtn", type = ButtonType.button) { +"🌐" }
        div(classes = "lang-dropdown-content") {
            a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸 English" }
            a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸 Español" }
            a(classes = "lang-option") { attributes["data-lang"] = "fr"; +"🇫🇷 Français" }
            a(classes = "lang-option") { attributes["data-lang"] = "pt"; +"🇧🇷 Português" }
            a(classes = "lang-option") { attributes["data-lang"] = "zh"; +"🇨🇳 中文" }
        }
    }
}

fun NAV.languageDropdown() {
    div(classes = "lang-dropdown") {
        button(classes = "lang-dropbtn", type = ButtonType.button) { +"🌐" }
        div(classes = "lang-dropdown-content") {
            a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸 English" }
            a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸 Español" }
            a(classes = "lang-option") { attributes["data-lang"] = "fr"; +"🇫🇷 Français" }
            a(classes = "lang-option") { attributes["data-lang"] = "pt"; +"🇧🇷 Português" }
            a(classes = "lang-option") { attributes["data-lang"] = "zh"; +"🇨🇳 中文" }
        }
    }
}

fun NAV.commonNav() {

    div(classes = "nav-right") {
        a("/photographers") { style = "display:none"; id = "nav-photographers"; attributes["data-i18n"] = "photographers"; +"Photographers" }
        a("/temporal-homes") { style = "display:none"; id = "nav-temporal-homes"; attributes["data-i18n"] = "findTemporalHomes"; +"Find Temporal Homes" }
        a("/login") { id = "nav-login"; attributes["data-i18n"] = "login"; +"Login" }
        a("/register") { id = "nav-register"; attributes["data-i18n"] = "register"; +"Register" }

        div(classes = "user-menu") {
            div(classes = "user-avatar") { id = "user-avatar"; style = "display:none"; +"👤" }
            div(classes = "user-dropdown") { id = "user-dropdown"; style = "display:none"
                a("/profile") { id = "dropdown-profile"; 
                    span { attributes["data-i18n"] = "profile"; +"Profile" }
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>""") }
                }
                a("/my-pets") { id = "dropdown-mypets";
                    span { attributes["data-i18n"] = "myPets"; +"My Pets" }
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 5.172C10 3.782 8.423 2.679 6.5 3c-2.823.47-4.113 6.006-4 7 .08.703 1.725 1.722 3.656 1 1.261-.472 1.96-1.45 2.344-2.5"/><path d="M14.267 5.172c0-1.39 1.577-2.493 3.5-2.172 2.823.47 4.113 6.006 4 7-.08.703-1.725 1.722-3.656 1-1.261-.472-1.96-1.45-2.344-2.5"/><path d="M8 14v.5"/><path d="M16 14v.5"/><path d="M11.25 16.25h1.5L12 17l-.75-.75Z"/><path d="M4.42 11.247A13.152 13.152 0 0 0 4 14.556C4 18.728 7.582 21 12 21s8-2.272 8-6.444c0-1.061-.162-2.2-.493-3.309m-9.243-6.082A8.801 8.801 0 0 1 12 5c.78 0 1.5.108 2.161.306"/></svg>""") }
                }
                a("/temporal-home") { style = "display:none"; id = "dropdown-temporal-home";
                    span { attributes["data-i18n"] = "myTemporalHome"; +"My Temporal Home" }
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>""") }
                }
                a("/admin") { style = "display:none"; id = "dropdown-admin";
                    span { attributes["data-i18n"] = "admin"; +"Admin" }
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>""") }
                }
                a("#") { style = "display:none"; id = "dropdown-logout";
                    span { attributes["data-i18n"] = "logout"; +"Close session" }
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>""") }
                }
            }
        }
    }
    languageDropdown()
}

fun NAV.guestNav() {
    div(classes = "nav-right") {
        a("/login", classes = "nav-right") { id = "nav-login"; attributes["data-i18n"] = "login"; +"Login" }
        a("/register", classes = "nav-right") { id = "nav-register"; attributes["data-i18n"] = "register"; +"Register" }
        languageDropdown()
    }
}

fun SELECT.countrySelect(id: String, includeSelectOption: Boolean = true) {
    if (includeSelectOption) {
        option { value = ""; attributes["data-i18n"] = "selectCountry"; +"Select a country" }
    }
    val countries = listOf(
        "Afghanistan" to "country.afghanistan",
        "Albania" to "country.albania",
        "Algeria" to "country.algeria",
        "Argentina" to "country.argentina",
        "Armenia" to "country.armenia",
        "Australia" to "country.australia",
        "Austria" to "country.austria",
        "Azerbaijan" to "country.azerbaijan",
        "Bangladesh" to "country.bangladesh",
        "Belarus" to "country.belarus",
        "Belgium" to "country.belgium",
        "Bolivia" to "country.bolivia",
        "Bosnia and Herzegovina" to "country.bosnia",
        "Brazil" to "country.brazil",
        "Bulgaria" to "country.bulgaria",
        "Cambodia" to "country.cambodia",
        "Cameroon" to "country.cameroon",
        "Canada" to "country.canada",
        "Chile" to "country.chile",
        "China" to "country.china",
        "Colombia" to "country.colombia",
        "Costa Rica" to "country.costaRica",
        "Croatia" to "country.croatia",
        "Cuba" to "country.cuba",
        "Czech Republic" to "country.czechia",
        "Denmark" to "country.denmark",
        "Dominican Republic" to "country.dominicanRepublic",
        "Ecuador" to "country.ecuador",
        "Egypt" to "country.egypt",
        "El Salvador" to "country.elSalvador",
        "Estonia" to "country.estonia",
        "Ethiopia" to "country.ethiopia",
        "Finland" to "country.finnland",
        "France" to "country.france",
        "Georgia" to "country.georgia",
        "Germany" to "country.germany",
        "Ghana" to "country.ghana",
        "Greece" to "country.greece",
        "Guatemala" to "country.guatemala",
        "Haiti" to "country.haiti",
        "Honduras" to "country.honduras",
        "Hungary" to "country.hungary",
        "Iceland" to "country.iceland",
        "India" to "country.india",
        "Indonesia" to "country.indonesia",
        "Iran" to "country.iran",
        "Iraq" to "country.iraq",
        "Ireland" to "country.ireland",
        "Israel" to "country.israel",
        "Italy" to "country.italy",
        "Jamaica" to "country.jamaica",
        "Japan" to "country.japan",
        "Jordan" to "country.jordan",
        "Kazakhstan" to "country.kazakhstan",
        "Kenya" to "country.kenya",
        "Kuwait" to "country.kuwait",
        "Latvia" to "country.latvia",
        "Lebanon" to "country.lebanon",
        "Libya" to "country.libya",
        "Lithuania" to "country.lithuania",
        "Luxembourg" to "country.luxembourg",
        "Malaysia" to "country.malaysia",
        "Mexico" to "country.mexico",
        "Moldova" to "country.moldova",
        "Mongolia" to "country.mongolia",
        "Montenegro" to "country.montenegro",
        "Morocco" to "country.morocco",
        "Myanmar" to "country.myanmar",
        "Nepal" to "country.nepal",
        "Netherlands" to "country.netherlands",
        "New Zealand" to "country.newZealand",
        "Nicaragua" to "country.nicaragua",
        "Nigeria" to "country.nigeria",
        "North Korea" to "country.northKorea",
        "Norway" to "country.norway",
        "Pakistan" to "country.pakistan",
        "Panama" to "country.panama",
        "Paraguay" to "country.paraguay",
        "Peru" to "country.peru",
        "Philippines" to "country.philippines",
        "Poland" to "country.poland",
        "Portugal" to "country.portugal",
        "Puerto Rico" to "country.puertoRico",
        "Qatar" to "country.qatar",
        "Romania" to "country.romania",
        "Russia" to "country.russia",
        "Saudi Arabia" to "country.saudiArabia",
        "Serbia" to "country.serbia",
        "Singapore" to "country.singapore",
        "Slovakia" to "country.slovakia",
        "Slovenia" to "country.slovenia",
        "South Africa" to "country.southAfrica",
        "South Korea" to "country.southKorea",
        "Spain" to "country.spain",
        "Sri Lanka" to "country.sriLanka",
        "Sudan" to "country.sudan",
        "Sweden" to "country.sweden",
        "Switzerland" to "country.switzerland",
        "Syria" to "country.syria",
        "Taiwan" to "country.taiwan",
        "Thailand" to "country.thailand",
        "Tunisia" to "country.tunisia",
        "Turkey" to "country.turkey",
        "Ukraine" to "country.ukraine",
        "United Arab Emirates" to "country.uae",
        "United Kingdom" to "country.uk",
        "United States" to "country.usa",
        "Uruguay" to "country.uraguay",
        "Uzbekistan" to "country.uzbekistan",
        "Venezuela" to "country.venezuela",
        "Vietnam" to "country.vietnam",
        "Yemen" to "country.yemen"
    )
    countries.forEach { (value, i18nKey) ->
        option { this.value = value; attributes["data-i18n"] = i18nKey; +value }
    }
}

fun BODY.footer() {
    footer {
        a("/privacy") { attributes["data-i18n"] = "privacyPolicy"; +"Privacy Policy" }
        span { +" | " }
        a("/terms") { attributes["data-i18n"] = "termsConditions"; +"Terms and Conditions" }
        span { +" | " }
        span { +"© 2025 Adopt-U" }
    }
}
