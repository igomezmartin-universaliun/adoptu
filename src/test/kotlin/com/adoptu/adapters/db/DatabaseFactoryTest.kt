package com.adoptu.adapters.db

import com.adoptu.models.Users
import com.adoptu.models.WebAuthnCredentials
import com.adoptu.models.Pets
import com.adoptu.models.AdoptionRequests
import com.adoptu.models.PetImages
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseFactoryTest {

    @Test
    fun `listOfTables contains all required tables`() {
        val tables = DatabaseFactory.listOfTables
        
        assertEquals(5, tables.size)
        assertTrue(tables.contains(Users))
        assertTrue(tables.contains(WebAuthnCredentials))
        assertTrue(tables.contains(Pets))
        assertTrue(tables.contains(AdoptionRequests))
        assertTrue(tables.contains(PetImages))
    }

    @Test
    fun `DatabaseFactory listOfTables is not null`() {
        val tables = DatabaseFactory.listOfTables
        assertNotNull(tables)
    }

    @Test
    fun `DatabaseFactory has 5 tables`() {
        assertEquals(5, DatabaseFactory.listOfTables.size)
    }

    @Test
    fun `Users table is in listOfTables`() {
        assertTrue(DatabaseFactory.listOfTables.contains(Users))
    }

    @Test
    fun `WebAuthnCredentials table is in listOfTables`() {
        assertTrue(DatabaseFactory.listOfTables.contains(WebAuthnCredentials))
    }

    @Test
    fun `Pets table is in listOfTables`() {
        assertTrue(DatabaseFactory.listOfTables.contains(Pets))
    }

    @Test
    fun `PetImages table is in listOfTables`() {
        assertTrue(DatabaseFactory.listOfTables.contains(PetImages))
    }

    @Test
    fun `AdoptionRequests table is in listOfTables`() {
        assertTrue(DatabaseFactory.listOfTables.contains(AdoptionRequests))
    }
}

class DatabaseIntegrationTest {

    @Test
    fun `can connect to in-memory H2 database`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:mocks;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        assertNotNull(db)
    }

    @Test
    fun `can create schema with all tables`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testschema;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(
                Users,
                WebAuthnCredentials,
                Pets,
                PetImages,
                AdoptionRequests
            )
        }
        
        // Schema created successfully
        assertTrue(true)
    }

    @Test
    fun `Users table can be created`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testusers;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(Users)
        }
        
        // Table created
        assertTrue(true)
    }

    @Test
    fun `Pets table can be created`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testpets;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(Users, Pets)
        }
        
        assertTrue(true)
    }

    @Test
    fun `PetImages table can be created with foreign key`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testimages;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(Users, Pets, PetImages)
        }
        
        assertTrue(true)
    }

    @Test
    fun `AdoptionRequests table can be created with foreign keys`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testadoption;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(Users, Pets, AdoptionRequests)
        }
        
        assertTrue(true)
    }

    @Test
    fun `WebAuthnCredentials table can be created with foreign key`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testwebauthn;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            SchemaUtils.create(Users, WebAuthnCredentials)
        }
        
        assertTrue(true)
    }

    @Test
    fun `all tables can be created in correct order`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testall;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.defaultDatabase = db
        
        transaction {
            // Create in correct order due to foreign keys
            SchemaUtils.create(Users)
            SchemaUtils.create(Pets)
            SchemaUtils.create(WebAuthnCredentials)
            SchemaUtils.create(PetImages)
            SchemaUtils.create(AdoptionRequests)
        }
        
        assertTrue(true)
    }

    @Test
    fun `Database connect accepts driver class name`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testdriver;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        assertNotNull(db)
    }

    @Test
    fun `Database connect accepts user credentials`() {
        val db = Database.connect(
            url = "jdbc:h2:mem:testcreds;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "testuser",
            password = "testpassword"
        )
        
        assertNotNull(db)
    }
}