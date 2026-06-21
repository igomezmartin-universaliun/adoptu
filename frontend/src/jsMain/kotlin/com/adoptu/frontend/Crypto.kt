package com.adoptu.frontend

import kotlinx.browser.window
import kotlin.js.Promise

@JsExport
@JsName("rsaCrypto")
object RsaCryptoModule {
    private var cachedPublicKeyPromise: Promise<String>? = null

    fun getPublicKey(): Promise<String> {
        if (cachedPublicKeyPromise != null) return cachedPublicKeyPromise!!
        cachedPublicKeyPromise = window.asDynamic().fetch("/api/auth/encryption-key")
            .then { res -> res.json() }
            .then { data -> data.publicKey as String }
        return cachedPublicKeyPromise!!
    }

    fun encrypt(plaintext: String, publicKeyPem: String): Promise<String> {
        return importPublicKey(publicKeyPem).then<dynamic> { publicKey ->
            val encoder = js("new TextEncoder()")
            val data = encoder.encode(plaintext)
            window.asDynamic().crypto.subtle.encrypt(
                js("({name: 'RSA-OAEP', hash: 'SHA-256'})"),
                publicKey,
                data
            )
        }.then<dynamic> { encrypted ->
            arrayToBase64Url(encrypted)
        }
    }

    fun importPublicKey(pemKey: String): Promise<dynamic> {
        val base64 = pemKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        val binary = window.atob(base64)
        val len = binary.length
        val bytes = js("new Uint8Array(len)")
        for (i in 0 until len) {
            js("bytes[i] = binary.charCodeAt(i)")
        }
        val keyData = js("bytes.buffer")
        return window.asDynamic().crypto.subtle.importKey(
            "spki",
            keyData,
            js("({name: 'RSA-OAEP', hash: 'SHA-256'})"),
            false,
            js("['encrypt']")
        )
    }
}

fun arrayToBase64Url(buffer: dynamic): String {
    val bytes = js("new Uint8Array(buffer)")
    var binary = ""
    for (i in 0 until bytes.length) {
        binary += js("String.fromCharCode(bytes[i])")
    }
    return window.btoa(binary).replace("+", "-").replace("/", "_").replace("=", "")
}