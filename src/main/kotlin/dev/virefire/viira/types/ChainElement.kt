package dev.virefire.viira.types

import dev.virefire.viira.Middleware
import dev.virefire.viira.Router

open class ChainElement(val path: List<RouteEntry>) {
    fun check(req: Request, exact: Boolean, prefix: List<RouteEntry>): Boolean {
        val result: RouteEntry.CheckResult =
            RouteEntry.check(prefix + path, req.path)
        if (!result.match) return false
        if (!result.exact && exact) return false
        if (exact) {
            result.params.forEach { req.params[it.key] = it.value }
            req.wildcardMatches.addAll(result.wildcard)
        }
        return true
    }

    class MethodHandler(path: String, val type: Method, val middlewares: List<Middleware>, val handler: RequestHandler) :
        ChainElement(RouteEntry.parse(path)) {
        suspend fun execute(req: Request, res: Response) {
            var i = 0
            var callback: (suspend () -> Unit)? = null
            val ctx = MiddlewareContext(req, res) {
                callback?.invoke()
            }
            val nextCalled = mutableListOf<Int>()
            val next = suspend {
                if (i < middlewares.size) {
                    i++
                    middlewares[i - 1].handler(ctx)
                } else {
                    handler(ctx)
                }
            }
            callback = suspend {
                if (!nextCalled.contains(i)){
                    nextCalled.add(i)
                    next()
                }
            }
            next()
        }
    }

    class SubRouter(path: String, val router: Router) : ChainElement(RouteEntry.parse(path))

    class GlobalMiddleware(path: String, val middleware: Middleware) : ChainElement(RouteEntry.parse(path)) {
        constructor(middleware: Middleware) : this("", middleware)
    }
}