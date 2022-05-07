package dev.virefire.viira

import dev.virefire.viira.types.*
import java.util.concurrent.atomic.AtomicBoolean

open class Router {
    private val routes = mutableListOf<ChainElement>()
    private val errorHandlers = mutableListOf<ErrorHandler>()

    fun get(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.GET, middlewares.asList(), handler))
    }

    fun post(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.POST, middlewares.asList(), handler))
    }

    fun put(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.PUT, middlewares.asList(), handler))
    }

    fun delete(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.DELETE, middlewares.asList(), handler))
    }

    fun patch(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.PATCH, middlewares.asList(), handler))
    }

    fun head(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.HEAD, middlewares.asList(), handler))
    }

    fun options(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.OPTIONS, middlewares.asList(), handler))
    }

    fun trace(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.TRACE, middlewares.asList(), handler))
    }

    fun connect(path: String, vararg middlewares: Middleware, handler: RequestHandler) {
        routes.add(ChainElement.MethodHandler(path, Method.CONNECT, middlewares.asList(), handler))
    }

    fun use(path: String, vararg middlewares: Middleware) {
        middlewares.forEach {
            routes.add(ChainElement.GlobalMiddleware(path, it))
        }
    }

    fun use(vararg middlewares: Middleware) {
        middlewares.forEach {
            routes.add(ChainElement.GlobalMiddleware(it))
        }
    }

    fun use(path: String, router: Router) {
        routes.add(ChainElement.SubRouter(path, router))
    }

    fun error(handler: ErrorHandler) {
        errorHandlers.add(handler)
    }

    private suspend fun handleError(request: Request, response: Response, error: Throwable) {
        var i = 0
        suspend fun next(error: Throwable) {
            if (i >= errorHandlers.size)
                throw error
            try {
                errorHandlers[i++](ErrorContext(request, response, error))
            } catch (e: Throwable) {
                next(e)
            }
        }
        next(error)
    }

    protected fun find(prefix: List<RouteEntry>, req: Request): List<Method> {
        val methods = mutableListOf<Method>()
        routes.forEach {
            if (it is ChainElement.MethodHandler && it.check(req, true, prefix)) {
                methods.add(it.type)
            } else if (it is ChainElement.SubRouter) {
                methods.addAll(it.router.find(prefix + it.path, req))
            }
        }
        return methods
    }

    protected suspend fun execute(ascent: Int, prefix: List<RouteEntry>, req: Request, res: Response): Boolean {
        try {
            for (i in routes.indices) {
                if (i < ascent) continue
                when (val route = routes[i]) {
                    is ChainElement.MethodHandler -> {
                        if (route.type == req.method && route.check(req, true, prefix)) {
                            route.execute(req, res)
                            return true
                        }
                    }
                    is ChainElement.GlobalMiddleware -> {
                        if (route.check(req, false, prefix)) {
                            val executed = AtomicBoolean(false)
                            val result = AtomicBoolean(false)
                            val ctx = MiddlewareContext(req, res) {
                                if (!executed.getAndSet(true) && i != routes.size - 1) {
                                    result.set(execute(i + 1, prefix, req, res))
                                }
                            }
                            route.middleware.handler(ctx)
                            return result.get()
                        }
                    }
                    is ChainElement.SubRouter -> {
                        if (route.check(req, false, prefix)) {
                            return route.router.execute(0, prefix + route.path, req, res)
                        }
                    }
                }
            }
            return false
        } catch (e: Throwable) {
            handleError(req, res, e)
            return true
        }
    }
}

fun router(init: Router.() -> Unit): Router {
    val router = Router()
    router.init()
    return router
}

fun router(): Router {
    return Router()
}