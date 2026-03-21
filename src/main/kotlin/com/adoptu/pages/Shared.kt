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
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
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
    if (!e.target.closest('.user-menu')) {
        document.getElementById('user-dropdown')?.style?.setProperty('display', 'none');
    }
});
i18n.updatePage();
window.cachedUserData = null;
(async () => {
    let user = window.cachedUserData;
    if (!user) {
        try {
            user = await api.me();
            window.cachedUserData = user;
        } catch (e) { return; }
    }
    if (user.authenticated !== false) {
        if (user.language) {
            i18n.setLang(user.language, false);
        }
        document.getElementById('nav-login')?.style?.setProperty('display', 'none');
        document.getElementById('nav-register')?.style?.setProperty('display', 'none');
        document.getElementById('nav-photographers')?.style?.setProperty('display', 'inline');
        document.getElementById('nav-temporal-homes')?.style?.setProperty('display', 'inline');
        document.getElementById('user-avatar')?.style?.setProperty('display', 'flex');
        document.getElementById('user-dropdown')?.style?.setProperty('display', 'block');
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
    }
})();
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
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>""") }
                    span { attributes["data-i18n"] = "profile"; +"Profile" }
                }
                a("/my-pets") { id = "dropdown-mypets"; 
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>""") }
                    span { attributes["data-i18n"] = "myPets"; +"My Pets" }
                }
                a("/temporal-home") { style = "display:none"; id = "dropdown-temporal-home";
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>""") }
                    span { attributes["data-i18n"] = "myTemporalHome"; +"My Temporal Home" }
                }
                a("/admin") { style = "display:none"; id = "dropdown-admin"; 
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>""") }
                    span { attributes["data-i18n"] = "admin"; +"Admin" }
                }
                a("#") { style = "display:none"; id = "dropdown-logout";
                    unsafe { raw("""<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>""") }
                    span { attributes["data-i18n"] = "logout"; +"Close session" }
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

fun BODY.footer() {
    footer {
        a("/privacy") { attributes["data-i18n"] = "privacyPolicy"; +"Privacy Policy" }
        span { +" | " }
        a("/terms") { attributes["data-i18n"] = "termsConditions"; +"Terms and Conditions" }
        span { +" | " }
        span { +"© 2025 Adopt-U" }
    }
}
