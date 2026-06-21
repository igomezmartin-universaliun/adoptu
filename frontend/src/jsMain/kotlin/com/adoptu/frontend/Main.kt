package com.adoptu.frontend

import com.adoptu.frontend.pages.IndexPageModule
import com.adoptu.frontend.pages.LoginPageModule
import com.adoptu.frontend.pages.ProfilePageModule
import com.adoptu.frontend.pages.RegisterPageModule
import com.adoptu.frontend.pages.MyPetsPageModule
import com.adoptu.frontend.pages.PetDetailPageModule
import com.adoptu.frontend.pages.SheltersPageModule
import com.adoptu.frontend.pages.TemporalHomeSearchPageModule
import com.adoptu.frontend.pages.PhotographersPageModule
import com.adoptu.frontend.pages.SterilizationLocationsPageModule
import kotlinx.browser.window

fun main() {
    console.log("Frontend main() started, path: " + window.location.pathname)
    try {
        val path = window.location.pathname

        window.asDynamic().AdoptuI18n = I18n
        window.asDynamic().AdoptuCommon = CommonModule
        window.asDynamic().AdoptuWebAuthn = WebAuthnModule
        window.asDynamic().AdoptuIndexPage = IndexPageModule
        window.asDynamic().AdoptuLoginPage = LoginPageModule
        window.asDynamic().AdoptuProfilePage = ProfilePageModule
        window.asDynamic().AdoptuRegisterPage = RegisterPageModule
        window.asDynamic().AdoptuMyPets = MyPetsPageModule

        CommonModule.initI18n(null).then<Unit> {
            try {
                when {
                    path == "/" -> IndexPageModule.init()
                    path == "/login" || path == "/login/" -> LoginPageModule.init()
                    path == "/register" || path == "/register/" -> RegisterPageModule.init()
                    path == "/profile" || path == "/profile/" -> ProfilePageModule.init()
                    path == "/my-pets" || path == "/my-pets/" -> MyPetsPageModule.init()
                    path.startsWith("/pet/") -> PetDetailPageModule.init()
                    path == "/shelters" || path == "/shelters/" -> SheltersPageModule.init()
                    path == "/temporal-homes" || path == "/temporal-homes/" -> TemporalHomeSearchPageModule.init()
                    path == "/photographers" || path == "/photographers/" -> PhotographersPageModule.init()
                    path == "/sterilization-locations" || path == "/sterilization-locations/" -> SterilizationLocationsPageModule.init()
                    else -> {}
                }
            } catch (e: dynamic) {}
        }
    } catch (e: dynamic) {}
}