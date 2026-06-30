package com.adoptu.services.validation

import com.adoptu.services.ServiceResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SheltersValidationServiceTest {

    private lateinit var service: SheltersValidationService

    @BeforeEach
    fun setup() {
        service = SheltersValidationService()
    }

    @Test
    fun `validateId returns Success for valid numeric id`() {
        val result = service.validateId("42")

        assertIs<ServiceResult.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun `validateId returns Error for null id`() {
        val result = service.validateId(null)

        assertIs<ServiceResult.Error<Int>>(result)
        assertEquals(ValidationConstants.INVALID_ID, result.message)
    }

    @Test
    fun `validateId returns Error for non-numeric id`() {
        val result = service.validateId("not-a-number")

        assertIs<ServiceResult.Error<Int>>(result)
        assertEquals(ValidationConstants.INVALID_ID, result.message)
    }

    @Test
    fun `validateId returns Error for blank id`() {
        val result = service.validateId("")

        assertIs<ServiceResult.Error<Int>>(result)
        assertEquals(ValidationConstants.INVALID_ID, result.message)
    }

    @Test
    fun `validateRequired returns Success for non-blank value`() {
        val result = service.validateRequired("Some value", "Name")

        assertIs<ServiceResult.Success<String>>(result)
        assertEquals("Some value", result.data)
    }

    @Test
    fun `validateRequired returns Error for null value`() {
        val result = service.validateRequired(null, "Name")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Name is required", result.message)
    }

    @Test
    fun `validateRequired returns Error for blank value`() {
        val result = service.validateRequired("   ", "Name")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Name is required", result.message)
    }

    @Test
    fun `validateRequired returns Error for empty value`() {
        val result = service.validateRequired("", "City")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("City is required", result.message)
    }
}
