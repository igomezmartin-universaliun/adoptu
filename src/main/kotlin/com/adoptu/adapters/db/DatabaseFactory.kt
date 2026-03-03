package com.adoptu.adapters.db

import com.adoptu.models.*
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    val listOfTables = listOf(
        Users,
        WebAuthnCredentials,
        Pets,
        AdoptionRequests,
        PetImages)
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
        }
    }

}
