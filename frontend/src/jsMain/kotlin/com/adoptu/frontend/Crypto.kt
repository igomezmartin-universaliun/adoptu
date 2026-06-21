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
    private var cachedPublicKeyPromise: Promise<String>? = null

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

    fun encrypt(plaintext: String, publicKeyPem: String): Promise<String?> {
        return Promise<dynamic> { resolve, reject ->
            try {
                val keyData = base64ToArrayBuffer(pemToBase64(publicKeyPem))
                crypto.subtle.importKey(
                    "spki",
                    keyData,
                    js("({name: 'RSA-OAEP', hash: 'SHA-256'})"),
                    false,
                    js("['encrypt']")
                ).then { publicKey ->
                    val encoder = js("new TextEncoder()")
                    val data = encoder.encode(plaintext)
                    crypto.subtle.encrypt(
                        js("({name: 'RSA-OAEP', hash: 'SHA-256'})"),
                        publicKey,
                        data
                    ).then { encrypted ->
                        resolve(arrayBufferToBase64(encrypted))
                    }.catch { e ->
                        console.error("Encryption failed: $e")
                        resolve(null)
                    }
                }.catch { e ->
                    console.error("Key import failed: $e")
                    resolve(null)
                }
            } catch (e: Throwable) {
                console.error("Encryption failed: $e")
                resolve(null)
            }
        }
    }

    fun getPublicKey(): Promise<String> {
        if (cachedPublicKeyPromise != null) {
            return cachedPublicKeyPromise!!
        }
        cachedPublicKeyPromise = fetch("/api/auth/encryption-key")
            .then { res -> res.json() }
            .then { data -> data.publicKey as String }
        return cachedPublicKeyPromise!!
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
