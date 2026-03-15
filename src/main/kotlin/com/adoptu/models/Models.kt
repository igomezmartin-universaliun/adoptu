package com.adoptu.models

import org.jetbrains.exposed.v1.core.Table
import java.math.BigDecimal

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex()
    val displayName = varchar("display_name", 255)
    val email = varchar("email", 255).nullable()
    val role = varchar("role", 50) // ADMIN, RESCUER, ADOPTER
    val createdAt = long("created_at")
    val lastAcceptedPrivacyPolicy = long("last_accepted_privacy_policy").nullable()
    val lastAcceptedTermsAndConditions = long("last_accepted_terms_and_conditions").nullable()

    override val primaryKey = PrimaryKey(id)
}

object WebAuthnCredentials : Table("webauthn_credentials") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val credentialId = varchar("credential_id", 500)
    val attestedCredentialDataBase64 = text("attested_credential_data")
    val signCount = long("sign_count")
    val transports = varchar("transports", 255).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Pets : Table("pets") {
    val id = integer("id").autoIncrement()
    val rescuerId = integer("rescuer_id").references(Users.id)
    val name = varchar("name", 255)
    val type = varchar("type", 50) // DOG, CAT, BIRD, FISH
    val breed = varchar("breed", 255).nullable()
    val description = text("description")
    val weight = decimal("weight", 6, 2)
    val ageYears = integer("age_years")
    val ageMonths = integer("age_months")
    val sex = varchar("sex", 10).default("MALE") // MALE, FEMALE
    val status = varchar("status", 50) // AVAILABLE, ADOPTED, PENDING
    val color = varchar("color", 100).nullable()
    val size = varchar("size", 20).nullable() // SMALL, MEDIUM, LARGE
    val temperament = varchar("temperament", 100).nullable()
    val isSterilized = bool("is_sterilized").default(false)
    val isMicrochipped = bool("is_microchipped").default(false)
    val microchipId = varchar("microchip_id", 100).nullable()
    val vaccinations = text("vaccinations").nullable()
    val isGoodWithKids = bool("is_good_with_kids").default(true)
    val isGoodWithDogs = bool("is_good_with_dogs").default(true)
    val isGoodWithCats = bool("is_good_with_cats").default(true)
    val isHouseTrained = bool("is_house_trained").default(false)
    val energyLevel = varchar("energy_level", 20).nullable() // LOW, MEDIUM, HIGH
    val rescueDate = long("rescue_date").nullable()
    val rescueLocation = varchar("rescue_location", 255).nullable()
    val specialNeeds = text("special_needs").nullable()
    val adoptionFee = decimal("adoption_fee", 10, 2).default(BigDecimal.ZERO)
    val currency = varchar("currency", 10).default("USD")
    val isUrgent = bool("is_urgent").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PetImages : Table("pet_images") {
    val id = integer("id").autoIncrement()
    val petId = integer("pet_id").references(Pets.id)
    val imageUrl = text("image_url") // S3 URL
    val isPrimary = bool("is_primary").default(false)
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(id)
}

object AdoptionRequests : Table("adoption_requests") {
    val id = integer("id").autoIncrement()
    val petId = integer("pet_id").references(Pets.id)
    val adopterId = integer("adopter_id").references(Users.id)
    val message = text("message")
    val status = varchar("status", 50) // PENDING, APPROVED, REJECTED
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
