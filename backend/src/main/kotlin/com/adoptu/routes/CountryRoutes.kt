package com.adoptu.routes

import com.adoptu.common.Country
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Prefers CloudFront's CloudFront-Viewer-Country header (IP-based, only present when
// requests go through the app's CloudFront distribution - see infra/cloudfront.tf).
// Falls back to the region subtag of the browser's own locale (e.g. "es-MX" -> "MX"),
// passed by the client, for local dev or any request that bypasses CloudFront.
fun Route.countryRoutes() {
    get("/api/detect-country") {
        val viewerCountry = Country.fromIso2(call.request.header("CloudFront-Viewer-Country"))
        val country = viewerCountry ?: Country.fromIso2(regionFromLocale(call.request.queryParameters["locale"]))
        call.respond(mapOf("country" to country?.displayName))
    }
}

private fun regionFromLocale(locale: String?): String? {
    if (locale.isNullOrBlank()) return null
    val parts = locale.split('-')
    if (parts.size < 2) return null
    return parts.last()
}
