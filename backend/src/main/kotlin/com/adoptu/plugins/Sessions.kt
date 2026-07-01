package com.adoptu.plugins

import com.adoptu.services.auth.SessionUser
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSessions() {
    install(Sessions) {
        val secretHashKey = "0123456789012345678901234567890123456789012345678901234567890123".hexToByteArray()
        cookie<SessionUser>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400 * 7 // 7 days
            transform(SessionTransportTransformerMessageAuthentication(secretHashKey, "HmacSHA256"))
        }
    }
}
