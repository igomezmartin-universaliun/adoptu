package com.adoptu.services.validation

import com.adoptu.dto.input.UserDto
import com.adoptu.dto.input.UserRole
import com.adoptu.services.ServiceResult
import com.adoptu.services.auth.SessionUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsersValidationServiceTest {

    private val service by lazy { UsersValidationService() }

    @Nested
    inner class ValidateSession {
        @Test
        fun `returns Success when session is not null`() {
            val session = SessionUser(userId = 1, email = "test@test.com", displayName = "Test")

            val result = service.validateSession(session)

            assertTrue(result is ServiceResult.Success)
            assertEquals(session, (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Forbidden when session is null`() {
            val result = service.validateSession(null)

            assertEquals(ServiceResult.Forbidden, result)
        }
    }

    @Nested
    inner class ValidateUser {
        @Test
        fun `returns Success when user is not null`() {
            val user = UserDto(id = 1, username = "test@test.com", displayName = "Test")

            val result = service.validateUser(user)

            assertTrue(result is ServiceResult.Success)
            assertEquals(user, (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns NotFound when user is null`() {
            val result = service.validateUser(null)

            assertEquals(ServiceResult.NotFound, result)
        }
    }

    @Nested
    inner class ValidateId {
        @Test
        fun `returns Success for valid numeric ID`() {
            val result = service.validateId("123")

            assertTrue(result is ServiceResult.Success)
            assertEquals(123, (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Error for non-numeric ID`() {
            val result = service.validateId("abc")

            assertTrue(result is ServiceResult.Error)
            assertEquals(ValidationConstants.INVALID_ID, (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error for null ID`() {
            val result = service.validateId(null)

            assertTrue(result is ServiceResult.Error)
            assertEquals(ValidationConstants.INVALID_ID, (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error for blank ID`() {
            val result = service.validateId("   ")

            assertTrue(result is ServiceResult.Error)
            assertEquals(ValidationConstants.INVALID_ID, (result as ServiceResult.Error).message)
        }
    }

    @Nested
    inner class ValidateRequired {
        @Test
        fun `returns Success for non-blank value`() {
            val result = service.validateRequired("test", "testField")

            assertTrue(result is ServiceResult.Success)
            assertEquals("test", (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Error for null value`() {
            val result = service.validateRequired(null, "testField")

            assertTrue(result is ServiceResult.Error)
            assertEquals("testField is required", (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error for blank value`() {
            val result = service.validateRequired("   ", "testField")

            assertTrue(result is ServiceResult.Error)
            assertEquals("testField is required", (result as ServiceResult.Error).message)
        }
    }

    @Nested
    inner class ValidateRole {
        @Test
        fun `returns Success when user has required role`() {
            val user = UserDto(id = 1, username = "test@test.com", displayName = "Test", activeRoles = setOf(UserRole.RESCUER))

            val result = service.validateRole(user, "RESCUER")

            assertEquals(ServiceResult.Success(Unit), result)
        }

        @Test
        fun `returns Success when user is admin regardless of role`() {
            val user = UserDto(id = 1, username = "test@test.com", displayName = "Test", activeRoles = setOf(UserRole.ADMIN))

            val result = service.validateRole(user, "RESCUER")

            assertEquals(ServiceResult.Success(Unit), result)
        }

        @Test
        fun `returns Forbidden when user lacks required role`() {
            val user = UserDto(id = 1, username = "test@test.com", displayName = "Test", activeRoles = setOf(UserRole.ADOPTER))

            val result = service.validateRole(user, "RESCUER")

            assertEquals(ServiceResult.Forbidden, result)
        }
    }

    @Nested
    inner class ValidateNotSelf {
        @Test
        fun `returns Success when user IDs are different`() {
            val result = service.validateNotSelf(currentUserId = 1, targetUserId = 2)

            assertEquals(ServiceResult.Success(Unit), result)
        }

        @Test
        fun `returns Error when user IDs are the same`() {
            val result = service.validateNotSelf(currentUserId = 1, targetUserId = 1)

            assertTrue(result is ServiceResult.Error)
            assertEquals("Cannot perform action on yourself", (result as ServiceResult.Error).message)
        }
    }

    @Nested
    inner class ValidateNotAdmin {
        @Test
        fun `returns Success when user is not admin`() {
            val user = UserDto(id = 1, username = "test@test.com", displayName = "Test", activeRoles = setOf(UserRole.RESCUER))

            val result = service.validateNotAdmin(user)

            assertEquals(ServiceResult.Success(Unit), result)
        }

        @Test
        fun `returns Error when user is admin`() {
            val user = UserDto(id = 1, username = "test@test.com", displayName = "Test", activeRoles = setOf(UserRole.ADMIN))

            val result = service.validateNotAdmin(user)

            assertTrue(result is ServiceResult.Error)
            assertEquals("Cannot perform action on an admin", (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns NotFound when user is null`() {
            val result = service.validateNotAdmin(null)

            assertEquals(ServiceResult.NotFound, result)
        }
    }
}
