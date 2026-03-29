package com.adoptu.services.validation

import com.adoptu.dto.input.UserDto
import com.adoptu.services.ServiceResult
import com.adoptu.services.UserService
import com.adoptu.services.auth.SessionUser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthValidationService : KoinComponent {

    private val userService: UserService by inject()

    fun validateSession(session: SessionUser?): ServiceResult<SessionUser> {
        return if (session != null) ServiceResult.Success(session) 
               else ServiceResult.Forbidden
    }

    fun validateUserById(userId: Int): ServiceResult<UserDto> {
        val user = userService.getById(userId)
        return if (user != null) ServiceResult.Success(user) 
               else ServiceResult.NotFound
    }

    fun validateNotBanned(userId: Int): ServiceResult<UserDto> {
        if (userService.isBanned(userId)) {
            val user = userService.getById(userId)
            return if (user != null) ServiceResult.Error("Your account has been suspended. Reason: ${user.banReason ?: "Contact administrator"}")
                   else ServiceResult.NotFound
        }
        return validateUserById(userId)
    }

    fun validateVerified(userId: Int, email: String): ServiceResult<Unit> {
        if (!userService.isUserVerified(userId)) {
            return ServiceResult.Error(email)
        }
        return ServiceResult.Success(Unit)
    }
}
