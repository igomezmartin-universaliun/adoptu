package com.adoptu.mocks

import com.adoptu.models.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TestDatabase {
    private var initialized = false

    fun initH2() {
        if (initialized) {
            clearAllData()
            return
        }
        
        val db = Database.connect(
            url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(
                Users,
                WebAuthnCredentials,
                Pets,
                PetImages,
                AdoptionRequests
            )
        }
        initialized = true
    }

    fun clearAllData() {
        transaction {
            exec("DELETE FROM adoption_requests")
            exec("DELETE FROM pet_images")
            exec("DELETE FROM pets")
            exec("DELETE FROM webauthn_credentials")
            exec("DELETE FROM users")
        }
    }
}
