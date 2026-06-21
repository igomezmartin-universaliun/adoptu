package com.adoptu.frontend.pages

import com.adoptu.frontend.ApiClientModule
import kotlinx.browser.document
import org.w3c.dom.*
import kotlin.js.Promise

@JsExport
@JsName("IndexPage")
object IndexPageModule {
    private var currentType = ""

    fun init() {
        // Placeholder - simplified for build
    }

    fun loadPets(): Promise<dynamic> {
        return ApiClientModule.getPets(currentType).then<Unit> { pets ->
            renderPets(pets)
            pets
        }
    }

    private fun renderPets(pets: dynamic) {
        val container = document.getElementById("pets-container")
        if (container != null) {
            container.innerHTML = ""
        }
    }
}