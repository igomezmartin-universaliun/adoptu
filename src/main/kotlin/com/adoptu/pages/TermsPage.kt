package com.adoptu.pages

import kotlinx.html.*

fun HTML.termsPage() {
    commonHead("Terms and Conditions - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
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
