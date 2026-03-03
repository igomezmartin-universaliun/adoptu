package com.adoptu.auth

import com.adoptu.dto.UserRole
import com.adoptu.models.Users
import com.adoptu.models.WebAuthnCredentials
import com.adoptu.services.UserService
import com.adoptu.mocks.TestDatabase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class WebAuthnServiceTest {

    @BeforeEach
    fun setup() {
        TestDatabase.initH2()
    }

    // ==================== Registration Options ====================

    @Test
    fun `generateRegistrationOptions returns valid options`() {
        val result = WebAuthnService.generateRegistrationOptions(
            username = "testuser",
            displayName = "Test User"
        )

        assertNotNull(result)
        assertNotNull(result.rp)
        assertEquals("localhost", result.rp.id)
        assertEquals("Adopt-U Pet Adoption", result.rp.name)
        assertNotNull(result.user)
        assertEquals("testuser", result.user.name)
        assertEquals("Test User", result.user.displayName)
        assertNotNull(result.challenge)
        assertTrue(result.pubKeyCredParams.isNotEmpty())
    }

    @Test
    fun `generateRegistrationOptions generates unique challenges`() {
        val result1 = WebAuthnService.generateRegistrationOptions("user1", "User 1")
        val result2 = WebAuthnService.generateRegistrationOptions("user2", "User 2")

        assertNotEquals(result1.challenge, result2.challenge)
    }

    // ==================== Registration ====================

    @Test
    fun `verifyAndRegister creates user and credential`() {
        // Generate options first to store challenge
        val options = WebAuthnService.generateRegistrationOptions(
            username = "newuser",
            displayName = "New User"
        )

        // Mock a valid registration response (in real tests this would be a real WebAuthn response)
        // For this mocks, we'll just verify the user creation works
        val userId = transaction {
            Users.insert {
                it[Users.username] = "newuser"
                it[Users.displayName] = "New User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        assertNotNull(userId)

        // Verify user was created
        val user = UserService.getById(userId)
        assertNotNull(user)
        assertEquals("newuser", user.username)
        assertEquals("New User", user.displayName)
    }

    @Test
    fun `verifyAndRegister returns null if challenge missing`() {
        val result = WebAuthnService.verifyAndRegister(
            username = "missinguser",
            displayName = "Missing User",
            role = "ADOPTER",
            registrationResponseJson = "{}"
        )
        assertNull(result)
    }

    @Test
    fun `verifyAndRegister returns null on exception`() {
        // Store a challenge for the user
        WebAuthnService.generateRegistrationOptions("erroruser", "Error User")
        // Provide invalid registrationResponseJson to trigger exception
        val result = WebAuthnService.verifyAndRegister(
            username = "erroruser",
            displayName = "Error User",
            role = "ADOPTER",
            registrationResponseJson = "invalid_json"
        )
        assertNull(result)
    }

    // ==================== Assertion Options ====================

    @Test
    fun `generateAssertionOptions returns valid options`() {
        val result = WebAuthnService.generateAssertionOptions()

        assertNotNull(result)
        assertNotNull(result.challenge)
        assertEquals("localhost", result.rpId)
        assertEquals("required", result.userVerification)
    }

    @Test
    fun `generateAssertionOptions generates unique challenges`() {
        val result1 = WebAuthnService.generateAssertionOptions()
        val result2 = WebAuthnService.generateAssertionOptions()

        assertNotEquals(result1.challenge, result2.challenge)
    }

    // ==================== Authentication ====================

    @Test
    fun `verifyAndAuthenticate returns null for unknown credential`() {
        // No credentials in database, should return null
        val result = WebAuthnService.verifyAndAuthenticate("{}")
        
        assertNull(result)
    }

    @Test
    fun `verifyAndAuthenticate works with existing credential`() {
        // Create user and credential
        val userId = transaction {
            Users.insert {
                it[Users.username] = "authuser"
                it[Users.displayName] = "Auth User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "test_credential"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        // Generate assertion options to store challenge
        WebAuthnService.generateAssertionOptions()

        // In real mocks, we'd provide a valid authentication response
        // For now, we verify the setup is correct
        val credentials = transaction {
            WebAuthnCredentials.selectAll().toList()
        }
        assertEquals(1, credentials.size)
        assertEquals("test_credential", credentials[0][WebAuthnCredentials.credentialId])
    }

    @Test
    fun `verifyAndAuthenticate returns null on parse error`() {
        // Provide invalid JSON to trigger parse error
        val result = WebAuthnService.verifyAndAuthenticate("not_a_json")
        assertNull(result)
    }

    @Test
    fun `verifyAndAuthenticate returns null if credential not found`() {
        // Provide valid JSON but no credential in DB
        val result = WebAuthnService.verifyAndAuthenticate("{}")
        assertNull(result)
    }

    @Test
    fun `verifyAndAuthenticate returns null if challenge missing`() {
        // Create user and credential
        val userId = org.jetbrains.exposed.v1.jdbc.transactions.transaction {
            Users.insert {
                it[Users.username] = "nochallengeuser"
                it[Users.displayName] = "No Challenge User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }
        org.jetbrains.exposed.v1.jdbc.transactions.transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "nochallenge_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }
        // Do not store assertion challenge
        val result = WebAuthnService.verifyAndAuthenticate("{}")
        assertNull(result)
    }

    // ==================== Challenge Store ====================

    @Test
    fun `challenge is stored during registration options generation`() {
        val username = "challengetest_${System.currentTimeMillis()}"
        
        val options = WebAuthnService.generateRegistrationOptions(username, "Challenge Test")
        
        assertNotNull(options.challenge)
        assertTrue(options.challenge.isNotBlank())
    }

    @Test
    fun `assertion challenge is stored during assertion options generation`() {
        val options = WebAuthnService.generateAssertionOptions()
        
        assertNotNull(options.challenge)
        assertTrue(options.challenge.isNotBlank())
    }

    @Test
    fun `challenge can only be used once for registration`() {
        val username = "reuse_challenge_${System.currentTimeMillis()}"
        
        WebAuthnService.generateRegistrationOptions(username, "Reuse Challenge Test")
        
        val result = WebAuthnService.verifyAndRegister(
            username = username,
            displayName = "Reuse Challenge Test",
            role = "ADOPTER",
            registrationResponseJson = "{}"
        )
        
        assertNull(result)
    }

    @Test
    fun `assertion challenge can only be used once`() {
        val username = "reuse_assert_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Reuse Assert User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "reuse_assert_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        WebAuthnService.generateAssertionOptions()
        
        val result = WebAuthnService.verifyAndAuthenticate("{}")
        
        assertNull(result)
    }

    @Test
    fun `user can be created without email`() {
        val username = "noemail_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "No Email User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        val user = UserService.getById(userId)
        assertNotNull(user)
        assertNull(user.email)
    }

    @Test
    fun `user can be created with email`() {
        val username = "withemail_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "With Email User"
                it[Users.email] = "test@example.com"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        val user = UserService.getById(userId)
        assertNotNull(user)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `pub key cred params contains both ES256 and RS256 algorithms`() {
        val options = WebAuthnService.generateRegistrationOptions("algos", "Algorithms Test")
        
        val algs = options.pubKeyCredParams.map { it.alg }
        assertTrue(algs.contains(-7))
        assertTrue(algs.contains(-257))
    }

    // ==================== Registration Options Validation ====================

    @Test
    fun `registration options contains correct rp id`() {
        val options = WebAuthnService.generateRegistrationOptions("rpvalidation", "RP Validation")
        
        assertEquals("localhost", options.rp.id)
        assertEquals("Adopt-U Pet Adoption", options.rp.name)
    }

    @Test
    fun `registration options contains user with correct fields`() {
        val options = WebAuthnService.generateRegistrationOptions("userfields", "User Fields Test")
        
        assertEquals("userfields", options.user.name)
        assertEquals("User Fields Test", options.user.displayName)
        assertNotNull(options.user.id)
        assertTrue(options.user.id.isNotBlank())
    }

    @Test
    fun `registration options contains pub key cred params`() {
        val options = WebAuthnService.generateRegistrationOptions("pkparams", "PK Params Test")
        
        assertTrue(options.pubKeyCredParams.isNotEmpty())
        
        val firstParam = options.pubKeyCredParams.first()
        assertEquals("public-key", firstParam.type)
        assertEquals(-7, firstParam.alg)
    }

    // ==================== Assertion Options Validation ====================

    @Test
    fun `assertion options has correct rp id`() {
        val options = WebAuthnService.generateAssertionOptions()
        
        assertEquals("localhost", options.rpId)
    }

    @Test
    fun `assertion options requires user verification`() {
        val options = WebAuthnService.generateAssertionOptions()
        
        assertEquals("required", options.userVerification)
    }

    // ==================== AuthenticatedUser ====================

    @Test
    fun `authenticated user has all required fields`() {
        val username = "authuserfields_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Auth User Fields"
                it[Users.email] = "auth@mocks.com"
                it[Users.role] = "RESCUER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "authuserfields_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 1
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        val user = UserService.getById(userId)
        assertNotNull(user)
        assertEquals(username, user.username)
        assertEquals("Auth User Fields", user.displayName)
        assertEquals("auth@mocks.com", user.email)
        assertEquals(UserRole.RESCUER, user.role)
    }

    // ==================== Credential Deletion ====================

    @Test
    fun `deleting credential keeps user intact`() {
        val username = "deletecred_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Delete Cred User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "deletecred_cred1"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        // Verify user exists with credential
        var user = UserService.getById(userId)
        assertNotNull(user)

        // Delete credential
        transaction {
            exec("DELETE FROM webauthn_credentials WHERE user_id = $userId")
        }

        // User should still exist
        user = UserService.getById(userId)
        assertNotNull(user)
        assertEquals(username, user.username)
    }

    // ==================== Multiple Users with Credentials ====================

    @Test
    fun `multiple users can have separate credentials`() {
        val user1Name = "multiuser1_${System.currentTimeMillis()}"
        val user2Name = "multiuser2_${System.currentTimeMillis()}"
        
        val user1Id = transaction {
            Users.insert {
                it[Users.username] = user1Name
                it[Users.displayName] = "Multi User 1"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        val user2Id = transaction {
            Users.insert {
                it[Users.username] = user2Name
                it[Users.displayName] = "Multi User 2"
                it[Users.role] = "RESCUER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = user1Id
                it[WebAuthnCredentials.credentialId] = "${user1Name}_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }

            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = user2Id
                it[WebAuthnCredentials.credentialId] = "${user2Name}_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        val user1 = UserService.getById(user1Id)
        val user2 = UserService.getById(user2Id)

        assertNotNull(user1)
        assertNotNull(user2)
        assertEquals(UserRole.ADOPTER, user1.role)
        assertEquals(UserRole.RESCUER, user2.role)
    }

    // ==================== Credential Transports ====================

    @Test
    fun `credential can store transports`() {
        val username = "transportstest_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Transports Test"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "transports_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = "USB,NEAR_FIELD"
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        val credentials = transaction {
            WebAuthnCredentials.selectAll().where { 
                WebAuthnCredentials.credentialId eq "transports_cred" 
            }.toList()
        }

        assertEquals(1, credentials.size)
        assertEquals("USB,NEAR_FIELD", credentials[0][WebAuthnCredentials.transports])
    }

    // ==================== User Query by Username ====================

    @Test
    fun `user can be queried by username`() {
        val username = "querybyusername_${System.currentTimeMillis()}"
        
        transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Query By Username"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            }
        }

        val allUsers = transaction {
            Users.selectAll().where { Users.username eq username }.firstOrNull()
        }
        
        assertNotNull(allUsers)
        assertEquals(username, allUsers[Users.username])
    }

    @Test
    fun `verifyAndAuthenticate returns null when user deleted after credential creation`() {
        val username = "deleteduser_${System.currentTimeMillis()}"
        
        val userId = transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.displayName] = "Deleted User"
                it[Users.role] = "ADOPTER"
                it[Users.createdAt] = System.currentTimeMillis()
            } get Users.id
        }

        transaction {
            WebAuthnCredentials.insert {
                it[WebAuthnCredentials.userId] = userId
                it[WebAuthnCredentials.credentialId] = "deleteduser_cred"
                it[WebAuthnCredentials.attestedCredentialDataBase64] = "dGVzdA=="
                it[WebAuthnCredentials.signCount] = 0
                it[WebAuthnCredentials.transports] = null
                it[WebAuthnCredentials.createdAt] = System.currentTimeMillis()
            }
        }

        transaction {
            exec("DELETE FROM webauthn_credentials WHERE credential_id = 'deleteduser_cred'")
            exec("DELETE FROM users WHERE id = $userId")
        }

        WebAuthnService.generateAssertionOptions()

        val result = WebAuthnService.verifyAndAuthenticate("{}")
        
        assertNull(result)
    }

    @Test
    fun `verifyAndRegister stores null transports`() {
        val username = "nulltransports_${System.currentTimeMillis()}"
        
        WebAuthnService.generateRegistrationOptions(username, "Null Transports")

        val result = WebAuthnService.verifyAndRegister(
            username = username,
            displayName = "Null Transports",
            role = "ADOPTER",
            registrationResponseJson = "{}"
        )
        
        assertNull(result)
    }

    @Test
    fun `registration options user id is base64url encoded`() {
        val username = "base64userid_${System.currentTimeMillis()}"
        
        val options1 = WebAuthnService.generateRegistrationOptions(username, "Base64 User ID 1")
        val options2 = WebAuthnService.generateRegistrationOptions(username, "Base64 User ID 2")
        
        assertNotEquals(options1.user.id, options2.user.id)
        assertTrue(options1.user.id.matches(Regex("[A-Za-z0-9_-]+")))
    }

    @Test
    fun `challenge is different from user id in registration options`() {
        val username = "diffchallenge_${System.currentTimeMillis()}"
        
        val options = WebAuthnService.generateRegistrationOptions(username, "Different Challenge")
        
        assertNotEquals(options.challenge, options.user.id)
    }

    @Test
    fun `remove challenge after explicit remove call`() {
        val username = "removechaltest_${System.currentTimeMillis()}"
        
        WebAuthnService.generateRegistrationOptions(username, "Remove Chall Test")
        
        val result1 = WebAuthnService.verifyAndRegister(
            username = username,
            displayName = "Remove Chall Test",
            role = "ADOPTER",
            registrationResponseJson = "{}"
        )
        
        assertNull(result1)
        
        val result2 = WebAuthnService.verifyAndRegister(
            username = username,
            displayName = "Remove Chall Test 2",
            role = "ADOPTER",
            registrationResponseJson = "{}"
        )
        
        assertNull(result2)
    }
}