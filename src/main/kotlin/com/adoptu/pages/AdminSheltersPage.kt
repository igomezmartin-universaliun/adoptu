package com.adoptu.pages

import com.adoptu.routes.NavParams
import kotlinx.html.*

fun HTML.adminSheltersPage(navParams: NavParams = NavParams()) {
    commonHead("Manage Shelters - Adopt-U", "shelters.css")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav(navParams.isLoggedIn, navParams.isAdmin, navParams.isRescuerOrAdmin, navParams.isTemporalHomeOrAdmin) }
        }
        main {
            h1 { attributes["data-i18n"] = "manageShelters"; +"Manage Shelters" }
            
            div(classes = "filter-form") {
                div(classes = "search-country") {
                    label { htmlFor = "filter-country"; attributes["data-i18n"] = "country"; +"Country" }
                    select { id = "filter-country"; name = "country" }
                }
                div(classes = "search-state") {
                    label { htmlFor = "filter-state"; attributes["data-i18n"] = "state"; +"State" }
                    select { id = "filter-state"; name = "state" }
                }
            }
            div { id = "message"; +"" }
            div { id = "shelters"; classes = setOf("shelter-list"); +"" }
        }
        footer()
        commonScripts(navParams.isLoggedIn)
        script(src = "/static/js/admin-shelters.js") {}
    }
}