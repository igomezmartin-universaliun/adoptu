package com.adoptu.routes

import com.adoptu.adapters.db.SterilizationLocations
import com.adoptu.adapters.db.repositories.SterilizationLocationRepository
import com.adoptu.dto.input.CreateSterilizationLocationRequest
import com.adoptu.dto.input.UpdateSterilizationLocationRequest
import com.adoptu.mocks.TestDatabase
import com.adoptu.plugins.configureSerialization
import com.adoptu.ports.SterilizationLocationRepositoryPort
import com.adoptu.services.SterilizationLocationService
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
class SterilizationLocationRoutesE2ETest {

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
            single<SterilizationLocationRepositoryPort> { SterilizationLocationRepository(get()) }
            single { SterilizationLocationService(get()) }
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
                sterilizationLocationRoutes()
                adminSterilizationLocationRoutes()
            }
        }
    }

    private fun createLocationInDb(
        name: String = "Vet Clinic",
        country: String = "United States",
        state: String? = "CA",
        city: String = "LA",
        neighborhood: String? = null,
        zip: String? = null
    ): Int {
        return transaction {
            val now = clock.now().toEpochMilliseconds()
            SterilizationLocations.insert {
                it[SterilizationLocations.name] = name
                it[SterilizationLocations.country] = com.adoptu.common.Country.fromDisplayName(country)!!
                it[SterilizationLocations.state] = state
                it[SterilizationLocations.city] = city
                it[SterilizationLocations.neighborhood] = neighborhood
                it[SterilizationLocations.zip] = zip
                it[SterilizationLocations.address] = "123 Main St"
                it[SterilizationLocations.createdAt] = now
                it[SterilizationLocations.updatedAt] = now
            } get SterilizationLocations.id
        }
    }

    // ==================== GET /api/sterilization-locations ====================

    @Test
    fun `GET sterilization-locations returns empty list when none exist`() {
        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }

    @Test
    fun `GET sterilization-locations returns all locations without filters`() {
        createLocationInDb(name = "Clinic A", country = "United States")
        createLocationInDb(name = "Clinic B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Clinic A"))
            assertTrue(body.contains("Clinic B"))
        }
    }

    @Test
    fun `GET sterilization-locations filters by country`() {
        createLocationInDb(name = "Clinic A", country = "United States")
        createLocationInDb(name = "Clinic B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations?country=United%20States")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Clinic A"))
            assertTrue(!body.contains("Clinic B"))
        }
    }

    @Test
    fun `GET sterilization-locations filters by all params together`() {
        createLocationInDb(name = "Clinic A", country = "United States", state = "CA", city = "LA", neighborhood = "Downtown", zip = "90001")
        createLocationInDb(name = "Clinic B", country = "United States", state = "CA", city = "LA", neighborhood = "Uptown", zip = "90002")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations?country=United%20States&state=CA&city=LA&neighborhood=Downtown&zip=90001")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Clinic A"))
            assertTrue(!body.contains("Clinic B"))
        }
    }

    // ==================== GET /api/sterilization-locations/grouped ====================

    @Test
    fun `GET sterilization-locations grouped returns grouped structure`() {
        createLocationInDb(name = "Clinic A", country = "United States", state = "CA", city = "LA")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/grouped")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("United States"))
            assertTrue(body.contains("Clinic A"))
        }
    }

    @Test
    fun `GET sterilization-locations grouped returns empty list when none exist`() {
        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/grouped")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }

    // ==================== GET /api/sterilization-locations/countries ====================

    @Test
    fun `GET sterilization-locations countries returns distinct countries`() {
        createLocationInDb(name = "Clinic A", country = "United States")
        createLocationInDb(name = "Clinic B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/countries")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("United States"))
            assertTrue(body.contains("Canada"))
        }
    }

    // ==================== GET /api/sterilization-locations/countries/{country}/states ====================

    @Test
    fun `GET sterilization-locations states returns states for country`() {
        createLocationInDb(name = "Clinic A", country = "United States", state = "CA")
        createLocationInDb(name = "Clinic B", country = "United States", state = "NY")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/countries/United%20States/states")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("CA"))
            assertTrue(body.contains("NY"))
        }
    }

    // ==================== GET /api/sterilization-locations/countries/{country}/states/{state}/cities ====================

    @Test
    fun `GET sterilization-locations cities returns cities for country and state`() {
        createLocationInDb(name = "Clinic A", country = "United States", state = "CA", city = "LA")
        createLocationInDb(name = "Clinic B", country = "United States", state = "CA", city = "SF")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/countries/United%20States/states/CA/cities")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("LA"))
            assertTrue(body.contains("SF"))
        }
    }

    // ==================== GET /api/sterilization-locations/{id} ====================

    @Test
    fun `GET sterilization-location by id returns location when it exists`() {
        val id = createLocationInDb(name = "Clinic A")

        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Clinic A"))
        }
    }

    @Test
    fun `GET sterilization-location by id returns 404 when not found`() {
        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("Sterilization location not found"))
        }
    }

    @Test
    fun `GET sterilization-location by id returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.get("/api/sterilization-locations/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid ID"))
        }
    }

    // ==================== GET /api/admin/sterilization-locations ====================

    @Test
    fun `GET admin sterilization-locations returns all locations`() {
        createLocationInDb(name = "Clinic A", country = "United States")

        testApplication {
            setupApp()
            val response = client.get("/api/admin/sterilization-locations")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Clinic A"))
        }
    }

    @Test
    fun `GET admin sterilization-locations filters by country`() {
        createLocationInDb(name = "Clinic A", country = "United States")
        createLocationInDb(name = "Clinic B", country = "Canada")

        testApplication {
            setupApp()
            val response = client.get("/api/admin/sterilization-locations?country=United%20States")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Clinic A"))
            assertTrue(!body.contains("Clinic B"))
        }
    }

    // ==================== GET /api/admin/sterilization-locations/{id} ====================

    @Test
    fun `GET admin sterilization-location by id returns location when it exists`() {
        val id = createLocationInDb(name = "Clinic A")

        testApplication {
            setupApp()
            val response = client.get("/api/admin/sterilization-locations/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Clinic A"))
        }
    }

    @Test
    fun `GET admin sterilization-location by id returns 404 when not found`() {
        testApplication {
            setupApp()
            val response = client.get("/api/admin/sterilization-locations/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `GET admin sterilization-location by id returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.get("/api/admin/sterilization-locations/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // ==================== POST /api/admin/sterilization-locations ====================

    @Test
    fun `POST admin sterilization-locations creates location`() {
        testApplication {
            setupApp()

            val request = CreateSterilizationLocationRequest(
                name = "New Clinic",
                country = "United States",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/admin/sterilization-locations") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateSterilizationLocationRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("New Clinic"))
        }
    }

    @Test
    fun `POST admin sterilization-locations returns 400 for blank name`() {
        testApplication {
            setupApp()

            val request = CreateSterilizationLocationRequest(
                name = "",
                country = "United States",
                city = "LA",
                address = "123 Main St"
            )

            val response = client.post("/api/admin/sterilization-locations") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(CreateSterilizationLocationRequest.serializer(), request))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Name is required"))
        }
    }

    // ==================== PUT /api/admin/sterilization-locations/{id} ====================

    @Test
    fun `PUT admin sterilization-location updates location when it exists`() {
        val id = createLocationInDb(name = "Old Name")

        testApplication {
            setupApp()

            val response = client.put("/api/admin/sterilization-locations/$id") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateSterilizationLocationRequest.serializer(), UpdateSterilizationLocationRequest(
                    name = "New Name",
                    country = "Canada",
                    state = "ON",
                    city = "Toronto",
                    neighborhood = "Downtown",
                    address = "456 Other St",
                    zip = "M5V 2T6",
                    phone = "555-1234",
                    email = "location@example.com",
                    website = "https://example.com",
                    description = "Updated description"
                )))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("New Name"))
            assertTrue(body.contains("Canada"))
            assertTrue(body.contains("Toronto"))
        }
    }

    @Test
    fun `PUT admin sterilization-location returns 404 when not found`() {
        testApplication {
            setupApp()

            val response = client.put("/api/admin/sterilization-locations/999") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateSterilizationLocationRequest.serializer(), UpdateSterilizationLocationRequest(name = "New Name")))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `PUT admin sterilization-location returns 400 for invalid id`() {
        testApplication {
            setupApp()

            val response = client.put("/api/admin/sterilization-locations/abc") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(UpdateSterilizationLocationRequest.serializer(), UpdateSterilizationLocationRequest(name = "New Name")))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // ==================== DELETE /api/admin/sterilization-locations/{id} ====================

    @Test
    fun `DELETE admin sterilization-location deletes location when it exists`() {
        val id = createLocationInDb(name = "To Delete")

        testApplication {
            setupApp()

            val response = client.delete("/api/admin/sterilization-locations/$id")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"success\": true"))

            val followUp = client.get("/api/admin/sterilization-locations/$id")
            assertEquals(HttpStatusCode.NotFound, followUp.status)
        }
    }

    @Test
    fun `DELETE admin sterilization-location returns 404 when not found`() {
        testApplication {
            setupApp()
            val response = client.delete("/api/admin/sterilization-locations/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `DELETE admin sterilization-location returns 400 for invalid id`() {
        testApplication {
            setupApp()
            val response = client.delete("/api/admin/sterilization-locations/abc")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
