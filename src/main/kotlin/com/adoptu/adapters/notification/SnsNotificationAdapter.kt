package com.adoptu.adapters.notification

import com.adoptu.ports.NotificationPort
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

class SnsNotificationAdapter(config: ApplicationConfig) : NotificationPort {

    private val snsClient: SnsClient? = try {
        val region = config.propertyOrNull("sns.region")?.getString() ?: "us-east-1"
        val accessKeyId = config.propertyOrNull("sns.access_key_id")?.getString()
        val secretAccessKey = config.propertyOrNull("sns.secret_access_key")?.getString()
        val endpoint = config.propertyOrNull("sns.endpoint")?.getString()
        
        val builder = SnsClient.builder().region(Region.of(region))
        
        if (!accessKeyId.isNullOrBlank() && !secretAccessKey.isNullOrBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            ))
        }
        
        if (!endpoint.isNullOrBlank()) {
            builder.endpointOverride(java.net.URI.create(endpoint))
        }
        
        builder.build()
    } catch (e: Exception) {
        println("Failed to create SNS client: ${e.message}")
        null
    }

    private val topicArn = config.propertyOrNull("sns.topic_arn")?.getString()

    private val isConfigured: Boolean
        get() = snsClient != null && !topicArn.isNullOrBlank()

    override suspend fun sendEmail(to: String, subject: String, body: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            println("SNS not configured - would have sent notification: $subject")
            return@withContext false
        }

        try {
            val message = buildString {
                appendLine("Subject: $subject")
                appendLine()
                appendLine(body)
            }

            val request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .subject(subject)
                .build()

            snsClient?.publish(request)
            true
        } catch (e: Exception) {
            println("Failed to send SNS notification: ${e.message}")
            false
        }
    }

    override suspend fun sendPhotographerRequest(
        photographerEmail: String,
        photographerName: String,
        requesterName: String,
        petName: String?,
        message: String,
        fee: Double?,
        currency: String?
    ): Boolean {
        val subject = "New Photography Session Request - Adopt-U"
        val body = buildString {
            appendLine("Hello $photographerName,")
            appendLine()
            appendLine("You have received a new photography session request!")
            appendLine()
            appendLine("Request from: $requesterName")
            if (petName != null) {
                appendLine("Pet: $petName")
            }
            if (fee != null && fee > 0) {
                appendLine("Offered fee: $currency $fee")
            }
            appendLine()
            appendLine("Message:")
            appendLine(message)
            appendLine()
            appendLine("Log in to your account to respond to this request.")
            appendLine()
            appendLine("Best regards,")
            appendLine("The Adopt-U Team")
        }
        return sendEmail(photographerEmail, subject, body)
    }

    override suspend fun sendAdoptionRequestNotification(
        rescuerEmail: String,
        petName: String,
        adopterName: String,
        message: String?
    ): Boolean {
        val subject = "New Adoption Request for $petName"
        val body = buildString {
            appendLine("Hello,")
            appendLine()
            appendLine("You have received a new adoption request for $petName.")
            appendLine()
            appendLine("Adopter: $adopterName")
            if (!message.isNullOrBlank()) {
                appendLine()
                appendLine("Message:")
                appendLine(message)
            }
            appendLine()
            appendLine("Log in to your account to review the request.")
            appendLine()
            appendLine("Best regards,")
            appendLine("The Adopt-U Team")
        }
        return sendEmail(rescuerEmail, subject, body)
    }

    override suspend fun sendTemporalHomeRequest(
        temporalHomeEmail: String,
        temporalHomeAlias: String,
        rescuerName: String,
        petName: String?,
        message: String,
        spamReportLink: String
    ): Boolean {
        val subject = "New Pet Care Help Request - Adopt-U"
        val body = buildString {
            appendLine("Hello $temporalHomeAlias,")
            appendLine()
            appendLine("You have received a new pet care help request!")
            appendLine()
            appendLine("Request from: $rescuerName")
            if (petName != null) {
                appendLine("Pet: $petName")
            }
            appendLine()
            appendLine("Message:")
            appendLine(message)
            appendLine()
            appendLine("---")
            appendLine("If you want to block this rescuer from sending you more requests, click here:")
            appendLine(spamReportLink)
            appendLine()
            appendLine("Best regards,")
            appendLine("The Adopt-U Team")
        }
        return sendEmail(temporalHomeEmail, subject, body)
    }
}
