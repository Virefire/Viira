package dev.virefire.viira.types

import dev.virefire.kson.KSON
import dev.virefire.viira.exceptions.HeaderAlreadySentException

class Response(
    val headers: Headers,
    val sender: suspend (Int, Headers, ByteArray) -> Unit
) {
    var status = 200
        private set

    var headersSent = false
        private set

    suspend fun send(): Response {
        return send(ByteArray(0))
    }

    suspend fun send(bytes: ByteArray): Response {
        if (headersSent) throw HeaderAlreadySentException("Headers already sent")
        sender(status, headers, bytes)
        headersSent = true
        return this
    }

    fun status(status: Int): Response {
        if (headersSent) throw HeaderAlreadySentException("Headers already sent")
        this.status = status
        return this
    }

    fun type(type: String): Response {
        if (headersSent) throw HeaderAlreadySentException("Headers already sent")
        headers["Content-Type"] = type
        return this
    }

    fun header(name: String, value: String): Response {
        if (headersSent) throw HeaderAlreadySentException("Headers already sent")
        headers[name] = value
        return this
    }

    suspend fun body(response: ByteArray): Response {
        if (!headers.containsKey("Content-Type")) type("application/octet-stream")
        send(response)
        return this
    }

    suspend fun body(response: ByteArray, contentType: String): Response {
        type(contentType)
        send(response)
        return this
    }

    suspend fun body(response: String): Response {
        val bytes = response.toByteArray()
        if (!headers.containsKey("Content-Type")) type("text/html")
        send(bytes)
        return this
    }

    suspend fun body(response: String, contentType: String): Response {
        val bytes = response.toByteArray()
        type(contentType)
        send(bytes)
        return this
    }

    suspend fun json(obj: Any): Response {
        val response = KSON.stringify(obj)
        val bytes = response.toByteArray()
        type("application/json")
        send(bytes)
        return this
    }

    suspend fun text(text: String): Response {
        val bytes = text.toByteArray()
        type("text/plain")
        send(bytes)
        return this
    }

    suspend fun redirect(url: String): Response {
        status(302)
        headers["Location"] = url
        send()
        return this
    }

    suspend fun redirect(url: String, code: Int): Response {
        status(code)
        headers["Location"] = url
        send()
        return this
    }
}