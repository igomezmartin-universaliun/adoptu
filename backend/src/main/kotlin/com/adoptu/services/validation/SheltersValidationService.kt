package com.adoptu.services.validation

import com.adoptu.services.ServiceResult

class SheltersValidationService {

    fun validateId(id: String?): ServiceResult<Int> {
        val parsedId = id?.toIntOrNull()
        return if (parsedId != null) ServiceResult.Success(parsedId) 
               else ServiceResult.Error(ValidationConstants.INVALID_ID)
    }

    fun validateRequired(value: String?, fieldName: String): ServiceResult<String> {
        return if (!value.isNullOrBlank()) ServiceResult.Success(value)
               else ServiceResult.Error("$fieldName is required")
    }
}
