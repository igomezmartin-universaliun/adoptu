package com.adoptu.adapters.db.repositories

import com.adoptu.adapters.db.AdoptionRequests
import com.adoptu.adapters.db.PetImages
import com.adoptu.adapters.db.Pets
import com.adoptu.adapters.db.UserActiveRoles
import com.adoptu.dto.input.AdoptionRequestDto
import com.adoptu.dto.input.Currency
import com.adoptu.dto.input.Gender
import com.adoptu.dto.input.PetDto
import com.adoptu.dto.input.PetImageDto
import com.adoptu.dto.input.Status
import com.adoptu.dto.input.UpdatePetRequest
import com.adoptu.dto.input.UserRole
import com.adoptu.ports.PetRepositoryPort
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PetRepositoryImpl(private val clock: Clock) : PetRepositoryPort {

    private fun rowToPetDto(row: ResultRow): PetDto {
        val petId = row[Pets.id]
        val images = getPetImages(petId)
        return PetDto(
            id = petId,
            rescuerId = row[Pets.rescuerId],
            name = row[Pets.name],
            type = row[Pets.type],
            breed = row[Pets.breed],
            description = row[Pets.description],
            weight = row[Pets.weight].toDouble(),
            ageYears = row[Pets.ageYears],
            ageMonths = row[Pets.ageMonths],
            status = Status.valueOf(row[Pets.status]),
            sex = Gender.valueOf(row[Pets.sex]),
            color = row[Pets.color],
            size = row[Pets.size],
            temperament = row[Pets.temperament],
            isSterilized = row[Pets.isSterilized],
            isMicrochipped = row[Pets.isMicrochipped],
            microchipId = row[Pets.microchipId],
            vaccinations = row[Pets.vaccinations],
            isGoodWithKids = row[Pets.isGoodWithKids],
            isGoodWithDogs = row[Pets.isGoodWithDogs],
            isGoodWithCats = row[Pets.isGoodWithCats],
            isHouseTrained = row[Pets.isHouseTrained],
            energyLevel = row[Pets.energyLevel],
            rescueDate = row[Pets.rescueDate],
            rescueLocation = row[Pets.rescueLocation],
            specialNeeds = row[Pets.specialNeeds],
            adoptionFee = row[Pets.adoptionFee].toDouble(),
            currency = Currency.valueOf(row[Pets.currency]),
            isUrgent = row[Pets.isUrgent],
            isPromoted = row[Pets.isPromoted],
            createdAt = row[Pets.createdAt],
            images = images
        )
    }

    private fun getPetImages(petId: Int): List<PetImageDto> = transaction {
        PetImages.selectAll()
            .where { PetImages.petId eq petId }
            .orderBy(PetImages.sortOrder, SortOrder.ASC)
            .map { row ->
                PetImageDto(
                    id = row[PetImages.id],
                    imageUrl = row[PetImages.imageUrl],
                    isPrimary = row[PetImages.isPrimary],
                    sortOrder = row[PetImages.sortOrder]
                )
            }
    }

    override fun getAll(type: String?, showPromotedOnly: Boolean): List<PetDto> = transaction {
        val baseCondition = Pets.status eq "AVAILABLE"
        val finalCondition = if (type != null) {
            if (showPromotedOnly) {
                baseCondition and (Pets.type eq type.uppercase()) and (Pets.isPromoted eq true)
            } else {
                baseCondition and (Pets.type eq type.uppercase())
            }
        } else {
            if (showPromotedOnly) {
                baseCondition and (Pets.isPromoted eq true)
            } else {
                baseCondition
            }
        }
        
        val rescuerIds = UserActiveRoles.select(UserActiveRoles.userId)
            .where { UserActiveRoles.role eq UserRole.RESCUER.name }
            .map { it[UserActiveRoles.userId] }
        
        Pets.selectAll()
            .where { finalCondition and (Pets.rescuerId inList rescuerIds) }
            .orderBy(Pets.createdAt, SortOrder.DESC)
            .map(::rowToPetDto)
    }

    override fun getById(id: Int): PetDto? = transaction {
        Pets.selectAll().where { Pets.id eq id }.map(::rowToPetDto).firstOrNull()
    }

    override fun create(
        rescuerId: Int,
        name: String,
        type: String,
        description: String,
        weight: Double,
        ageYears: Int,
        ageMonths: Int,
        sex: Gender,
        breed: String?,
        color: String?,
        size: String?,
        temperament: String?,
        isSterilized: Boolean,
        isMicrochipped: Boolean,
        microchipId: String?,
        vaccinations: String?,
        isGoodWithKids: Boolean,
        isGoodWithDogs: Boolean,
        isGoodWithCats: Boolean,
        isHouseTrained: Boolean,
        energyLevel: String?,
        rescueDate: Long?,
        rescueLocation: String?,
        specialNeeds: String?,
        adoptionFee: Double,
        currency: Currency,
        isUrgent: Boolean,
        isPromoted: Boolean,
        status: String
    ): PetDto = transaction {
        val id = Pets.insert {
            it[Pets.rescuerId] = rescuerId
            it[Pets.name] = name
            it[Pets.type] = type.uppercase()
            it[Pets.breed] = breed
            it[Pets.description] = description
            it[Pets.weight] = BigDecimal(weight.toString())
            it[Pets.ageYears] = ageYears
            it[Pets.ageMonths] = ageMonths
            it[Pets.status] = status
            it[Pets.sex] = sex.name
            it[Pets.color] = color
            it[Pets.size] = size
            it[Pets.temperament] = temperament
            it[Pets.isSterilized] = isSterilized
            it[Pets.isMicrochipped] = isMicrochipped
            it[Pets.microchipId] = microchipId
            it[Pets.vaccinations] = vaccinations
            it[Pets.isGoodWithKids] = isGoodWithKids
            it[Pets.isGoodWithDogs] = isGoodWithDogs
            it[Pets.isGoodWithCats] = isGoodWithCats
            it[Pets.isHouseTrained] = isHouseTrained
            it[Pets.energyLevel] = energyLevel
            it[Pets.rescueDate] = rescueDate
            it[Pets.rescueLocation] = rescueLocation
            it[Pets.specialNeeds] = specialNeeds
            it[Pets.adoptionFee] = BigDecimal(adoptionFee.toString())
            it[Pets.currency] = currency.name
            it[Pets.isUrgent] = isUrgent
            it[Pets.isPromoted] = isPromoted
            it[Pets.createdAt] = clock.now().toEpochMilliseconds()
        } get Pets.id

        getById(id!!)!!
    }

    override fun update(id: Int, body: UpdatePetRequest): PetDto? = transaction {
        Pets.update({ Pets.id eq id }) {
            body.name?.let { name -> it[Pets.name] = name }
            body.type?.let { type -> it[Pets.type] = type.uppercase() }
            body.breed?.let { breed -> it[Pets.breed] = breed }
            body.description?.let { desc -> it[Pets.description] = desc }
            body.weight?.let { w -> it[Pets.weight] = BigDecimal(w.toString()) }
            body.ageYears?.let { y -> it[Pets.ageYears] = y }
            body.ageMonths?.let { m -> it[Pets.ageMonths] = m }
            body.status?.let { s -> it[Pets.status] = s.name }
            body.sex?.let { s -> it[Pets.sex] = s.name }
            body.color?.let { c -> it[Pets.color] = c }
            body.size?.let { s -> it[Pets.size] = s }
            body.temperament?.let { t -> it[Pets.temperament] = t }
            body.isSterilized?.let { n -> it[Pets.isSterilized] = n }
            body.isMicrochipped?.let { m -> it[Pets.isMicrochipped] = m }
            body.microchipId?.let { m -> it[Pets.microchipId] = m }
            body.vaccinations?.let { v -> it[Pets.vaccinations] = v }
            body.isGoodWithKids?.let { k -> it[Pets.isGoodWithKids] = k }
            body.isGoodWithDogs?.let { d -> it[Pets.isGoodWithDogs] = d }
            body.isGoodWithCats?.let { c -> it[Pets.isGoodWithCats] = c }
            body.isHouseTrained?.let { h -> it[Pets.isHouseTrained] = h }
            body.energyLevel?.let { e -> it[Pets.energyLevel] = e }
            body.rescueDate?.let { r -> it[Pets.rescueDate] = r }
            body.rescueLocation?.let { l -> it[Pets.rescueLocation] = l }
            body.specialNeeds?.let { s -> it[Pets.specialNeeds] = s }
            body.adoptionFee?.let { f -> it[Pets.adoptionFee] = BigDecimal(f.toString()) }
            body.currency?.let { c -> it[Pets.currency] = c.name }
            body.isUrgent?.let { u -> it[Pets.isUrgent] = u }
            body.isPromoted?.let { p -> it[Pets.isPromoted] = p }
        }
        getById(id)
    }

    override fun delete(petId: Int): Unit = transaction {
        exec("DELETE FROM pet_images WHERE pet_id = ?", listOf(IntegerColumnType() to petId))
        exec("DELETE FROM adoption_requests WHERE pet_id = ?", listOf(IntegerColumnType() to petId))
        exec("DELETE FROM pets WHERE id = ?", listOf(IntegerColumnType() to petId))
    }

    override fun createAdoptionRequest(petId: Int, adopterId: Int, message: String): AdoptionRequestDto = transaction {
        val createdAt = clock.now().toEpochMilliseconds()
        val id = AdoptionRequests.insert {
            it[AdoptionRequests.petId] = petId
            it[AdoptionRequests.adopterId] = adopterId
            it[AdoptionRequests.message] = message
            it[AdoptionRequests.status] = "PENDING"
            it[AdoptionRequests.createdAt] = createdAt
        } get AdoptionRequests.id

        AdoptionRequestDto(
            id = id!!,
            petId = petId,
            adopterId = adopterId,
            message = message,
            status = "PENDING",
            createdAt = createdAt
        )
    }

    override fun getAdoptionRequestsForPet(petId: Int): List<AdoptionRequestDto> = transaction {
        AdoptionRequests.selectAll()
            .where { AdoptionRequests.petId eq petId }
            .map { row ->
                AdoptionRequestDto(
                    id = row[AdoptionRequests.id],
                    petId = row[AdoptionRequests.petId],
                    adopterId = row[AdoptionRequests.adopterId],
                    message = row[AdoptionRequests.message],
                    status = row[AdoptionRequests.status],
                    createdAt = row[AdoptionRequests.createdAt]
                )
            }
    }

    override fun getAdoptionRequestsForUser(userId: Int): List<AdoptionRequestDto> = transaction {
        AdoptionRequests.selectAll()
            .where { AdoptionRequests.adopterId eq userId }
            .map { row ->
                AdoptionRequestDto(
                    id = row[AdoptionRequests.id],
                    petId = row[AdoptionRequests.petId],
                    adopterId = row[AdoptionRequests.adopterId],
                    message = row[AdoptionRequests.message],
                    status = row[AdoptionRequests.status],
                    createdAt = row[AdoptionRequests.createdAt]
                )
            }
    }

    override fun updateAdoptionRequestStatus(requestId: Int, status: String): Boolean = transaction {
        val updated = AdoptionRequests.update({ AdoptionRequests.id eq requestId }) {
            it[AdoptionRequests.status] = status
        }
        updated > 0
    }

    override fun getAdoptionRequestById(requestId: Int): AdoptionRequestDto? = transaction {
        AdoptionRequests.selectAll()
            .where { AdoptionRequests.id eq requestId }
            .firstOrNull()
            ?.let { row ->
                AdoptionRequestDto(
                    id = row[AdoptionRequests.id],
                    petId = row[AdoptionRequests.petId],
                    adopterId = row[AdoptionRequests.adopterId],
                    message = row[AdoptionRequests.message],
                    status = row[AdoptionRequests.status],
                    createdAt = row[AdoptionRequests.createdAt]
                )
            }
    }

    override fun addImage(petId: Int, imageUrl: String, isPrimary: Boolean, sortOrder: Int): PetImageDto = transaction {
        if (isPrimary) {
            PetImages.update({ PetImages.petId eq petId }) {
                it[PetImages.isPrimary] = false
            }
        }
        val maxOrder = PetImages.selectAll()
            .where { PetImages.petId eq petId }
            .orderBy(PetImages.sortOrder, SortOrder.DESC)
            .limit(1)
            .map { it[PetImages.sortOrder] }
            .firstOrNull() ?: -1

        val id = PetImages.insert {
            it[PetImages.petId] = petId
            it[PetImages.imageUrl] = imageUrl
            it[PetImages.isPrimary] = isPrimary
            it[PetImages.sortOrder] = if (sortOrder > 0) sortOrder else maxOrder + 1
        } get PetImages.id

        PetImageDto(
            id = id,
            imageUrl = imageUrl,
            isPrimary = isPrimary,
            sortOrder = if (sortOrder > 0) sortOrder else maxOrder + 1
        )
    }

    override fun removeImage(petId: Int, imageId: Int): Boolean = transaction {
        val image = PetImages.selectAll()
            .where { (PetImages.id eq imageId) and (PetImages.petId eq petId) }
            .firstOrNull()
        if (image != null) {
            exec("DELETE FROM pet_images WHERE id = ?", listOf(IntegerColumnType() to imageId))
            true
        } else {
            false
        }
    }

    override fun setPrimaryImage(petId: Int, imageId: Int): Boolean = transaction {
        val image = PetImages.selectAll()
            .where { (PetImages.id eq imageId) and (PetImages.petId eq petId) }
            .firstOrNull()
        if (image != null) {
            PetImages.update({ PetImages.petId eq petId }) {
                it[PetImages.isPrimary] = false
            }
            PetImages.update({ PetImages.id eq imageId }) {
                it[PetImages.isPrimary] = true
            }
            true
        } else {
            false
        }
    }

    override fun getImages(petId: Int): List<PetImageDto> = getPetImages(petId)
}
