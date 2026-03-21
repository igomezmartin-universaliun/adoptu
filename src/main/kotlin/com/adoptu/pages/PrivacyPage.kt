package com.adoptu.pages

import kotlinx.html.*

fun HTML.privacyPage() {
    commonHead("Privacy Policy - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
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
