package com.adoptu.services

import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.UserDto
import com.adoptu.dto.UserRole
import com.adoptu.models.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

object UserService {

    fun getById(userId: Int): UserDto? = transaction {
        Users.selectAll()
            .where { Users.id eq userId }
            .firstOrNull()
            ?.let { user ->
                UserDto(
                    id = user[Users.id],
                    username = user[Users.username],
                    displayName = user[Users.displayName],
                    email = user[Users.email],
                    role = UserRole.valueOf(user[Users.role]),
                    lastAcceptedPrivacyPolicy = user[Users.lastAcceptedPrivacyPolicy],
                    lastAcceptedTermsAndConditions = user[Users.lastAcceptedTermsAndConditions]
                )
            }
    }

    fun acceptTerms(userId: Int, request: AcceptTermsRequest): UserDto? {
        val now = System.currentTimeMillis()
        transaction {
            Users.update({ Users.id eq userId }) {
                if (request.acceptPrivacyPolicy) {
                    it[Users.lastAcceptedPrivacyPolicy] = now
                }
                if (request.acceptTermsAndConditions) {
                    it[Users.lastAcceptedTermsAndConditions] = now
                }
            }
        }
        return getById(userId)
    }
}
