package com.adoptu.services.validation

import com.adoptu.services.ServiceResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SterilizationLocationsValidationServiceTest {

    private lateinit var service: SterilizationLocationsValidationService

    @BeforeEach
    fun setup() {
        service = SterilizationLocationsValidationService()
    }

    @Test
    fun `validateId returns Success for valid numeric id`() {
        val result = service.validateId("17")

        assertIs<ServiceResult.Success<Int>>(result)
        assertEquals(17, result.data)
    }

    @Test
    fun `validateId returns Error for null id`() {
        val result = service.validateId(null)

        assertIs<ServiceResult.Error<Int>>(result)
        assertEquals(ValidationConstants.INVALID_ID, result.message)
    }

    @Test
    fun `validateId returns Error for non-numeric id`() {
        val result = service.validateId("abc")

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
        val result = service.validateRequired("Vet Clinic", "Name")

        assertIs<ServiceResult.Success<String>>(result)
        assertEquals("Vet Clinic", result.data)
    }

    @Test
    fun `validateRequired returns Error for null value`() {
        val result = service.validateRequired(null, "Country")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Country is required", result.message)
    }

    @Test
    fun `validateRequired returns Error for blank value`() {
        val result = service.validateRequired("   ", "Country")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("Country is required", result.message)
    }

    @Test
    fun `validateRequired returns Error for empty value`() {
        val result = service.validateRequired("", "City")

        assertIs<ServiceResult.Error<String>>(result)
        assertEquals("City is required", result.message)
    }
}
