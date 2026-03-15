package com.adoptu.pages

import kotlinx.html.*

fun HTML.indexPage() {
    commonHead("Adopt-U - Pet Adoption")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
                a("/pets") { id = "nav-browse"; attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { id = "nav-login"; attributes["data-i18n"] = "login"; +"Login" }
                a("/register") { id = "nav-register"; attributes["data-i18n"] = "register"; +"Register" }
                a("/my-pets") { id = "nav-mypets"; style = "display:none"; attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("/admin") { id = "nav-admin"; style = "display:none"; attributes["data-i18n"] = "admin"; +"Admin" }
                a("#") { id = "nav-logout"; style = "display:none"; attributes["data-i18n"] = "logout"; +"Logout" }
            }
        }
        main {
            section(classes = "hero") {
                h1 { attributes["data-i18n"] = "findYourNewBestFriend"; +"Find Your New Best Friend" }
                p { attributes["data-i18n"] = "dogsCatsBirdsFish"; +"Dogs, cats, birds, fish - all waiting for a loving home." }
                a("/pets") { classes = setOf("btn"); attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
            }
        }
        footer()
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
(async () => {
    const user = await api.me();
    if (user.authenticated !== false) {
        document.getElementById('nav-login').style.display = 'none';
        document.getElementById('nav-register').style.display = 'none';
        document.getElementById('nav-mypets').style.display = 'inline';
        document.getElementById('nav-logout').style.display = 'inline';
        if (user.role === 'ADMIN') document.getElementById('nav-admin').style.display = 'inline';
        document.getElementById('nav-logout').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
    }
})();
""".trimIndent()) } }
    }
}
