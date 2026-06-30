package com.adoptu.plugins

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the real production route wiring in configureRouting() -- every other E2E test
 * mounts an individual route function directly, so this top-level wiring function (and its
 * /health endpoint) was otherwise never covered. Mirrors BaseE2ETest's H2 setup: DatabaseFactory.init()
 * creates its own schema from scratch, so no separate TestDatabase.initH2() call is needed here.
 */
class RoutingTest {

    private fun TestApplicationBuilder.setupApp() {
        val config = MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to "80",
            "db.test.postgres.driver" to "org.h2.Driver",
            "db.test.postgres.url" to "jdbc:h2:mem:routingtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "db.test.postgres.user" to "sa",
            "db.test.postgres.password" to "",
            "storage.test.bucket" to "test-bucket",
            "storage.test.region" to "us-east-1",
            "storage.test.endpoint" to "",
            "storage.test.path_style_access" to "false",
            "email.from" to "test@test.com",
            "admin.email" to "admin@adopt-u.com"
        )

        environment {
            this.config = config
        }

        application {
            com.adoptu.di.appModule(config).let { module ->
                install(org.koin.ktor.plugin.Koin) {
                    modules(module)
                }
            }
            com.adoptu.adapters.db.DatabaseFactory.init(config)
            configureSerialization()
            configureSessions()
            configureWebAuthn()
            configureRouting()
        }
    }

    @Test
    fun `GET health returns ok status`() {
        testApplication {
            setupApp()
            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"status\""))
            assertTrue(response.bodyAsText().contains("\"ok\""))
        }
    }

    @Test
    fun `GET unknown route returns 404`() {
        testApplication {
            setupApp()
            val response = client.get("/this-route-does-not-exist")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET index page is served through the full route tree`() {
        testApplication {
            setupApp()
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
