package com.adoptu.adapters.db

import com.adoptu.dto.input.UserRole
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object DatabaseFactory {
    private val clock: Clock = Clock.System
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    val listOfTables = listOf(
        Users,
        EmailVerificationTokens,
        EmailVerificationAttempts,
        UserActiveRoles,
        Photographers,
        WebAuthnCredentials,
        Pets,
        PetImages,
        AdoptionRequests,
        PhotographyRequests,
        TemporalHomes,
        BlockedRescuers,
        TemporalHomeRequests,
        AnimalShelters,
        SterilizationLocations)
    fun init(config: ApplicationConfig) {
        val env = config.propertyOrNull("env")?.getString() ?: "prod"
        val prefix = "db.$env"

        val driverClassName = config.property("$prefix.postgres.driver").getString()
        val jdbcURL = config.property("$prefix.postgres.url").getString()
        val user = config.property("$prefix.postgres.user").getString()
        val password = config.property("$prefix.postgres.password").getString()

        Database.connect(jdbcURL, driverClassName, user = user, password = password)

        transaction {
            SchemaUtils.create(*listOfTables.toTypedArray())
            SchemaUtils.addMissingColumnsStatements(*listOfTables.toTypedArray())
                .forEach { exec(it)}
            MigrationUtils.dropUnmappedColumnsStatements(*listOfTables.toTypedArray())
        }

        createDefaultAdmin(config)
    }

    private fun createDefaultAdmin(config: ApplicationConfig) {
        val adminEmail = config.propertyOrNull("admin.email")?.getString() ?: "adopt-u@adopt-u.org"

        transaction {
            val existingAdmin = Users.selectAll().where { Users.username.eq(adminEmail) }.firstOrNull()
            if (existingAdmin == null) {
                val userId = Users.insert {
                    it[username] = adminEmail
                    it[displayName] = "Admin"
                    it[createdAt] = clock.now().toEpochMilliseconds()
                } get Users.id
                
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = userId
                    it[UserActiveRoles.role] = UserRole.ADMIN.name
                }
                
                logger.info("Default admin user created: $adminEmail")
            }
        }
    }
}
