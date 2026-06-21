package com.adoptu.pages

import kotlinx.html.*

fun DIV.locationSearchFilters(
    formClass: String = "location-search-form",
    includeNeighborhood: Boolean = true
) {
    div(classes = formClass) {
        div(classes = "location-search-country") {
            label { htmlFor = "search-country"; attributes["data-i18n"] = "countryLabel"; +"Country" }
            select {
                id = "search-country"
                name = "country"
                onChange = "window.onCountryChange()"
                countrySelect("search-country", true, "selectCountryToSearch")
            }
        }
        div(classes = "location-search-filters") {
            div(classes = "location-search-filter") {
                label { htmlFor = "search-state"; attributes["data-i18n"] = "state"; +"State" }
                input(InputType.text) { name = "state"; id = "search-state"; disabled = true }
            }
            div(classes = "location-search-filter") {
                label { htmlFor = "search-city"; attributes["data-i18n"] = "city"; +"City" }
                input(InputType.text) { name = "city"; id = "search-city"; disabled = true }
            }
            div(classes = "location-search-filter") {
                label { htmlFor = "search-zip"; attributes["data-i18n"] = "zipCode"; +"Zip" }
                input(InputType.text) { name = "zip"; id = "search-zip"; disabled = true; maxLength = "7" }
            }
            if (includeNeighborhood) {
                div(classes = "location-search-filter") {
                    label { htmlFor = "search-neighborhood"; attributes["data-i18n"] = "neighborhood"; +"Neighborhood" }
                    input(InputType.text) { name = "neighborhood"; id = "search-neighborhood"; disabled = true }
                }
            }
        }
    }
}