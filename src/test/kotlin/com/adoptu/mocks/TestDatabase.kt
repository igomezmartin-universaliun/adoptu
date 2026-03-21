package com.adoptu.mocks

import com.adoptu.models.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TestDatabase {
    fun initH2() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.drop(
                Users,
                WebAuthnCredentials,
                Pets,
                PetImages,
                AdoptionRequests,
                PhotographyRequests,
                UserActiveRoles,
                Photographers,
                TemporalHomes,
                BlockedRescuers,
                TemporalHomeRequests
            )
            SchemaUtils.create(
                Users,
                WebAuthnCredentials,
                Pets,
                PetImages,
                AdoptionRequests,
                PhotographyRequests,
                UserActiveRoles,
                Photographers,
                TemporalHomes,
                BlockedRescuers,
                TemporalHomeRequests
            )
        }
    }

    fun clearAllData() {
        transaction {
            exec("DELETE FROM temporal_home_requests")
            exec("DELETE FROM blocked_rescuers")
            exec("DELETE FROM temporal_homes")
            exec("DELETE FROM adoption_requests")
            exec("DELETE FROM pet_images")
            exec("DELETE FROM pets")
            exec("DELETE FROM webauthn_credentials")
            exec("DELETE FROM photography_requests")
            exec("DELETE FROM user_active_roles")
            exec("DELETE FROM photographers")
            exec("DELETE FROM users")
        }
    }
}
