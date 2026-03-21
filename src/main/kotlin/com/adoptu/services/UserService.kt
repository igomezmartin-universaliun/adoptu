package com.adoptu.services

import com.adoptu.dto.AcceptTermsRequest
import com.adoptu.dto.PhotographerDto
import com.adoptu.dto.PhotographerSettingsRequest
import com.adoptu.dto.UserDto
import com.adoptu.dto.UserRole
import com.adoptu.models.Pets
import com.adoptu.models.Photographers
import com.adoptu.models.UserActiveRoles
import com.adoptu.models.Users
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal

object UserService {

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

    fun getById(userId: Int): UserDto? = transaction {
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

    fun getPhotographerById(userId: Int): PhotographerDto? {
        val hasPhotographerRole = isRoleActive(userId, UserRole.PHOTOGRAPHER)
        if (!hasPhotographerRole) return null
        
        return transaction {
            val user = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            val photographer = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            if (user != null) {
                PhotographerDto(
                    userId = userId,
                    displayName = user[Users.displayName],
                    username = user[Users.username],
                    photographerFee = photographer?.get(Photographers.photographerFee)?.toDouble(),
                    photographerCurrency = photographer?.get(Photographers.photographerCurrency),
                    country = photographer?.get(Photographers.country),
                    state = photographer?.get(Photographers.state)
                )
            } else null
        }
    }

    fun getPhotographers(country: String? = null, state: String? = null): List<PhotographerDto> = transaction {
        val photographerUserIds = UserActiveRoles.selectAll()
            .where { UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name }
            .map { row -> row[UserActiveRoles.userId] }
            .distinct()
        
        photographerUserIds.mapNotNull { userId ->
            val user = Users.selectAll().where { Users.id eq userId }.firstOrNull()
            val photographer = Photographers.selectAll().where { Photographers.userId eq userId }.firstOrNull()
            
            if (user != null && photographer != null) {
                val photographerCountry = photographer[Photographers.country]
                val photographerState = photographer[Photographers.state]
                
                val matchesCountry = country.isNullOrBlank() || photographerCountry == country
                val matchesState = state.isNullOrBlank() || photographerState == state
                
                if (matchesCountry && matchesState) {
                    PhotographerDto(
                        userId = userId,
                        displayName = user[Users.displayName],
                        username = user[Users.username],
                        photographerFee = photographer[Photographers.photographerFee]?.toDouble(),
                        photographerCurrency = photographer[Photographers.photographerCurrency],
                        country = photographerCountry,
                        state = photographerState
                    )
                } else null
            } else null
        }
    }

    fun getRescuers(): List<UserDto> = transaction {
        UserActiveRoles.selectAll()
            .where { UserActiveRoles.role eq UserRole.RESCUER.name }
            .map { row -> row[UserActiveRoles.userId] }
            .distinct()
            .mapNotNull { userId -> getById(userId) }
    }

    fun isRoleActive(userId: Int, role: UserRole): Boolean {
        return transaction {
            UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq role.name) }
                .count() > 0
        }
    }

    fun activateRescuerProfile(userId: Int): UserDto? {
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

    fun deactivateRescuerProfile(userId: Int): UserDto? {
        transaction {
            val rolesToDelete = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.RESCUER.name) }
                .toList()
            
            for (row in rolesToDelete) {
                val uid = row[UserActiveRoles.userId]
                val r = row[UserActiveRoles.role]
                UserActiveRoles.update(
                    { (UserActiveRoles.userId eq uid) and (UserActiveRoles.role eq r) }
                ) { }
            }
            
            Pets.update({ Pets.rescuerId eq userId }) {
                it[Pets.isPromoted] = false
            }
        }
        return getById(userId)
    }

    fun activatePhotographerProfile(userId: Int): UserDto? {
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

    fun deactivatePhotographerProfile(userId: Int): UserDto? {
        transaction {
            val rolesToDelete = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.PHOTOGRAPHER.name) }
                .toList()
            
            for (row in rolesToDelete) {
                val uid = row[UserActiveRoles.userId]
                val r = row[UserActiveRoles.role]
                UserActiveRoles.update(
                    { (UserActiveRoles.userId eq uid) and (UserActiveRoles.role eq r) }
                ) { }
            }
        }
        return getById(userId)
    }

    fun activateTemporalHomeProfile(userId: Int): UserDto? {
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

    fun deactivateTemporalHomeProfile(userId: Int): UserDto? {
        transaction {
            val rolesToDelete = UserActiveRoles.selectAll()
                .where { (UserActiveRoles.userId eq userId) and (UserActiveRoles.role eq UserRole.TEMPORAL_HOME.name) }
                .toList()
            
            for (row in rolesToDelete) {
                val uid = row[UserActiveRoles.userId]
                val r = row[UserActiveRoles.role]
                UserActiveRoles.update(
                    { (UserActiveRoles.userId eq uid) and (UserActiveRoles.role eq r) }
                ) { }
            }
        }
        return getById(userId)
    }

    fun updateProfile(userId: Int, displayName: String, language: String? = null): UserDto? {
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

    fun updateLanguage(userId: Int, language: String): UserDto? {
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

    fun updatePhotographerSettings(userId: Int, request: PhotographerSettingsRequest): PhotographerDto? {
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
