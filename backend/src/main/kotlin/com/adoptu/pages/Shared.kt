package com.adoptu.pages

import kotlinx.html.*

fun HTML.commonHead(title: String, extraCss: String? = null) {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        this.title { +title }
        link(rel = "stylesheet", href = "/static/css/style.css")
        link(rel = "icon", href = "https://static.adopt-u.org/favicon.ico", type = "image/x-icon")
        link(rel = "stylesheet", href = "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200")
        extraCss?.let { link(rel = "stylesheet", href = "/static/css/$it") }
    }
}

fun A.commonLogo() {
    classes = setOf("logo")
    img(src = "https://static.adopt-u.org/logo.svg", alt = "Adopt-U Logo")
    span { +"Adopt-U" }
}

fun BODY.commonScripts(isLoggedIn: Boolean = false) {
    script(src = "/static/js/common.js") {}
    script { unsafe { raw("window.isLoggedInGlobal = $isLoggedIn; frontend.com.adoptu.frontend.Common.initDropdowns(); frontend.com.adoptu.frontend.Common.initI18n(null);") } }
}

fun DIV.languageDropdown() {
    div(classes = "lang-dropdown") {
        button(classes = "lang-dropbtn", type = ButtonType.button) { id = "lang-dropbtn"; +"🌐" }
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
        button(classes = "lang-dropbtn", type = ButtonType.button) { id = "lang-dropbtn"; +"🌐" }
        div(classes = "lang-dropdown-content") {
            a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸 English" }
            a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸 Español" }
            a(classes = "lang-option") { attributes["data-lang"] = "fr"; +"🇫🇷 Français" }
            a(classes = "lang-option") { attributes["data-lang"] = "pt"; +"🇧🇷 Português" }
            a(classes = "lang-option") { attributes["data-lang"] = "zh"; +"🇨🇳 中文" }
        }
    }
}

fun NAV.commonNav(isLoggedIn: Boolean = false, isAdmin: Boolean = false, isRescuerOrAdmin: Boolean = false, isTemporalHomeOrAdmin: Boolean = false) {
    div(classes = "nav-right") {
        if (isLoggedIn) {
            commonResourcesDropdown()
        }
        a("https://paypal.me/adoptu") { target = "_blank"; id = "nav-donate"; attributes["data-i18n"] = "donate"; +"Donate" }
        
        if (!isLoggedIn) {
            a("/login") { id = "nav-login"; attributes["data-i18n"] = "login"; +"Login" }
            a("/register") { id = "nav-register"; attributes["data-i18n"] = "register"; +"Register" }
        } else {
            div(classes = "user-menu") {
                div(classes = "user-avatar") { +"👤" }
                div(classes = "user-dropdown") {
                    a("/profile") { 
                        span { attributes["data-i18n"] = "profile"; +"Profile" }
                        span(classes = "material-symbols-outlined") { +Icons.USER }
                    }
                    if (isRescuerOrAdmin || isAdmin) {
                        a("/my-pets") {
                            span { attributes["data-i18n"] = "myPets"; +"My Pets" }
                            span(classes = "material-symbols-outlined") { +Icons.PAW }
                        }
                    }
                    if (isTemporalHomeOrAdmin || isAdmin) {
                        a("/temporal-home") {
                            span { attributes["data-i18n"] = "myTemporalHome"; +"My Temporal Home" }
                            span(classes = "material-symbols-outlined") { +Icons.HOME }
                        }
                    }
                    if (isAdmin) {
                        a("/admin") {
                            span { attributes["data-i18n"] = "admin"; +"Admin" }
                            span(classes = "material-symbols-outlined") { +Icons.SETTINGS }
                        }
                        a("/admin/shelters") {
                            span { attributes["data-i18n"] = "manageShelters"; +"Manage Shelters" }
                            span(classes = "material-symbols-outlined") { +Icons.HOME }
                        }
                    }
                    a("/logout") {
                        span { attributes["data-i18n"] = "logout"; +"Close session" }
                        span(classes = "material-symbols-outlined") { +Icons.LOGOUT }
                    }
                }
            }
        }
    }
    languageDropdown()
}

fun NAV.guestNav() {
    div(classes = "nav-right") {
        a("https://paypal.me/adoptu") { target = "_blank"; id = "nav-donate"; attributes["data-i18n"] = "donate"; +"Donate" }
        a("/login", classes = "nav-right") { id = "nav-login"; attributes["data-i18n"] = "login"; +"Login" }
        a("/register", classes = "nav-right") { id = "nav-register"; attributes["data-i18n"] = "register"; +"Register" }
        languageDropdown()
    }
}

fun DIV.commonResourcesDropdown() {
    div(classes = "resources-dropdown") {
        a(href = "#", classes = "resources-dropbtn") { 
            span { attributes["data-i18n"] = "resources"; +"Resources" }
            +" ▼"
        }
        div(classes = "resources-dropdown-content") {
            a("/shelters") { 
                span { attributes["data-i18n"] = "shelters"; +"Shelters" }
                span(classes = "material-symbols-outlined") { +Icons.SHELTER }
            }
            a("/photographers") { 
                span { attributes["data-i18n"] = "photographers"; +"Photographers" }
                span(classes = "material-symbols-outlined") { +Icons.CAMERA }
            }
            a("/sterilization-locations") { 
                span { attributes["data-i18n"] = "sterilizationLocations"; +"Sterilization Locations" }
                span(classes = "material-symbols-outlined") { +Icons.SYRINGE }
            }
            a("/temporal-homes") { 
                span { attributes["data-i18n"] = "findTemporalHomes"; +"Temporal Homes" }
                span(classes = "material-symbols-outlined") { +Icons.HOME }
            }
        }
    }
}

fun SELECT.countrySelect(id: String, includeSelectOption: Boolean = true, i18nKey: String = "selectCountry") {
    if (includeSelectOption) {
        option { value = ""; attributes["data-i18n"] = i18nKey }
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
