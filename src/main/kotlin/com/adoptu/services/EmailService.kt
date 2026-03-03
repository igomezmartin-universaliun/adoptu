package com.adoptu.services

import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.MimeMessage

open class EmailService(private val config: ApplicationConfig) {

    private val host = config.propertyOrNull("email.host")?.getString()
    private val port = config.propertyOrNull("email.port")?.getString()?.toIntOrNull() ?: 587
    private val username = config.propertyOrNull("email.username")?.getString()
    private val password = config.propertyOrNull("email.password")?.getString()
    private val from = config.propertyOrNull("email.from")?.getString() ?: "noreply@adopt-u.com"

    private val isConfigured: Boolean
        get() = !host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()

    open suspend fun sendEmail(to: String, subject: String, body: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            println("Email not configured - would have sent email to $to: $subject")
            return@withContext false
        }

        try {
            val properties = Properties().apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(javax.mail.internet.InternetAddress(from))
            message.addRecipient(Message.RecipientType.TO, javax.mail.internet.InternetAddress(to))
            message.subject = subject
            message.setText(body, "utf-8")

            Transport.send(message)
            true
        } catch (e: Exception) {
            println("Failed to send email: ${e.message}")
            false
        }
    }

    open suspend fun sendAdoptionRequestNotification(
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
}
