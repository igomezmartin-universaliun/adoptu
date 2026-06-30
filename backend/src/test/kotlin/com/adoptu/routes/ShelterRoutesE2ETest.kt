package com.adoptu.routes

import com.adoptu.adapters.db.AnimalShelters
import com.adoptu.adapters.db.repositories.ShelterRepository
import com.adoptu.dto.input.CreateShelterRequest
import com.adoptu.dto.input.UpdateShelterRequest
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.configureSerialization
import com.adoptu.ports.ShelterRepositoryPort
import com.adoptu.services.ShelterService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ShelterRoutesE2ETest {

    private val clock = Clock.System

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
        TestDatabase.clearAllData()
    }

    private fun TestApplicationBuilder.setupApp() {
        val config = MapApplicationConfig(
            "env" to "test",
            "ktor.deployment.port" to "80"
        )

        val testModules = module {
            single<kotlin.time.Clock> { kotlin.time.Clock.System }
            single<ShelterRepositoryPort> { ShelterRepository(get()) }
            single { ShelterService(get()) }
        }

        environment {
            this.config = config
        }

        application {
            install(Koin) {
                modules(testModules)
            }
            configureSerialization()
            routing {
                shelterRoutes()
                adminShelterRoutes()
            }
        }
    }

    private fun createShelterInDb(
        name: String = "Animal Rescue",
        country: String = "USA",
        state: String? = "CA",
        city: String = "LA",
        neighborhood: String? = null,
        zip: String? = null
    ): Int {
        return transaction {
            val now = clock.now().toEpochMilliseconds()
            AnimalShelters.insert {
                it[AnimalShelters.name] = name
                it[AnimalShelters.country] = country
                it[AnimalShelters.state] = state
                it[AnimalShelters.city] = city
                it[AnimalShelters.neighborhood] = neighborhood
                it[AnimalShelters.zip] = zip
                it[AnimalShelters.address] = "123 Main St"
                it[AnimalShelters.currency] = "USD"
                it[AnimalShelters.createdAt] = now
                it[AnimalShelters.updatedAt] = now
            } get AnimalShelters.id
        }
    }

    // ==================== GET /api/shelters ====================

    @Test
    fun `GET shelters returns 400 when country is missing`() {
        testApplication {
            setupApp()
            val response = client.get("/api/shelters")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Country is required"))
        }
    }

    @Test
    fun `GET shelters returns 400 when country is blank`() {
        testApplication {
            setupApp()
            val response = client.get("/api/shelters?country=")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `GET shelters returns matching shelters`() {
        createShelterInDb(name = "Shelter A", country = "USA")
        createShelterInDb(name = "Shelter B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/shelters?country=USA")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Shelter A"))
            assertTrue(!body.contains("Shelter B"))
        }
    }

    @Test
    fun `GET shelters filters by state city and zip`() {
        createShelterInDb(name = "Shelter A", country = "USA", state = "CA", city = "LA", zip = "90001")
        createShelterInDb(name = "Shelter B", country = "USA", state = "NY", city = "NYC", zip = "10001")

        testApplication {
            setupApp()
            val response = client.get("/api/shelters?country=USA&state=CA&city=LA&zip=90001")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Shelter A"))
            assertTrue(!body.contains("Shelter B"))
        }
    }

    // ==================== GET /api/shelters/countries ====================

    @Test
    fun `GET shelters countries returns distinct countries`() {
        createShelterInDb(name = "Shelter A", country = "USA")
        createShelterInDb(name = "Shelter B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/shelters/countries")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("USA"))
            assertTrue(body.contains("Canada"))
        }
    }

    // ==================== GET /api/shelters/countries/{country}/states ====================

    @Test
    fun `GET shelters states returns states for country`() {
        createShelterInDb(name = "Shelter A", country = "USA", state = "CA")
        createShelterInDb(name = "Shelter B", country = "USA", state = "NY")

        testApplication {
            setupApp()
            val response = client.get("/api/shelters/countries/USA/states")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("CA"))
            assertTrue(body.contains("NY"))
        }
    }

    // ==================== GET /api/shelters/{id} ====================

    @Test
    fun `GET shelter by id returns shelter when it exists`() {
        val id = createShelterInDb(name = "Shelter A")

        testApplication {
            setupApp()
            val response = client.get("/api/shelters/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Shelter A"))
        }
    }

    @Test
    fun `GET shelter by id returns 404 when not found`() {
        testApplication {
            setupApp()
            val response = client.get("/api/shelters/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("Shelter not found"))
        }
    }

    @Test
    fun `GET shelter by id returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.get("/api/shelters/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid ID"))
        }
    }

    // ==================== GET /api/admin/shelters ====================

    @Test
    fun `GET admin shelters returns empty list when country is missing`() {
        createShelterInDb(name = "Shelter A", country = "USA")

        testApplication {
            setupApp()
            val response = client.get("/api/admin/shelters")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }

    @Test
    fun `GET admin shelters returns matching shelters when country provided`() {
        createShelterInDb(name = "Shelter A", country = "USA")
        createShelterInDb(name = "Shelter B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/admin/shelters?country=USA")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Shelter A"))
            assertTrue(!body.contains("Shelter B"))
        }
    }

    // ==================== GET /api/admin/shelters/{id} ====================

    @Test
    fun `GET admin shelter by id returns shelter when it exists`() {
        val id = createShelterInDb(name = "Shelter A")

        testApplication {
            setupApp()
            val response = client.get("/api/admin/shelters/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Shelter A"))
        }
    }

    @Test
    fun `GET admin shelter by id returns 404 when not found`() {
        testApplication {
            setupApp()
            val response = client.get("/api/admin/shelters/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET admin shelter by id returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.get("/api/admin/shelters/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // ==================== POST /api/admin/shelters ====================

    @Test
    fun `POST admin shelters creates shelter`() {
        testApplication {
            setupApp()

            val request = CreateShelterRequest(
                name = "New Shelter",
                country = "USA",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/admin/shelters") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateShelterRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("New Shelter"))
        }
    }

    @Test
    fun `POST admin shelters returns 400 for blank name`() {
        testApplication {
            setupApp()

            val request = CreateShelterRequest(
                name = "",
                country = "USA",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/admin/shelters") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateShelterRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Name is required"))
        }
    }

    // ==================== PUT /api/admin/shelters/{id} ====================

    @Test
    fun `PUT admin shelter updates shelter when it exists`() {
        val id = createShelterInDb(name = "Old Name")

        testApplication {
            setupApp()

            val response = client.put("/api/admin/shelters/$id") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateShelterRequest.serializer(), UpdateShelterRequest(name = "New Name")))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("New Name"))
        }
    }

    @Test
    fun `PUT admin shelter returns 404 when not found`() {
        testApplication {
            setupApp()

            val response = client.put("/api/admin/shelters/999") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateShelterRequest.serializer(), UpdateShelterRequest(name = "New Name")))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT admin shelter returns 400 for invalid id`() {
        testApplication {
            setupApp()

            val response = client.put("/api/admin/shelters/abc") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateShelterRequest.serializer(), UpdateShelterRequest(name = "New Name")))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // ==================== DELETE /api/admin/shelters/{id} ====================

    @Test
    fun `DELETE admin shelter deletes shelter when it exists`() {
        val id = createShelterInDb(name = "To Delete")

        testApplication {
            setupApp()

            val response = client.delete("/api/admin/shelters/$id")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"success\": true"))

            val followUp = client.get("/api/admin/shelters/$id")
            assertEquals(HttpStatusCode.NotFound, followUp.status)
        }
    }

    @Test
    fun `DELETE admin shelter returns 404 when not found`() {
        testApplication {
            setupApp()
            val response = client.delete("/api/admin/shelters/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE admin shelter returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.delete("/api/admin/shelters/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
