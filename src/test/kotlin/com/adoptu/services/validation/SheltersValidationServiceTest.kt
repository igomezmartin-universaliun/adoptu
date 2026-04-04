package com.adoptu.services.validation

import com.adoptu.services.ServiceResult
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheltersValidationServiceTest {

    private val service = SheltersValidationService()

    @Nested
    inner class ValidateId {
        @Test
        fun `returns Success for valid numeric ID`() {
            val result = service.validateId("123")

            assertTrue(result is ServiceResult.Success)
            assertEquals(123, (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Success for zero ID`() {
            val result = service.validateId("0")

            assertTrue(result is ServiceResult.Success)
            assertEquals(0, (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Success for large numeric ID`() {
            val result = service.validateId("999999999")

            assertTrue(result is ServiceResult.Success)
            assertEquals(999999999, (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Error for non-numeric ID`() {
            val result = service.validateId("abc")

            assertTrue(result is ServiceResult.Error)
            assertEquals(ValidationConstants.INVALID_ID, (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error for alphanumeric ID`() {
            val result = service.validateId("123abc")

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
        fun `returns Success for single character value`() {
            val result = service.validateRequired("a", "field")

            assertTrue(result is ServiceResult.Success)
            assertEquals("a", (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Success for whitespace-trimmed value`() {
            val result = service.validateRequired("  test  ", "testField")

            assertTrue(result is ServiceResult.Success)
            assertEquals("  test  ", (result as ServiceResult.Success).data)
        }

        @Test
        fun `returns Error for null value`() {
            val result = service.validateRequired(null, "testField")

            assertTrue(result is ServiceResult.Error)
            assertEquals("testField is required", (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error for empty string`() {
            val result = service.validateRequired("", "testField")

            assertTrue(result is ServiceResult.Error)
            assertEquals("testField is required", (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error for blank string`() {
            val result = service.validateRequired("   ", "testField")

            assertTrue(result is ServiceResult.Error)
            assertEquals("testField is required", (result as ServiceResult.Error).message)
        }

        @Test
        fun `returns Error with correct field name`() {
            val result = service.validateRequired(null, "shelterName")

            assertTrue(result is ServiceResult.Error)
            assertEquals("shelterName is required", (result as ServiceResult.Error).message)
        }
    }
}
