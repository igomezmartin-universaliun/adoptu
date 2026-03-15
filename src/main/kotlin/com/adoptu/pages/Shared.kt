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
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
}

fun DIV.languageDropdown() {
    div(classes = "lang-dropdown") {
        button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
        div(classes = "lang-dropdown-content") {
            a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
            a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
        }
    }
}

fun NAV.languageDropdown() {
    div(classes = "lang-dropdown") {
        button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
        div(classes = "lang-dropdown-content") {
            a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
            a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
        }
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

fun HTML.privacyPage() {
    commonHead("Privacy Policy - Adopt-U")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { attributes["data-i18n"] = "login"; +"Login" }
                languageDropdown()
            }
        }
        main {
            div(classes = "policy-content") {
                h1 { attributes["data-i18n"] = "privacyPolicy"; +"Privacy Policy" }
                p { +"Last updated: March 2025" }
                h2 { +"Information We Collect" }
                p { +"We collect information you provide directly to us, including your username, display name, and profile information when you create an account." }
                h2 { +"How We Use Information" }
                p { +"We use the information we collect to provide, maintain, and improve our services and to communicate with you about pet adoption opportunities." }
                h2 { +"Contact Us" }
                p { +"If you have any questions about this Privacy Policy, please contact us." }
            }
        }
        footer()
        commonScripts()
    }
}

fun HTML.termsPage() {
    commonHead("Terms and Conditions - Adopt-U")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { attributes["data-i18n"] = "login"; +"Login" }
                languageDropdown()
            }
        }
        main {
            div(classes = "policy-content") {
                h1 { attributes["data-i18n"] = "termsConditions"; +"Terms and Conditions" }
                p { +"Last updated: March 2025" }
                h2 { +"Acceptance of Terms" }
                p { +"By accessing and using Adopt-U, you accept and agree to be bound by the terms and provision of this agreement." }
                h2 { +"User Responsibilities" }
                p { +"Users are responsible for providing accurate information and for using the platform in accordance with applicable laws and regulations." }
                h2 { +"Pet Adoption" }
                p { +"Adopt-U is a platform connecting potential adopters with pet rescuers. We do not guarantee the accuracy of pet listings or the outcome of adoption processes." }
                h2 { +"Contact Us" }
                p { +"If you have any questions about these Terms and Conditions, please contact us." }
            }
        }
        footer()
        commonScripts()
    }
}
