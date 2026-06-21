package com.adoptu.pages

import com.adoptu.routes.NavParams
import kotlinx.html.*

fun HTML.registerPage(navParams: NavParams = NavParams()) {
    commonHead("Register - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav(navParams.isLoggedIn, navParams.isAdmin, navParams.isRescuerOrAdmin, navParams.isTemporalHomeOrAdmin) }
        }
        main {
            div {
                id="auth-form"
                classes = setOf("auth-form", "register-simple")
                h1 { 
                    attributes["data-i18n"] = "registerNewAccount"; 
                    style = "text-align: center;"
                    +"Create Account" 
                }
                
                form { id = "register-form"
                    h2 { attributes["data-i18n"] = "accountDetails"; +"Account Details" }
                    label { htmlFor = "email"; attributes["data-i18n"] = "email"; +"Email" }; input(InputType.email) { name = "email"; id = "email"; required = true }
                    label { htmlFor = "displayName"; attributes["data-i18n"] = "displayName"; +"Display Name" }; input(InputType.text) { name = "displayName"; id = "displayName"; required = true }
                    
                    label { attributes["data-i18n"] = "selectAdditionalRoles"; +"Select additional roles:" }
                    div(classes = "checkbox-group") {
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "ADOPTER"; id = "role-adopter"; checked = true; disabled = true; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "adoptPet"; +"Adopt a pet" }
                            +" (required)"
                        }
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "RESCUER"; id = "role-rescuer"; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "publishPets"; +"Publish pets for adoption" }
                        }
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "PHOTOGRAPHER"; id = "role-photographer"; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "offerPhotography"; +"Offer photography services" }
                        }
                        div {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0;"
                            input(InputType.checkBox) { name = "roles"; value = "TEMPORAL_HOME"; id = "role-temporal-home"; style = "width: auto; height: auto; margin: 0;" }
                            span { attributes["data-i18n"] = "provideTemporaryHome"; +"Provide temporary home for pets" }
                        }
                    }
                    
                    h2 { attributes["data-i18n"] = "registrationMethod"; +"Registration Method" }
                    
                    div(classes = "method-selection") {
                        p { style = "margin: 0 0 0.5rem 0; font-size: 0.9rem; color: var(--text-muted);"
                            +"Select one or more login methods:" }
                        label {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0; cursor: pointer;"
                            input(InputType.checkBox) { name = "method"; value = "passkey"; id = "method-passkey"; checked = true; style = "width: auto; height: auto; margin: 0;" }
                            +"Passkey (most secure, works on all your devices)"
                        }
                        label {
                            style = "display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0; cursor: pointer;"
                            input(InputType.checkBox) { name = "method"; value = "password"; id = "method-password"; style = "width: auto; height: auto; margin: 0;" }
                            +"Password (less secure, use as backup only)"
                        }
                    }
                    
                    div(classes = "password-fields") {
                        attributes["id"] = "password-fields"
                        div(classes = "form-row") {
                            label { htmlFor = "password"; attributes["data-i18n"] = "password"; +"Password" }
                            input(InputType.password) { id = "password"; name = "password" }
                        }
                        div(classes = "form-row") {
                            label { htmlFor = "confirmPassword"; attributes["data-i18n"] = "confirmPassword"; +"Confirm Password" }
                            input(InputType.password) { id = "confirmPassword"; name = "confirmPassword" }
                        }
                        div(classes = "password-requirements") {
                            p { id = "password-requirements-text"; attributes["data-i18n"] = "passwordRequirements"; +"Password must be at least 8 characters and contain uppercase, lowercase, number, and symbol" }
                            div { id = "password-checks"; +"" }
                        }
                    }
                    
                    p { id = "message"; +"" }
                    button(classes = "btn", type = ButtonType.submit) {
                        id="register-button"
                        attributes["data-i18n"] = "register"; +"Register"
                    }
                }
                
                div(classes = "form-actions") {
                    p { style = "margin-top: 1rem; text-align: center; width: 100%;"; attributes["data-i18n"] = "alreadyHaveAccount"; +"Already have an account?" }
                    a(href = "/login") { button(classes = "btn btn-secondary", type = ButtonType.button) {
                        id="register-page-login"
                        attributes["data-i18n"] = "login"; +"Login"
                    } }
                }
            }
        }
        footer()
        commonScripts(navParams.isLoggedIn)
    }
}