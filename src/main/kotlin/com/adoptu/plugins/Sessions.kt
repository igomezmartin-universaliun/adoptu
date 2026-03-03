package com.adoptu.plugins

import com.adoptu.auth.SessionUser
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.*

fun Application.configureSessions() {
    install(Sessions) {
        val secretHashKey = hex("0123456789012345678901234567890123456789012345678901234567890123")
        cookie<SessionUser>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400 * 7 // 7 days
            transform(SessionTransportTransformerMessageAuthentication(secretHashKey, "HmacSHA256"))
        }
    }
}
