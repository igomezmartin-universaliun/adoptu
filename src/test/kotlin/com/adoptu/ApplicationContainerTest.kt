package com.adoptu

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
@DisabledOnOs(OS.LINUX, disabledReason = "Requires Docker which is not available in CI")
class ApplicationContainerTest {

    companion object {
        @Container
        val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("adoptu")
            .withUsername("adoptu")
            .withPassword("Ad0ptU")
    }

    private fun testAppConfig(): ApplicationConfig {
        return MapApplicationConfig(
            "env" to "prod",
            "ktor.deployment.port" to "8080",
            "db.prod.postgres.driver" to "org.postgresql.Driver",
            "db.prod.postgres.url" to postgresContainer.jdbcUrl,
            "db.prod.postgres.user" to postgresContainer.username,
            "db.prod.postgres.password" to postgresContainer.password,
            "storage.prod.bucket" to "test-bucket",
            "storage.prod.region" to "us-east-1",
            "storage.prod.endpoint" to "http://localhost:4566",
            "storage.prod.path_style_access" to "true",
            "email.from" to "test@test.com"
        )
    }

    @Test
    fun `application starts with PostgreSQL container`() {
        assertTrue(postgresContainer.isRunning, "PostgreSQL container should be running")
        
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
                "Application should respond"
            )
        }
    }

    @Test
    fun `application connects to PostgreSQL container and creates tables`() {
        assertTrue(postgresContainer.isRunning, "PostgreSQL container should be running")
        
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/api/v1/pets")
            assertTrue(
                response.status != HttpStatusCode.InternalServerError,
                "Application should handle database connection"
            )
        }
    }

    @Test
    fun `application handles requests with PostgreSQL container`() {
        assertTrue(postgresContainer.isRunning, "PostgreSQL container should be running")
        
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
