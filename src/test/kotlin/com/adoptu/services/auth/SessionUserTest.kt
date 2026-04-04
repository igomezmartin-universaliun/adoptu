package com.adoptu.services.auth

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionUserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Nested
    inner class Serialization {
        @Test
        fun `SessionUser serializes to JSON correctly`() {
            val sessionUser = SessionUser(
                userId = 1,
                email = "test@example.com",
                displayName = "Test User"
            )

            val jsonString = json.encodeToString(sessionUser)

            assertTrue(jsonString.contains("\"userId\":1"))
            assertTrue(jsonString.contains("\"email\":\"test@example.com\""))
            assertTrue(jsonString.contains("\"displayName\":\"Test User\""))
        }

        @Test
        fun `SessionUser deserializes from JSON correctly`() {
            val jsonString = """
                {
                    "userId": 42,
                    "email": "user@example.com",
                    "displayName": "Jane Doe"
                }
            """.trimIndent()

            val sessionUser = json.decodeFromString<SessionUser>(jsonString)

            assertEquals(42, sessionUser.userId)
            assertEquals("user@example.com", sessionUser.email)
            assertEquals("Jane Doe", sessionUser.displayName)
        }

        @Test
        fun `SessionUser round-trip serialization works`() {
            val original = SessionUser(
                userId = 123,
                email = "roundtrip@test.com",
                displayName = "Round Trip User"
            )

            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<SessionUser>(jsonString)

            assertEquals(original.userId, deserialized.userId)
            assertEquals(original.email, deserialized.email)
            assertEquals(original.displayName, deserialized.displayName)
        }
    }

    @Nested
    inner class DataClassBehavior {
        @Test
        fun `SessionUser equality works correctly`() {
            val sessionUser1 = SessionUser(userId = 1, email = "test@test.com", displayName = "Test")
            val sessionUser2 = SessionUser(userId = 1, email = "test@test.com", displayName = "Test")
            val sessionUser3 = SessionUser(userId = 2, email = "test@test.com", displayName = "Test")

            assertEquals(sessionUser1, sessionUser2)
            assertTrue(sessionUser1 == sessionUser2)
            assertTrue(sessionUser1 != sessionUser3)
            assertFalse(sessionUser1 == sessionUser3)
        }

        @Test
        fun `SessionUser copy works correctly`() {
            val original = SessionUser(userId = 1, email = "test@test.com", displayName = "Original")

            val copied = original.copy(displayName = "Modified")

            assertEquals(original.userId, copied.userId)
            assertEquals(original.email, copied.email)
            assertEquals("Modified", copied.displayName)
            assertNotEquals(original.displayName, copied.displayName)
        }

        @Test
        fun `SessionUser toString contains all fields`() {
            val sessionUser = SessionUser(userId = 1, email = "test@test.com", displayName = "Test")

            val stringRepresentation = sessionUser.toString()

            assertTrue(stringRepresentation.contains("userId=1"))
            assertTrue(stringRepresentation.contains("email=test@test.com"))
            assertTrue(stringRepresentation.contains("displayName=Test"))
        }

        @Test
        fun `SessionUser hashCode is consistent with equality`() {
            val sessionUser1 = SessionUser(userId = 1, email = "test@test.com", displayName = "Test")
            val sessionUser2 = SessionUser(userId = 1, email = "test@test.com", displayName = "Test")

            assertEquals(sessionUser1.hashCode(), sessionUser2.hashCode())
        }

        @Test
        fun `different users have different hashCodes`() {
            val sessionUser1 = SessionUser(userId = 1, email = "user1@test.com", displayName = "User 1")
            val sessionUser2 = SessionUser(userId = 2, email = "user2@test.com", displayName = "User 2")

            assertNotEquals(sessionUser1.hashCode(), sessionUser2.hashCode())
        }
    }
}
