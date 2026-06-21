package com.adoptu.frontend

import kotlinx.browser.window
import org.w3c.dom.Navigator
import org.w3c.dom.credentials.CredentialCreationOptions
import org.w3c.dom.credentials.CredentialsContainer
import kotlin.js.Promise
import kotlin.js.json

external val navigator: Navigator
external interface Navigator {
    val credentials: CredentialsContainer
}

external interface CredentialsContainer {
    fun create(options: dynamic): Promise<dynamic>
    fun get(options: dynamic): Promise<dynamic>
}

external interface PublicKeyCredential {
    val id: String
    val rawId: dynamic
    val type: String
    val response: AuthenticatorResponse
    fun toJSON(): dynamic
}

external interface AuthenticatorResponse {
    val clientDataJSON: dynamic
    val authenticatorData: dynamic
    val signature: dynamic
    val userHandle: dynamic
}

external object PublicKeyCredentialCompanion {
    fun parseCreationOptionsFromJSON(opts: dynamic): dynamic
    fun parseRequestOptionsFromJSON(opts: dynamic): dynamic
}

object WebAuthn {
    suspend fun register(email: String, displayName: String, roles: List<String>, temporalHomeProfile: dynamic? = null): dynamic {
        val optsRes = fetch("/api/auth/registration-options", {
            method: "POST",
            headers: js("{'Content-Type': 'application/x-www-form-urlencoded'}"),
            body: "email=" + encodeURIComponent(email) + "&displayName=" + encodeURIComponent(displayName)
        }).awaitFetch()
        if (!optsRes.ok) {
            val error = optsRes.json().awaitCrypto()
            throw Error(error.error ?: "Registration failed")
        }
        val options = optsRes.json().awaitCrypto()
        val credential = navigator.credentials.create(json("publicKey" to parseCreationOptions(options))).awaitCrypto()

        val body = js("new URLSearchParams()")
        body.append("email", email)
        body.append("displayName", displayName)
        body.append("roles", roles.joinToString(","))
        body.append("registrationResponse", JSON.stringify(credential.toJSON?.invoke() ?: toJSON(credential)))

        temporalHomeProfile?.let {
            body.append("temporalHomeProfile", JSON.stringify(it))
        }

        val regRes = fetch("/api/auth/register", js("({method: 'POST', headers: {'Content-Type': 'application/x-www-form-urlencoded'}, body: body.toString(), credentials: 'include'})")).awaitFetch()
        return regRes.json().awaitCrypto()
    }

    suspend fun authenticate(): Boolean {
        val result = authenticateWithResponse()
        return (result as dynamic).success == true
    }

    suspend fun authenticateWithResponse(): dynamic {
        val optsRes = fetch("/api/auth/assertion-options", js("({credentials: 'include'})")).awaitFetch()
        val options = optsRes.json().awaitCrypto()
        val credential = navigator.credentials.get(json("publicKey" to parseRequestOptions(options))).awaitCrypto()
        if (credential == null) {
            throw Error("Passkey authentication was cancelled")
        }
        val authRes = fetch("/api/auth/authenticate", js("({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(credential.toJSON?.invoke() ?: toJSON(credential)), credentials: 'include'})")).awaitFetch()
        return authRes.json().awaitCrypto()
    }

    private fun parseCreationOptions(opts: dynamic): dynamic {
        val hasParse = js("window.PublicKeyCredential && window.PublicKeyCredential.parseCreationOptionsFromJSON")
        if (hasParse == true) {
            return js("PublicKeyCredential.parseCreationOptionsFromJSON")(opts)
        }
        return json(
            "challenge" to base64ToArray(opts.challenge as String),
            "rp" to opts.rp,
            "user" to js("Object.assign({}, opts.user, {id: base64ToArray(opts.user.id)})"),
            "pubKeyCredParams" to (opts.pubKeyCredParams ?: js("[{type: 'public-key', alg: -7}, {type: 'public-key', alg: -257}]"))
        )
    }

    private fun parseRequestOptions(opts: dynamic): dynamic {
        val hasParse = js("window.PublicKeyCredential && window.PublicKeyCredential.parseRequestOptionsFromJSON")
        if (hasParse == true) {
            return js("PublicKeyCredential.parseRequestOptionsFromJSON")(opts)
        }
        return json(
            "challenge" to base64ToArray(opts.challenge as String),
            "rpId" to (opts.rpId ?: "localhost")
        )
    }

    private fun base64ToArray(s: String): dynamic {
        val bin = atob(s.replace(Regex("-"), "+").replace(Regex("_"), "/"))
        val arr = js("new Uint8Array(bin.length)")
        for (i in 0 until bin.length) {
            arr[i] = bin.charCodeAt(i).toInt()
        }
        return arr
    }

    private fun arrayToBase64(arr: dynamic): String {
        var bin = ""
        for (i in 0 until arr.length) {
            bin += js("String.fromCharCode")(arr[i])
        }
        return btoa(bin).replace(Regex("\\+"), "-").replace(Regex("/"), "_").replace(Regex("=+$"), "")
    }

    private fun toJSON(cred: dynamic): dynamic {
        val rawId = arrayToBase64(js("new Uint8Array")(cred.rawId))
        val response = cred.response
        return json(
            "id" to cred.id,
            "rawId" to rawId,
            "type" to cred.type,
            "response" to json(
                "clientDataJSON" to arrayToBase64(js("new Uint8Array")(response.clientDataJSON)),
                "authenticatorData" to (response.authenticatorData?.let { arrayToBase64(js("new Uint8Array")(it)) } ?: undefined),
                "signature" to arrayToBase64(js("new Uint8Array")(response.signature)),
                "userHandle" to (response.userHandle?.let { arrayToBase64(js("new Uint8Array")(it)) } ?: undefined)
            )
        )
    }

    fun serializeCredential(credential: dynamic): String {
        val json = credential.toJSON?.invoke() ?: toJSON(credential)
        return JSON.stringify(json)
    }
}

external fun atob(data: String): String
external fun btoa(data: String): String
external val undefined: dynamic

external object JSON {
    fun stringify(value: dynamic): String
}

external fun fetch(resource: String, init: dynamic = definedExternally): Promise<FetchResponse>
external interface FetchResponse {
    val ok: Boolean
    fun json(): Promise<dynamic>
}

suspend fun Promise<*>.awaitCrypto(): dynamic = kotlin.coroutines.suspendCoroutine { cont ->
    this.asDynamic().then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

suspend fun Promise<FetchResponse>.awaitFetch(): FetchResponse = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}
