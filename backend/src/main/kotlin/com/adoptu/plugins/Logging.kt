package com.adoptu.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            val path = call.request.path()
            // Skip noisy health checks and static assets
            path != "/health" && !path.startsWith("/static") && !path.startsWith("/css") && !path.startsWith("/js")
        }
        format { call ->
            val status = call.response.status()
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            "$method $path → $status (${duration}ms)"
        }
    }
}
