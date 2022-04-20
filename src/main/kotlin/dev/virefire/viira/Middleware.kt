package dev.virefire.viira

import dev.virefire.viira.types.*

fun middleware (m: MiddlewareHandler): Middleware {
    return Middleware(m)
}

class Middleware(val handler: MiddlewareHandler)