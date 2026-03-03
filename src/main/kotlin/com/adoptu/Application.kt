package com.adoptu

import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.di.appModule
import com.adoptu.plugins.configureRouting
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.plugins.configureWebAuthn
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    val configModule = module {
        single { config }
    }
    install(Koin) {
        slf4jLogger()
        modules(appModule, configModule)
    }
    DatabaseFactory.init(config)
    configureSerialization()
    configureSessions()
    configureWebAuthn()
    configureRouting()
}
