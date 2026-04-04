package com.adoptu.e2e

import com.adoptu.adapters.db.DatabaseFactory
import com.adoptu.di.appModule
import com.adoptu.plugins.configureRouting
import com.adoptu.plugins.configureSerialization
import com.adoptu.plugins.configureSessions
import com.adoptu.plugins.configureWebAuthn
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.ktor.server.application.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseE2ETest {

    companion object {
        private val started = AtomicBoolean(false)
        private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
        private var serverPort = 0
        
        protected fun getBaseUrl(): String = "http://localhost:$serverPort"
    }

    protected lateinit var playwright: Playwright
    protected lateinit var browser: Browser
    protected lateinit var context: BrowserContext
    protected lateinit var page: Page

    protected val jsErrors = mutableListOf<String>()
    protected val consoleErrors = mutableListOf<String>()

    @BeforeAll
    fun setupServer() {
        if (!started.getAndSet(true)) {
            startTestServer()
        }
    }

    private fun startTestServer() {
        serverPort = findFreePort()

        val config = MapApplicationConfig(
            "env" to "test",
            "db.test.postgres.driver" to "org.h2.Driver",
            "db.test.postgres.url" to "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "db.test.postgres.user" to "sa",
            "db.test.postgres.password" to "",
            "storage.test.bucket" to "test-bucket",
            "storage.test.region" to "us-east-1",
            "storage.test.endpoint" to "",
            "storage.test.path_style_access" to "false",
            "email.from" to "test@test.com",
            "admin.email" to "admin@adopt-u.com"
        )

        server = embeddedServer(Netty, port = serverPort) {
            install(Koin) {
                slf4jLogger()
                modules(appModule(config))
            }
            DatabaseFactory.init(config)
            configureSerialization()
            configureSessions()
            configureWebAuthn()
            configureRouting()
        }

        server!!.start()
    }

    private fun findFreePort(): Int {
        return ServerSocket(0).use { socket -> socket.localPort }
    }

    @BeforeAll
    fun setupBrowser() {
        playwright = Playwright.create()
        val options = BrowserType.LaunchOptions().apply {
            headless = true
            args = listOf("--no-sandbox", "--disable-setuid-sandbox")
        }
        browser = playwright.chromium().launch(options)
        context = browser.newContext()
        page = context.newPage()

        page.onPageError { jsErrors.add(it) }
        page.onConsoleMessage { msg ->
            if (msg.type() == "error") {
                consoleErrors.add(msg.text())
            }
        }
    }

    @AfterAll
    fun teardownBrowser() {
        page.close()
        context.close()
        browser.close()
        playwright.close()
        server?.stop(1000, 2000)
    }

    protected fun clearErrors() {
        jsErrors.clear()
        consoleErrors.clear()
    }

    protected fun navigateTo(path: String) {
        clearErrors()
        page.navigate("${getBaseUrl()}$path")
        page.waitForLoadState()
    }

    protected fun assertNoJsErrors() {
        assert(jsErrors.isEmpty()) {
            "JavaScript errors detected: ${jsErrors.joinToString(", ")}"
        }
    }

    protected fun assertNoConsoleErrors() {
        assert(consoleErrors.isEmpty()) {
            "Console errors detected: ${consoleErrors.joinToString(", ")}"
        }
    }

    protected fun assertNoErrors() {
        assertNoJsErrors()
        assertNoConsoleErrors()
    }
}
