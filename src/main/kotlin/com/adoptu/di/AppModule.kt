package com.adoptu.di

import com.adoptu.adapters.storage.S3ImageStorageAdapter
import com.adoptu.adapters.notification.SnsNotificationAdapter
import com.adoptu.auth.WebAuthnService
import com.adoptu.ports.ImageStoragePort
import com.adoptu.ports.NotificationPort
import com.adoptu.repositories.PetRepository
import com.adoptu.services.PetService
import com.adoptu.services.PhotographerService
import com.adoptu.services.TemporalHomeService
import com.adoptu.services.UserService
import io.ktor.server.config.*
import org.koin.dsl.module

val appModule = module {
    single { WebAuthnService }
    single { PetRepository }
    single { UserService }
    single<ImageStoragePort> { createImageStorageAdapter(get()) }
    single<NotificationPort> { SnsNotificationAdapter(get()) }
    single<PetService> { PetService(get(), get(), get()) }
    single { PhotographerService }
    single { PhotographerService.apply { setNotificationAdapter(get()) } }
    single { TemporalHomeService }
    single { TemporalHomeService.apply { setNotificationAdapter(get()) } }
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
