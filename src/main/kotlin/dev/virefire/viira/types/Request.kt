package dev.virefire.viira.types

import dev.virefire.kson.KSON
import dev.virefire.kson.ParsedElement
import io.ktor.server.application.*
import java.io.InputStream
import java.net.HttpCookie
import java.util.Date

data class Request (
    val url: String,
    val path: String,
    val host: String,
    val stream: InputStream,
    val ip: String,
    val timestamp: Date,
    val headers: Headers,
    val originalHeaders: io.ktor.http.Headers,
    val method: Method,
    val query: MutableMap<String, String>,
    val cookies: MutableMap<String, HttpCookie>,
    val params: MutableMap<String, String>,
    val meta: MutableMap<String, Any>,
    val wildcardMatches: MutableList<String>,
    val originalCall: ApplicationCall,
    private val silentJson: Boolean
) {
    private var jsonBody: ParsedElement? = null

    val json: ParsedElement
        get() {
            if (jsonBody == null) {
                jsonBody = if (silentJson)
                    KSON.parse(stream.reader()).silent
                else
                    KSON.parse(stream.reader())
            }

            return jsonBody!!
        }

    private var body: ByteArray? = null

    val bytes: ByteArray
        get() {
            if (body == null) {
                body = stream.readBytes()
            }

            return body!!
        }
}