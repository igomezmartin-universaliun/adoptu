package com.adoptu.pages

import com.adoptu.mocks.TestDatabase
import com.adoptu.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*

@Execution(ExecutionMode.SAME_THREAD)
class PagesIntegrationTest {

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
        "storage.dev.access_key_id" to "test",
        "storage.dev.secret_access_key" to "test",
        "storage.dev.endpoint" to "http://localhost:4566",
        "storage.dev.path_style_access" to "true",
        "email.from" to "test@test.com"
    )

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
    }

    @Test
    fun `index page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.contentType()?.contentType == "text")
            assertTrue(response.bodyAsText().contains("Adopt-U"))
        }
    }

    @Test
    fun `login page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/login")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Login"))
        }
    }

    @Test
    fun `register page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/register")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Register"))
        }
    }

    @Test
    fun `pets page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/pets")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Pets"))
        }
    }

    @Test
    fun `pet detail page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/pet/1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Pet Details"))
        }
    }

    @Test
    fun `my-pets page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/my-pets")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("My Pets"))
        }
    }

    @Test
    fun `admin page returns HTML content`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val response = client.get("/admin")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Admin"))
        }
    }

    @Test
    fun `index page contains required elements`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val body = client.get("/").bodyAsText()
            assertTrue(body.contains("class=\"logo\""))
            assertTrue(body.contains("href=\"/pets\""))
            assertTrue(body.contains("hero"))
        }
    }

    @Test
    fun `pets page contains filter elements`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val body = client.get("/pets").bodyAsText()
            assertTrue(body.contains("filter-btn"))
            assertTrue(body.contains("pet-grid"))
        }
    }

    @Test
    fun `login page contains webauthn elements`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val body = client.get("/login").bodyAsText()
            assertTrue(body.contains("login-btn"))
            assertTrue(body.contains("webauthn"))
        }
    }

    @Test
    fun `register page contains form elements`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val body = client.get("/register").bodyAsText()
            assertTrue(body.contains("username"))
            assertTrue(body.contains("displayName"))
            assertTrue(body.contains("role"))
        }
    }

    @Test
    fun `my-pets page contains form elements`() {
        testApplication {
            environment {
                config = testAppConfig()
            }
            application {
                module()
            }

            val body = client.get("/my-pets").bodyAsText()
            assertTrue(body.contains("pet-form"))
            assertTrue(body.contains("add-btn"))
        }
    }
}
