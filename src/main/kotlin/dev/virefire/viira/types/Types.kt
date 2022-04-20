package dev.virefire.viira.types

open class RequestContext(val req: Request, val res: Response)
class MiddlewareContext(req: Request, res: Response, val next: Next) : RequestContext(req, res)
class ErrorContext(req: Request, res: Response, val err: Throwable) : RequestContext(req, res)

typealias RequestHandler = suspend RequestContext.() -> Unit
typealias MiddlewareHandler = suspend MiddlewareContext.() -> Unit
typealias Next = suspend () -> Unit
typealias ErrorHandler = suspend ErrorContext.() -> Unit