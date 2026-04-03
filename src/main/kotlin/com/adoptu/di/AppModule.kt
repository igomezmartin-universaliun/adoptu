package com.adoptu.di

import com.adoptu.adapters.db.repositories.*
import com.adoptu.adapters.notification.SesEmailAdapter
import com.adoptu.adapters.storage.S3ImageStorageAdapter
import com.adoptu.ports.*
import com.adoptu.services.*
import com.adoptu.services.auth.WebAuthnService
import com.adoptu.services.validation.*
import io.ktor.server.config.*
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun appModule(config: ApplicationConfig) = module {
    single { config }
    single<Clock> { Clock.System }
    single { WebAuthnService(get(), get(), get(), config.propertyOrNull("admin.email")?.getString() ?: "admin@adopt-u.com", config.propertyOrNull("webauthn.rpId")?.getString() ?: "localhost", config.propertyOrNull("webauthn.rpName")?.getString() ?: "Adopt-U Pet Adoption", config.propertyOrNull("webauthn.origin")?.getString() ?: "http://localhost:8080") }
    single<PetRepositoryPort> { PetRepositoryImpl(get()) }
    single<UserRepositoryPort> { UserRepository(get()) }
    single<PhotographerRepositoryPort> { PhotographerRepositoryImpl(get(), get(), get()) }
    single<TemporalHomeRepositoryPort> { TemporalHomeRepositoryImpl(get(), get(), get()) }
    single<ShelterRepositoryPort> { ShelterRepository(get()) }
    single<SterilizationLocationRepositoryPort> { SterilizationLocationRepository(get()) }
    single<ImageStoragePort> { createImageStorageAdapter(config) }
    single<NotificationPort> { SesEmailAdapter(get()) }
    single<PhotographerService> { PhotographerService(get(), get(), get(), get()) }
    single<UserService> { UserService(get()) }
    single<PetService> { PetService(get(), get(), get(), get()) }
    single<TemporalHomeService> { TemporalHomeService(get(), get(), get(), get()) }
    single { ShelterService(get()) }
    single { SterilizationLocationService(get()) }
    single { EmailVerificationService(get(), get(), get()) }
    single { PasswordService(get(), get(), get()) }
    single { MagicLinkService(get(), get(), get()) }
    single { EmailChangeService(get(), get(), get()) }
    single { UsersValidationService() }
    single { PetsValidationService() }
    single { PhotographersValidationService() }
    single { SheltersValidationService() }
    single { SterilizationLocationsValidationService() }
    single { TemporalHomesValidationService() }
    single { AuthValidationService() }
}

internal fun createImageStorageAdapter(config: ApplicationConfig): ImageStoragePort {
    val env = config.propertyOrNull("env")?.getString() ?: "prod"
    val prefix = "storage.$env"

    val bucketName = config.property("$prefix.bucket").getString()
    val region = config.propertyOrNull("$prefix.region")?.getString() ?: "us-east-1"
    val accessKeyId = config.propertyOrNull("$prefix.access_key_id")?.getString()
    val secretAccessKey = config.propertyOrNull("$prefix.secret_access_key")?.getString()
    val endpoint = config.propertyOrNull("$prefix.endpoint")?.getString()
    val pathStyleAccess = config.propertyOrNull("$prefix.path_style_access")?.getString()?.toBoolean() ?: false

    return S3ImageStorageAdapter(
        bucketName = bucketName,
        region = region,
        accessKeyId = accessKeyId,
        secretAccessKey = secretAccessKey,
        endpoint = endpoint,
        pathStyleAccess = pathStyleAccess
    )
}
