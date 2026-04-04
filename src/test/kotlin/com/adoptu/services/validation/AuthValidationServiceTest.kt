package com.adoptu.services.validation

import com.adoptu.services.ServiceResult
import com.adoptu.services.auth.SessionUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthValidationServiceTest {

    private val service = AuthValidationService()

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
}
