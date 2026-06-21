package com.adoptu.frontend

import kotlinx.browser.window
import kotlin.js.Promise
import kotlin.js.json

data class FetchResult(val ok: Boolean, val statusText: String, val text: String)

fun doFetch(path: String, options: dynamic = null): Promise<FetchResult> {
    val opts = options ?: js("({})")
    if (opts.credentials == null) {
        opts.credentials = "include"
    }
    return window.asDynamic().fetch(path, opts).then { response ->
        try {
            val res = response.unsafeCast<dynamic>()
            res.text().then { text ->
                js("({ok: res.ok, statusText: res.statusText, text: text})")
            }
        } catch (e: dynamic) {
            js("({ok: false, statusText: 'error', text: ''})")
        }
    }
}

fun fetchJson(path: String, options: dynamic = null): Promise<dynamic> {
    return doFetch(path, options).then { result ->
        if (result.text.isNotEmpty()) {
            try {
                JSON.parse<dynamic>(result.text)
            } catch (e: Exception) {
                js("({})")
            }
        } else {
            js("({})")
        }
    }
}