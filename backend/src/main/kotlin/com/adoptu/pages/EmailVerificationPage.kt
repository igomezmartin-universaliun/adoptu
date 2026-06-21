package com.adoptu.pages

import com.adoptu.routes.NavParams
import kotlinx.html.*

fun HTML.emailVerificationPage(success: Boolean, language: String = "en", navParams: NavParams = NavParams()) {
    commonHead("Email Verification - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav(navParams.isLoggedIn, navParams.isAdmin, navParams.isRescuerOrAdmin, navParams.isTemporalHomeOrAdmin) }
        }
        main {
            div(classes = "verification-container") {
                if (success) {
                    div(classes = "verification-success") {
                        h1(classes = "success-title") {
                            attributes["data-i18n"] = "emailVerifiedTitle"
                            +"Email Verified Successfully!"
                        }
                        div(classes = "success-icon") {
                            img(src = Icons.CHECK_CIRCLE, alt = "Success") { width = "80"; height = "80" }
                        }
                        p(classes = "success-message") {
                            attributes["data-i18n"] = "emailVerifiedMessage"
                            +"Your email has been verified. Your registration is now complete."
                        }
                        p(classes = "redirect-message") {
                            attributes["data-i18n"] = "redirectingIn"
                            +"Redirecting to the main page in "
                        }
                        span(classes = "countdown") { id = "countdown"; +"10" }
                        span { +"..." }
                        div(classes = "login-prompt") {
                            p {
                                attributes["data-i18n"] = "canAlsoLogin"
                                +"You can also "
                            }
                            a("/login") {
                                attributes["data-i18n"] = "loginNow"
                                +"login now"
                            }
                        }
                    }
                } else {
                    div(classes = "verification-error") {
                        h1(classes = "error-title") {
                            attributes["data-i18n"] = "emailVerificationFailed"
                            +"Verification Failed"
                        }
                        div(classes = "error-icon") {
                            img(src = Icons.ERROR_CIRCLE, alt = "Error") { width = "80"; height = "80" }
                        }
                        p(classes = "error-message") {
                            attributes["data-i18n"] = "invalidOrExpiredToken"
                            +"The verification link is invalid or has expired."
                        }
                        p(classes = "try-again") {
                            a("/register") {
                                attributes["data-i18n"] = "tryRegisteringAgain"
                                +"Please try registering again."
                            }
                        }
                    }
                }
            }
        }
        footer()
        script(src = "/static/js/email-verification.js") {}
        commonScripts(navParams.isLoggedIn)
    }
}
