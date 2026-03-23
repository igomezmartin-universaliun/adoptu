package com.adoptu.di

import com.adoptu.adapters.storage.S3ImageStorageAdapter
import com.adoptu.adapters.notification.SnsNotificationAdapter
import com.adoptu.services.auth.WebAuthnService
import com.adoptu.ports.ImageStoragePort
import com.adoptu.ports.NotificationPort
import com.adoptu.ports.PetRepositoryPort
import com.adoptu.ports.PhotographerRepositoryPort
import com.adoptu.ports.TemporalHomeRepositoryPort
import com.adoptu.ports.UserRepositoryPort
import com.adoptu.adapters.db.repositories.PetRepositoryImpl
import com.adoptu.adapters.db.repositories.PhotographerRepositoryImpl
import com.adoptu.adapters.db.repositories.TemporalHomeRepositoryImpl
import com.adoptu.adapters.db.repositories.UserRepository
import com.adoptu.services.PetService
import com.adoptu.services.PhotographerService
import com.adoptu.services.TemporalHomeService
import com.adoptu.services.UserService
import io.ktor.server.config.*
import org.koin.dsl.module

val appModule = module {
    single { WebAuthnService }
    single<PetRepositoryPort> { PetRepositoryImpl() }
    single<UserRepositoryPort> { UserRepository() }
    single<PhotographerRepositoryPort> { PhotographerRepositoryImpl(get(), get()) }
    single<TemporalHomeRepositoryPort> { TemporalHomeRepositoryImpl(get(), get()) }
    single<ImageStoragePort> { createImageStorageAdapter(get()) }
    single<NotificationPort> { SnsNotificationAdapter(get()) }
    single<PhotographerService> { PhotographerService(get(), get(), get()) }
    single<UserService> { UserService(get(), get()) }
    single<PetService> { PetService(get(), get(), get(), get()) }
    single<TemporalHomeService> { TemporalHomeService(get(), get(), get()) }
}

private fun createImageStorageAdapter(config: ApplicationConfig): ImageStoragePort {
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
