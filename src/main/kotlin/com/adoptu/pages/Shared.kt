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
