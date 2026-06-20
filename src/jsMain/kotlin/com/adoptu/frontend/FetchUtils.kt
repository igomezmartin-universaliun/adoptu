package com.adoptu.frontend

import kotlinx.browser.window
import org.w3c.dom.RequestCredentials
import org.w3c.dom.fetch.RequestInit
import org.w3c.dom.fetch.Response
import kotlin.js.Promise
import kotlin.js.json

external fun fetch(resource: String, init: RequestInit? = definedExternally): Promise<Response>

data class FetchResult(val ok: Boolean, val statusText: String, val text: String)

suspend fun doFetch(path: String, options: RequestInit? = null): FetchResult {
    val init = options ?: RequestInit()
    init.credentials = RequestCredentials.include
    val response = fetch(path, init).await()
    val text = response.text().await()
    return FetchResult(response.ok, response.statusText, text)
}

suspend fun fetchJson(path: String, options: RequestInit? = null): dynamic {
    val result = doFetch(path, options)
    return if (result.text.isNotEmpty()) {
        try {
            JSON.parse<dynamic>(result.text)
        } catch (e: Exception) {
            js("({})")
        }
    } else {
        js("({})")
    }
}

fun RequestInit.jsonBody(data: dynamic): RequestInit {
    this.method = "POST"
    this.headers = json("Content-Type" to "application/json")
    this.body = JSON.stringify(data)
    return this
}

fun RequestInit.formBody(body: String): RequestInit {
    this.method = "POST"
    this.headers = json("Content-Type" to "application/x-www-form-urlencoded")
    this.body = body
    return this
}

fun RequestInit.putJson(data: dynamic): RequestInit {
    this.method = "PUT"
    this.headers = json("Content-Type" to "application/json")
    this.body = JSON.stringify(data)
    return this
}

fun RequestInit.deleteInit(): RequestInit {
    this.method = "DELETE"
    return this
}

suspend fun <T> Promise<T>.await(): T = kotlin.coroutines.suspendCoroutine { cont ->
    this.then(
        onFulfilled = { cont.resume(it) },
        onRejected = { cont.resumeWith(Result.failure(it as Throwable)) }
    )
}

external object JSON {
    fun parse(text: String): dynamic
    fun <T> parse(text: String): T
    fun stringify(value: dynamic): String
}
