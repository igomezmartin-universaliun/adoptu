package com.adoptu.mocks

import com.adoptu.services.EmailService
import io.ktor.server.config.*

class MockEmailService : EmailService(MapApplicationConfig()) {
    private val sentEmails = mutableListOf<EmailRecord>()
    private var shouldFail = false

    data class EmailRecord(
        val to: String,
        val subject: String,
        val body: String
    )

    fun setFailMode(fail: Boolean) {
        shouldFail = fail
    }

    override suspend fun sendEmail(to: String, subject: String, body: String): Boolean {
        if (shouldFail) {
            return false
        }
        sentEmails.add(EmailRecord(to, subject, body))
        return true
    }

    override suspend fun sendAdoptionRequestNotification(
        rescuerEmail: String,
        petName: String,
        adopterName: String,
        message: String?
    ): Boolean {
        val subject = "New Adoption Request for $petName"
        val body = """
            Hello,
            
            You have received a new adoption request for $petName.
            
            Adopter: $adopterName
            Message: $message
            
            Please login to review and respond to this request.
        """.trimIndent()
        
        return sendEmail(rescuerEmail, subject, body)
    }

    fun getSentEmails(): List<EmailRecord> = sentEmails.toList()

    fun clear() {
        sentEmails.clear()
        shouldFail = false
    }
}
