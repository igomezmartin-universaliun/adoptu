package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.Pets
import com.adoptu.adapters.db.Photographers
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.adapters.db.Users
import com.adoptu.adapters.db.EmailVerificationTokens
import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.PhotographerDto
import com.adoptu.dto.PhotographerSettingsRequest
import com.adoptu.dto.UserDto
import com.adoptu.dto.UserRole
import com.adoptu.ports.UserRepositoryPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal

class UserRepository : UserRepositoryPort {

    private fun getActiveRolesForUser(userId: Int): Set<UserRole> {
        return transaction {
            UserActiveRoles.selectAll()
                .where { UserActiveRoles.userId eq userId }
                .map { row ->
                    try {
                        UserRole.valueOf(row[UserActiveRoles.role])
                    } catch (e: Exception) {
                        null
                    }
                }
                .filterNotNull()
                .toSet()
        }
    }

    override fun getById(userId: Int): UserDto? = transaction {
        Users.selectAll()
            .where { Users.id eq userId }
            .firstOrNull()
            ?.let { user ->
                val activeRoles = getActiveRolesForUser(userId)
                UserDto(
                    id = user[Users.id],
                    username = user[Users.username],
                    email = user[Users.username],
                    displayName = user[Users.displayName],
                    language = user[Users.language],
                    activeRoles = activeRoles,
                    lastAcceptedPrivacyPolicy = user[Users.lastAcceptedPrivacyPolicy],
                    lastAcceptedTermsAndConditions = user[Users.lastAcceptedTermsAndConditions]
                )
            }
    }

    override fun getPhotographers(country: String?, state: String?): List<PhotographerDto> = transaction {
        val photographerUserIds = UserActiveRoles.selectAll()
            .where { UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name }
            .map { row -> row[UserActiveRoles.userId] }
            .distinct()
        
        photographerUserIds.mapNotNull { userId ->
            val user = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            val photographer = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            
            if (user != null) {
                val photographerCountry = photographer?.get(Photographers.country)
                val photographerState = photographer?.get(Photographers.state)
                
                val matchesCountry = country.isNullOrBlank() || photographerCountry == country
                val matchesState = state.isNullOrBlank() || photographerState == state
                
                if (matchesCountry && matchesState) {
                    PhotographerDto(
                        userId = userId,
                        displayName = user[Users.displayName],
                        username = user[Users.username],
                        photographerFee = photographer?.get(Photographers.photographerFee)?.toDouble(),
                        photographerCurrency = photographer?.get(Photographers.photographerCurrency),
                        country = photographerCountry,
                        state = photographerState
                    )
                } else null
            } else null
        }
    }

    override fun getRescuers(): List<UserDto> = transaction {
        UserActiveRoles.selectAll()
            .where { UserActiveRoles.role eq UserRole.RESCUER.name }
            .map { row -> row[UserActiveRoles.userId] }
            .distinct()
            .mapNotNull { userId -> getById(userId) }
    }

