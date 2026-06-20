package com.adoptu.frontend

import kotlinx.browser.window
import org.w3c.dom.crypto.Crypto
import org.w3c.dom.SubtleCrypto
import org.w3c.files.BlobPropertyBag
import kotlin.js.Promise

external val crypto: Crypto
external interface Crypto {
    val subtle: SubtleCrypto
}

external interface SubtleCrypto {
    fun importKey(format: String, keyData: dynamic, algorithm: dynamic, extractable: Boolean, keyUsages: Array<String>): Promise<dynamic>
    fun encrypt(algorithm: dynamic, key: dynamic, data: dynamic): Promise<dynamic>
}

external fun atob(data: String): String
external fun btoa(data: String): String

object RsaCrypto {
    private var cachedPublicKey: String? = null

    private fun pemToBase64(pem: String): String = pem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace(Regex("\\s"), "")

    private fun base64ToArrayBuffer(base64: String): dynamic {
        val binary = atob(base64)
        val bytes = js("new Uint8Array(binary.length)")
        for (i in 0 until binary.length) {
            bytes[i] = binary.charCodeAt(i).toInt()
        }
        return bytes.buffer
    }

    private fun arrayBufferToBase64(buffer: dynamic): String {
        val bytes = js("new Uint8Array(buffer)")
        var binary = ""
        for (i in 0 until bytes.length) {
            binary += js("String.fromCharCode")(bytes[i])
        }
        return btoa(binary)
    }

    suspend fun encrypt(plaintext: String, publicKeyPem: String): String? {
        return try {
            val keyData = base64ToArrayBuffer(pemToBase64(publicKeyPem))
            val publicKey = crypto.subtle.importKey(
                "spki",
                keyData,
                js("({name: 'RSA-OAEP', hash: 'SHA-256'})"),
                false,
                js("['encrypt']")
            ).awaitCrypto()

            val encoder = js("new TextEncoder()")
            val data = encoder.encode(plaintext)

            val encrypted = crypto.subtle.encrypt(
                js("({name: 'RSA-OAEP', hash: 'SHA-256'})"),
                publicKey,
                data
            ).awaitCrypto()

            arrayBufferToBase64(encrypted)
        } catch (e: Throwable) {
            console.error("Encryption failed: $e")
            null
        }
    }

    suspend fun getPublicKey(): String {
        cachedPublicKey?.let { return it }
        val response = fetch("/api/auth/encryption-key").awaitFetch()
        val data = response.json().awaitCrypto()
        val key = data.publicKey as String
        cachedPublicKey = key
        return key
    }
}

external fun fetch(resource: String): Promise<FetchResponse>
external interface FetchResponse {
    val ok: Boolean
    fun json(): Promise<dynamic>
    fun text(): Promise<String>
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

external object console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
    fun warn(vararg args: Any?)
}
