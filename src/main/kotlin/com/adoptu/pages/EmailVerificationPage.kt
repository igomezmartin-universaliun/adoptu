package com.adoptu.pages

import kotlinx.html.*

fun HTML.emailVerificationPage(success: Boolean, language: String = "en") {
    commonHead("Email Verification - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
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
                            unsafe { raw("""<svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#4CAF50" stroke-width="2">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                                <polyline points="22 4 12 14.01 9 11.01"/>
                            </svg>""") }
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
                            unsafe { raw("""<svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#f44336" stroke-width="2">
                                <circle cx="12" cy="12" r="10"/>
                                <line x1="15" y1="9" x2="9" y2="15"/>
                                <line x1="9" y1="9" x2="15" y2="15"/>
                            </svg>""") }
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
        script { unsafe { raw("""
            let seconds = 10;
            const countdownEl = document.getElementById('countdown');
            const interval = setInterval(() => {
                seconds--;
                if (countdownEl) countdownEl.textContent = seconds;
                if (seconds <= 0) {
                    clearInterval(interval);
                    window.location.href = '/';
                }
            }, 1000);
        """) } }
        commonScripts()
    }
}