    override fun isRoleActive(userId: Int, role: UserRole): Boolean {
        return transaction {
            UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq role.name) }
                .count() > 0
        }
    }

    override fun activateRescuerProfile(userId: Int): UserDto? {
        val user = getById(userId) ?: return null
        
        transaction {
            val existingRole = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.RESCUER.name) }
                .firstOrNull()
            if (existingRole == null) {
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = userId
                    it[UserActiveRoles.role] = UserRole.RESCUER.name
                }
            }
        }
        return getById(userId)
    }

    override fun deactivateRescuerProfile(userId: Int): UserDto? {
        transaction {
            UserActiveRoles.deleteWhere {
                (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.RESCUER.name)
            }
            
            Pets.update({ Pets.rescuerId eq userId }) {
                it[Pets.isPromoted] = false
            }
        }
        return getById(userId)
    }

    override fun activatePhotographerProfile(userId: Int): UserDto? {
        val user = getById(userId) ?: return null
        
        transaction {
            val existingRole = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name) }
                .firstOrNull()
            if (existingRole == null) {
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = userId
                    it[UserActiveRoles.role] = UserRole.PHOTOGRAPHER.name
                }
            }
            val existingPhotographer = Photographers.selectAll()
                .where { Photographers.userId eq userId }
                .firstOrNull()
            if (existingPhotographer == null) {
                Photographers.insert {
                    it[Photographers.userId] = userId
                    it[country] = null
                    it[state] = null
                }
            }
        }
        return getById(userId)
    }

    override fun deactivatePhotographerProfile(userId: Int): UserDto? {
        transaction {
            UserActiveRoles.deleteWhere {
                (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name)
            }
        }
        return getById(userId)
    }

    override fun activateTemporalHomeProfile(userId: Int): UserDto? {
        val user = getById(userId) ?: return null
        
        transaction {
            val existingRole = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.TEMPORAL_HOME.name) }
                .firstOrNull()
            if (existingRole == null) {
                UserActiveRoles.insert {
                    it[UserActiveRoles.userId] = userId
                    it[UserActiveRoles.role] = UserRole.TEMPORAL_HOME.name
                }
            }
        }
        return getById(userId)
    }

    override fun deactivateTemporalHomeProfile(userId: Int): UserDto? {
        transaction {
            UserActiveRoles.deleteWhere {
                (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.TEMPORAL_HOME.name)
            }
        }
        return getById(userId)
    }

    override fun updateProfile(userId: Int, displayName: String, language: String?): UserDto? {
        if (displayName.isBlank()) {
            throw IllegalArgumentException("Display name cannot be empty")
        }
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.displayName] = displayName
                if (language != null) {
                    it[Users.language] = language
                }
            }
        }
        return getById(userId)
    }

    override fun updateLanguage(userId: Int, language: String): UserDto? {
        if (language.isBlank()) {
            throw IllegalArgumentException("Language cannot be empty")
        }
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.language] = language
            }
        }
        return getById(userId)
    }

    override fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto? {
        if (request.photographerFee < 0) {
            throw IllegalArgumentException("Photographer fee must be zero or positive")
        }
        
        val userExists = transaction {
            Users.selectAll().where { Users.id eq userId }.firstOrNull() != null
        }
        if (!userExists) return null
        
        transaction {
            val existing = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            if (existing != null) {
                Photographers.update({ Photographers.userId eq userId }) {
                    it[photographerFee] = BigDecimal.valueOf(request.photographerFee)
                    it[photographerCurrency] = request.photographerCurrency
                    it[country] = request.country
                    it[state] = request.state
                }
            } else {
                Photographers.insert {
                    it[Photographers.userId] = userId
                    it[photographerFee] = BigDecimal.valueOf(request.photographerFee)
                    it[photographerCurrency] = request.photographerCurrency
                    it[country] = request.country
                    it[state] = request.state
                }
            }
        }
        
        return transaction {
            val user = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            val photographer = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            if (user != null) {
                PhotographerDto(
                    userId = userId,
                    displayName = user[Users.displayName],
                    photographerFee = photographer?.get(Photographers.photographerFee)?.toDouble(),
                    photographerCurrency = photographer?.get(Photographers.photographerCurrency),
                    country = photographer?.get(Photographers.country),
                    state = photographer?.get(Photographers.state)
                )
            } else null
        }
    }

    override fun acceptTerms(userId: Int, request: AcceptTermsRequest): UserDto? {
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

    override fun isEmailVerified(userId: Int): Boolean {
        return transaction {
            Users.selectAll()
                .where { Users.id eq userId }
                .firstOrNull()
                ?.get(Users.isEmailVerified) ?: false
        }
    }

    override fun setEmailVerified(userId: Int, verified: Boolean): Boolean {
        return transaction {
            val rowsUpdated = Users.update({ Users.id eq userId }) {
                it[Users.isEmailVerified] = verified
            }
            rowsUpdated > 0
        }
    }

    override fun createEmailVerificationToken(userId: Int, token: String, expiresAt: Long): Boolean {
        return transaction {
            try {
                EmailVerificationTokens.insert {
                    it[EmailVerificationTokens.userId] = userId
                    it[EmailVerificationTokens.token] = token
                    it[EmailVerificationTokens.expiresAt] = expiresAt
                    it[EmailVerificationTokens.createdAt] = System.currentTimeMillis()
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun verifyToken(token: String): Int? {
        return transaction {
            val now = System.currentTimeMillis()
            val tokenRow = EmailVerificationTokens
                .selectAll()
                .where { EmailVerificationTokens.token eq token }
                .firstOrNull()

            if (tokenRow != null && tokenRow[EmailVerificationTokens.expiresAt] > now) {
                tokenRow[EmailVerificationTokens.userId]
            } else {
                null
            }
        }
    }

    override fun deleteVerificationTokens(userId: Int) {
        transaction {
            EmailVerificationTokens.deleteWhere { EmailVerificationTokens.userId eq userId }
        }
    }
}
