package com.adoptu.mocks

import com.adoptu.ports.NotificationPort

class MockNotificationAdapter : NotificationPort {
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
        val body = "Request from: $requesterName, Pet: $petName, Message: $message"
        return sendEmail(photographerEmail, subject, body)
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
        val body = "Request from: $rescuerName, Pet: $petName, Message: $message\nSpam Report Link: $spamReportLink"
        return sendEmail(temporalHomeEmail, subject, body)
    }

    fun getSentEmails(): List<EmailRecord> = sentEmails.toList()

    fun clear() {
        sentEmails.clear()
        shouldFail = false
    }
}
