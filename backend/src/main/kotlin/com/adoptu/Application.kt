package com.adoptu

import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.di.appModule
import com.adoptu.plugins.configureRouting
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.plugins.configureWebAuthn
import com.adoptu.services.crypto.CryptoService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AdoptU")

fun main(args: Array<String>) {
    val env = System.getenv("ADOPTU_ENV") ?: "prod"
    logger.info("Starting Adopt-U application in $env environment")
    EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(appModule(config))
    }
    DatabaseFactory.init(config)
    CryptoService.initialize()
    configureSerialization()
    configureSessions()
    configureWebAuthn()
    configureRouting()
}
