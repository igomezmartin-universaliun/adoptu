package com.adoptu.frontend

import kotlin.js.json

external interface RequestInit {
    var method: String
    var headers: dynamic
    var body: dynamic
    var credentials: String
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun RequestInit(): RequestInit = js("({})") as RequestInit

object ApiClient {
    private val base = ""

    private suspend fun request(path: String, options: RequestInit? = null): dynamic {
        val init = options ?: RequestInit()
        init.credentials = "include"
        val response = fetch(base + path, init).awaitCommon()
        val text = response.text().awaitCommon()
        val data = if (text.isNotEmpty()) {
            try {
                JSON.parse<dynamic>(text)
            } catch (e: Exception) {
                js("({})")
            }
        } else {
            js("({})")
        }
        if (!response.ok) {
            throw Error((data as dynamic)?.error?.toString() ?: response.statusText)
        }
        return data
    }

    suspend fun me(): dynamic = request("/api/auth/me")
    suspend fun logout(): dynamic = request("/api/auth/logout", RequestInit().apply { method = "POST" })

    suspend fun getPets(type: String? = null): dynamic = request("/api/pets" + (type?.let { "?type=${js("encodeURIComponent")(it)}" } ?: ""))
    suspend fun getPet(id: String): dynamic = request("/api/pets/$id")

    suspend fun createPet(pet: dynamic): dynamic = request("/api/pets", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(pet)
    })

    suspend fun updatePet(id: String, pet: dynamic): dynamic = request("/api/pets/$id", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(pet)
    })

    suspend fun deletePet(id: String): dynamic = request("/api/pets/$id", RequestInit().apply { method = "DELETE" })

    suspend fun adoptPet(id: String, message: String): dynamic = request("/api/pets/$id/adopt", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("message" to (message ?: "")))
    })

    suspend fun addImage(petId: String, file: dynamic, isPrimary: Boolean = false): dynamic {
        val formData = js("new FormData()")
        formData.append("file", file)
        formData.append("isPrimary", isPrimary.toString())
        return request("/api/pets/$petId/images", RequestInit().apply {
            method = "POST"
            body = formData
        })
    }

    suspend fun removeImage(petId: String, imageId: String): dynamic = request("/api/pets/$petId/images/$imageId", RequestInit().apply { method = "DELETE" })
    suspend fun setPrimaryImage(petId: String, imageId: String): dynamic = request("/api/pets/$petId/images/$imageId/primary", RequestInit().apply { method = "PUT" })

    suspend fun getAdoptionRequests(petId: String): dynamic = request("/api/pets/$petId/adoption-requests")
    suspend fun updateAdoptionRequest(requestId: String, status: String): dynamic = request("/api/pets/adoption-requests/$requestId", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/x-www-form-urlencoded")
        body = "status=${js("encodeURIComponent")(status)}"
    })

    suspend fun getMyAdoptionRequests(): dynamic = request("/api/pets/my-adoption-requests")
    suspend fun getPhotographers(): dynamic = request("/api/photographers")

    suspend fun updateProfile(displayName: String): dynamic = request("/api/users/profile", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("displayName" to displayName))
    })

    suspend fun updatePhotographerSettings(fee: Double, currency: String, country: String, state: String): dynamic = request("/api/photographers/settings", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("photographerFee" to fee, "photographerCurrency" to currency, "country" to country, "state" to state))
    })

    suspend fun createPhotographyRequest(photographerId: Int, petId: String?, message: String): dynamic = request("/api/photographers/requests", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("photographerId" to photographerId, "petId" to (petId ?: null), "message" to message))
    })

    suspend fun createMultiplePhotographyRequests(photographerIds: Array<Int>, petId: String?, message: String): dynamic = request("/api/photographers/requests/multiple", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("photographerIds" to photographerIds, "petId" to (petId ?: null), "message" to message))
    })

    suspend fun getPhotographyRequests(): dynamic = request("/api/photographers/requests")
    suspend fun updatePhotographyRequest(requestId: String, status: String? = null, scheduledDate: String? = null): dynamic {
        val bodyObj = js("({})")
        status?.let { bodyObj.status = it }
        scheduledDate?.let { bodyObj.scheduledDate = it }
        return request("/api/photographers/requests/$requestId", RequestInit().apply {
            method = "PUT"
            headers = json("Content-Type" to "application/json")
            body = JSON.stringify(bodyObj)
        })
    }

    suspend fun activateRescuer(activate: Boolean): dynamic = request("/api/users/rescuer-profile", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("activate" to activate))
    })

    suspend fun activatePhotographer(activate: Boolean): dynamic = request("/api/photographers/profile", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("activate" to activate))
    })

    suspend fun activateTemporalHome(activate: Boolean): dynamic = request("/api/users/temporal-home-profile", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("activate" to activate))
    })

    suspend fun createTemporalHome(data: dynamic): dynamic = request("/api/users/temporal-home", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(data)
    })

    suspend fun updateTemporalHome(data: dynamic): dynamic = request("/api/users/temporal-home", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(data)
    })

    suspend fun getTemporalHome(): dynamic = request("/api/users/temporal-home")
    suspend fun getTemporalHomeRequests(): dynamic = request("/api/users/temporal-home/requests")

    suspend fun blockRescuer(rescuerId: String): dynamic = request("/api/temporal-homes/block", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("rescuerId" to rescuerId))
    })

    suspend fun updateLanguage(language: String): dynamic = request("/api/users/language", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("language" to language))
    })

    suspend fun hasPassword(): dynamic = request("/api/users/has-password")

    suspend fun setPassword(encryptedPassword: String): dynamic = request("/api/users/password", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("encryptedPassword" to encryptedPassword))
    })

    suspend fun changePassword(encryptedCurrent: String, encryptedNew: String): dynamic = request("/api/users/password", RequestInit().apply {
        method = "PUT"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("encryptedCurrentPassword" to encryptedCurrent, "encryptedNewPassword" to encryptedNew))
    })

    suspend fun requestEmailChange(newEmail: String): dynamic = request("/api/users/request-email-change", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("newEmail" to newEmail))
    })

    suspend fun requestMagicLink(encryptedData: String): dynamic = request("/api/auth/request-magic-link", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("encryptedData" to encryptedData))
    })

    suspend fun loginWithPassword(email: String, encryptedPassword: String): dynamic = request("/api/auth/login-with-password", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("email" to email, "encryptedPassword" to encryptedPassword))
    })

    suspend fun forgotPassword(encryptedData: String): dynamic = request("/api/auth/forgot-password", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("encryptedData" to encryptedData))
    })

    suspend fun resetPassword(token: String, encryptedPassword: String): dynamic = request("/api/auth/reset-password?token=${js("encodeURIComponent")(token)}", RequestInit().apply {
        method = "POST"
        headers = json("Content-Type" to "application/json")
        body = JSON.stringify(json("encryptedData" to encryptedPassword))
    })
}

external class Error(message: String) : Throwable
