package dev.virefire.viira

import dev.virefire.viira.types.*
import dev.virefire.viira.types.Headers
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpCookie
import java.net.URI
import java.net.URLDecoder
import java.util.*

suspend fun handle(
    config: AppConfig,
    appReq: ApplicationRequest,
    appRes: ApplicationResponse,
    execute: suspend (Int, List<RouteEntry>, Request, Response) -> Boolean,
    find: (List<RouteEntry>, Request) -> List<Method>
) {
    val requestHeaders = Headers()
    for (key in appReq.headers.names()) {
        requestHeaders[key] = appReq.headers.getAll(key)?.get(0) ?: ""
    }
    var ip = appReq.origin.remoteHost
    if (config.trustProxy && requestHeaders.containsKey(config.proxyHeader))
        ip = requestHeaders[config.proxyHeader]!!.split(",")[0].trim()
    val uri = URI(appReq.uri)
    val request = Request(
        url = appReq.uri,
        path = uri.path,
        host = uri.host ?: "",
        stream = appReq.receiveChannel().toInputStream(),
        ip = ip,
        timestamp = Date(),
        headers = requestHeaders,
        originalHeaders = appReq.headers,
        method = Method.valueOf(appReq.httpMethod.value.uppercase()),
        query = splitQuery(uri),
        cookies = parseCookie(appReq.headers),
        params = mutableMapOf(),
        meta = mutableMapOf(),
        wildcardMatches = mutableListOf(),
        originalCall = appReq.call,
        silentJson = config.silentJson,
    )
    val responseHeaders = Headers()
    if (!config.hideBranding) {
        responseHeaders["X-Powered-By".lowercase()] = "Viira v" + Viira.version
    }
    val response = Response(responseHeaders) { status, headers, bytes ->
        appRes.status(HttpStatusCode.fromValue(status))
        headers
            .filterNot { HttpHeaders.isUnsafe(it.key) }
            .forEach { appRes.header(it.key, it.value) }
            appRes.call.respondOutputStream(ContentType.parse(headers["Content-Type"] ?: "application/octet-stream")) {
                withContext(Dispatchers.IO) {
                    write(bytes)
                }
            }
    }
    if (config.handlePreflight && request.method == Method.OPTIONS) {
        val methods = find(listOf(), request)
        if (methods.isEmpty()) {
            response.status(404)
        } else {
            response.status(200)
            response.header("Access-Control-Allow-Methods", methods.joinToString(", "))
            response.header("Access-Control-Allow-Origin", "*")
            response.header("Access-Control-Allow-Headers", "*")
            response.header("Access-Control-Allow-Credentials", "true")
            response.header("Access-Control-Max-Age", "3600")
        }
        response.send()
        return
    }
    try {
        execute(0, listOf(), request, response)
    } catch (e: Throwable) {
        e.printStackTrace()
        if (!response.headersSent) {
            response.status(500)
            response.text("500 Internal Error\n${request.method.name} ${request.path}")
        }
        return
    }
    if (!response.headersSent) {
        response.status(404)
        response.text("404 Not Found\n${request.method.name} ${request.path}")
    }
}

fun splitQuery(uri: URI): MutableMap<String, String> {
    val queryPairs = mutableMapOf<String, String>()
    val query = uri.query ?: return queryPairs
    val pairs = query.split("&").toTypedArray()
    for (pair in pairs) {
        val idx = pair.indexOf("=")
        if (idx == -1) continue
        queryPairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] =
            URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
    }
    return queryPairs
}

private fun parseCookie(responseHeaders: io.ktor.http.Headers): MutableMap<String, HttpCookie> {
    val cookies = mutableListOf<HttpCookie>()
    if (responseHeaders.contains("Cookie")) responseHeaders.getAll("Cookie")!!
        .forEach { cookie: String? ->
            cookies.addAll(
                HttpCookie.parse(
                    cookie
                )
            )
        }
    val cookieMap = mutableMapOf<String, HttpCookie>()
    cookies.forEach { cookie: HttpCookie ->
        cookieMap[cookie.name] = cookie
    }
    return cookieMap
}