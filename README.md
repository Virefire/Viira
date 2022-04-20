# Viira - Kotlin HTTP framework for building RESTful APIs

![Open issues](https://img.shields.io/github/issues-raw/Virefire/Viira)

🎉 Viira is a simple and beautiful Kotlin HTTP framework designed to create a RESTful APIs (but of course it can be used for something else as well). It is built on top of the Ktor CIO engine and uses coroutines to process requests asynchronously. Viira API is inspired by ExpressJS, so if you already have experience building APIs in NodeJS, learning Viira will be very easy.

💼 **This readme contains full library documentation/tutorial!**

## Install

Gradle Kotlin:
```kotlin
repositories {
    maven {
        url = uri("https://maven.rikonardo.com/releases")
    }
}

dependencies {
    implementation("dev.virefire.viira:Viira:1.0.0")
}
```

## Documentation

| Content                                      |
|----------------------------------------------|
| **1. [Application](#application)**           |
| **2. [Request handling](#request-handling)** |
| **3. [Middlewares](#middlewares)**           |
| **4. [Routers](#routers)**                   |
| **5. [Error handling](#error-handling)**     |

### Application

Simple application example:

```kotlin
fun main() {
    val app = application()
    app.get("/") {
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

Notice, that by default calling `app.start(...)` will freeze the current thread. You can change this behavior by calling `app.start(..., wait = false)` instead.

Example above shows how to create an application with default settings. But you can also create an application with custom configuration:

```kotlin
fun main() {
    val app = application {
        trustProxy = true
        proxyHeader = "X-Forwarded-For"
        threadPoolSize = 32
    }
    app.get("/") {
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

List of available configuration options:

| Name              | Description                                                                                                | Default           |
|-------------------|------------------------------------------------------------------------------------------------------------|-------------------|
| `trustProxy`      | Whether to trust proxy headers or not                                                                      | `false`           |
| `proxyHeader`     | Name of the proxy header to use                                                                            | `X-Forwarded-For` |
| `threadPoolSize`  | Number of threads used to process requests                                                                 | `4`               |
| `hideBranding`    | Whether to hide Viira branding from headers or not                                                         | `false`           |
| `handlePreflight` | Whether to automatically handle preflight requests                                                         | `true`            |
| `silentJson`      | Enables [silent mode in JSON parser](https://github.com/Virefire/KSON/blob/master/README.md#deserializing) | `false`           |

You can also pass `routes` lambda to `application` function to app configuration:

```kotlin
fun main() {
    application {
        routes {
            get("/") {
                res.text("Hello, world!")
            }
            get("/hello/:name") {
                res.text("Hello, ${req.params["name"]}!")
            }
        }
    }.start(8080)
}
```

### Request handling

Look at this example:

```kotlin
fun main() {
    val app = application()
    app.get("/") {
        println("Request received: ${req.method} ${req.path}")
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

Here we see the use of `req` (`Request` object) and `res` (`Response` object). They are available in the context of the request handler.

You can get all request data from `req` object:

```kotlin
fun main() {
    val app = application()
    app.get("/") {
        println("""
            Request received: ${req.method} ${req.path}
            Headers: ${req.headers}
            Cookies: ${req.cookies}
            Query params: ${req.query}
            Params: ${req.params}
            IP: ${req.ip}
            Timestamp: ${req.timestamp.time}
            Host: ${req.host}
        """.trimIndent())
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

Using `req.stream` we can access raw request body as an InputStream. But when we building REST API, we usually want to work with json data, so Viira provides `req.json` method to parse json data from request body.

```kotlin
fun main() {
    val app = application()
    app.post("/hello") {
        res.text("Hello, ${req.json["name"].string}!")
    }
    app.start(8080)
}
```

Viira uses [KSON](https://github.com/Virefire/KSON) to parse json data. Visit [KSON documentation](https://github.com/Virefire/KSON/blob/master/README.md) to learn more about its API.

After processing the request, you can send response using `res` object. It has a lot of methods to send different types of data:

```kotlin
fun main() {
    val app = application()
    app.get("/text") {
        // Automatically sets Content-Type header to text/plain
        res.text("Hello, world!")
    }
    app.get("/body") {
        // Sends String or ByteArray without forcing Content-Type header, which is set to application/octet-stream by default
        res.body("Hello, world!")
    }
    app.get("/body2") {
        // You can set Content-Type header by passing it as a second parameter
        res.body("Hello, world!", "text/plain")
    }
    app.get("/body3") {
        // Or you can set Content-Type header with separate method
        res.type("text/plain").body("Hello, world!")
    }
    app.get("/body4") {
        // By the way, you can set response status code with res.status(code) method
        res.status(403).type("text/plain").body("Nope")
    }
    app.get("/json") {
        // You can send json data using `json` method, it automatically sets Content-Type header to application/json
        res.json(mapOf(
            "message" to "Hello, world!"
        ))
    }
    app.get("/json2") {
        // Because Viira uses KSON, you can pass json data not only as a map, but also as an object
        res.json(object {
            val message = "Hello, world!"
        })
    }
    app.get("/redirect") {
        // You can redirect to another url with res.redirect(url) method
        res.redirect("/")
    }
    app.get("/redirect2") {
        // And you can also specify status code by passing it as a second parameter (by default it's 302)
        res.redirect("/", 301)
    }
    app.start(8080)
}
```

Viira supports all HTTP methods, but `OPTIONS` requests will be automatically handled by Viira if `handlePreflight` is set to `true` in application configuration.

You can add method handlers to your application using `get`, `post`, `put`, `patch`, `delete`, `trace`, `connect`, `options` and `head` methods. Note that Viiara doesn't support websockets, but they still can be implemented by interacting directly with Ktor call using `req.originalCall`.

### Middlewares

Middlewares are functions that can intercept requests and do something before or after the request is processed. Here is an example of middleware that logs all requests:

```kotlin
val logger = middleware {
    println("${req.method} ${req.path}")
    next()
}

fun main() {
    val app = application()
    app.use(logger)
    app.get("/") {
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

Notice, that middleware context has a `next` method that you can use to call next middleware or request handler in the chain. If you don't call `next()`, request won't be passed to next handlers.

`next()` call can be used, for example, to measure time of request processing:

```kotlin
val timer = middleware {
    val millis = measureTimeMillis { next() }
    println("$millis ms".padEnd(8, ' ') + " | ${req.method} ${req.path}")
}

fun main() {
    val app = application()
    app.use(timer)
    app.get("/") {
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

Often we need to pass some data between middlewares. To do that, we can use `req.meta`.

```kotlin
data class User(val id: Int, val name: String)

val authorize = middleware {
    req.meta["session"] = User(1, "Viira")
    next()
}

fun main() {
    val app = application()
    app.use(authorize)
    app.get("/") {
        val user = req.meta["session"] as User
        res.text("Hello, ${user.name}!")
    }
    app.start(8080)
}
```

To pass arguments to middlewares, you can wrap them into a function:

```kotlin
fun logger(name: String) = middleware {
    println("[$name] ${req.method} ${req.path}")
    next()
}

fun main() {
    val app = application()
    app.use(logger("MyLogger"))
    app.get("/") {
        res.text("Hello, world!")
    }
    app.start(8080)
}
```

You can also apply middlewares to specific route prefix:

```kotlin
val validate = middleware {
    if (req.json["name"].isNull) {
        res.status(400).text("Name is required")
    } else {
        next()
    }
}

fun main() {
    val app = application()
    app.use("/api", validate)
    app.get("/api/greeter") {
        res.text("Hello, ${req.json["name"].string}!")
    }
    app.start(8080)
}
```

Or event to specific handler:

```kotlin
fun main() {
    val app = application()
    app.get("/api/greeter", validate) {
        res.text("Hello, ${req.json["name"].string}!")
    }
    app.start(8080)
}
```

You can also apply multiple middlewares, they're going to be executed in the left to right order:

```kotlin
fun main() {
    val app = application()
    app.get("/api/greeter", logger("Greeter"), validate) {
        res.text("Hello, ${req.json["name"].string}!")
    }
    app.start(8080)
}
```

### Routers

Writing all routes in one file is not very convenient. To solve this problem, we can use routers. Routers are a way to group routes into logical groups.

```kotlin
val greeter = router {
    get("/") {
        res.text("Hello, world!")
    }
    get("/:name") {
        res.text("Hello, ${req.params["name"]}!")
    }
}

fun main() {
    val app = application()
    app.use("/greeter", greeter)
    app.start(8080)
}
```

There is also a more classical way to write routers, you can use it, if, for example, you want to generate routers dynamically:

```kotlin
fun greeter(): Router {
    val router = router()
    router.get("/") {
        res.text("Hello, world!")
    }
    router.get("/:name") {
        res.text("Hello, ${req.params["name"]}!")
    }
    return router
}
```

Note that all middlewares, applied to router will be only executed for queries that match router's prefix.

### Error handling

Viira also provides a way to handle exceptions. You can add error handlers to an application or router.

```kotlin
fun main() {
    val app = application()
    
    app.get("/:name") {
        if (req.params["name"] != "Patrik") {
            throw IllegalArgumentException("Its not a Patrik!")
        }
        else {
            res.text("Hello, Sponge Bob!")
        }
    }
    
    app.error {
        if (err is IllegalArgumentException) {
            res.status(400).text("IllegalArgumentException: ${err.message}")
        } else {
            throw err
        }
    }
    app.error {
        res.status(500).text("Something went wrong") // This will be executed if no other error handler matches
    }
    
    app.start(8080)
}
```

`err` is a variable, accessible in error handler context, that contains the exception that was thrown. Rethrowing the exception will pass it to the next error handler.