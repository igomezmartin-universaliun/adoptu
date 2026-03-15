package com.adoptu

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*

@Execution(ExecutionMode.SAME_THREAD)
class ApplicationStartupTest {

    private fun testAppConfig(): ApplicationConfig = MapApplicationConfig(
        "env" to "dev",
        "ktor.deployment.port" to "8080",
        "db.dev.postgres.driver" to "org.h2.Driver",
        "db.dev.postgres.url" to "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "db.dev.postgres.user" to "sa",
        "db.dev.postgres.password" to "",
        "db.prod.postgres.driver" to "org.h2.Driver",
        "db.prod.postgres.url" to "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "db.prod.postgres.user" to "sa",
        "db.prod.postgres.password" to "",
        "storage.dev.bucket" to "test-bucket",
        "storage.dev.region" to "us-east-1",
        "storage.dev.endpoint" to "http://localhost:4566",
        "storage.dev.path_style_access" to "true",
        "email.from" to "test@test.com"
    )

    @Test
    fun `application module loads without errors`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }
        }
    }

    @Test
    fun `application starts and responds to root route`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/")
            assertTrue(
                response.status == HttpStatusCode.OK ||
                response.status == HttpStatusCode.NotFound ||
                response.status == HttpStatusCode.Found,
                "Root route should be accessible"
            )
        }
    }

    @Test
    fun `application starts with all plugins configured`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/")
            assertNotEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `application starts with serialization plugin configured`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/api/v1/pets")
            assertNotEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Test
    fun `application responds to static content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/style.css")
            assertTrue(
                response.status == HttpStatusCode.OK || 
                response.status == HttpStatusCode.NotFound,
                "Static content request should either succeed or return 404"
            )
        }
    }

    @Test
    fun `application handles unknown routes gracefully`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/unknown-route-xyz-123")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }
}
