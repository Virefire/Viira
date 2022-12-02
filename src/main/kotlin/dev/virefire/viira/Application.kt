package dev.virefire.viira

import dev.virefire.viira.server.AppServer
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers.Default
import java.util.*

object Viira {
    val version: String
    init {
        val p = Properties()
        val stream = Viira::class.java.getResourceAsStream("/dev/virefire/viira/version.properties")
        if (stream != null) {
            p.load(stream)
            version = p.getProperty("version")
        } else {
            version = "[unknown]"
        }
    }
}

class Application(val config: AppConfig) : Router() {
    init {
        config.routes?.let { it(this) }
    }

    var server: AppServer? = null

    fun start(port: Int, wait: Boolean = true): Application {
        return start("0.0.0.0", port, wait)
    }

    fun start(ip: String, port: Int, wait: Boolean = true): Application {
        if (server != null)
            throw IllegalStateException("Already listening")
        server = AppServer(ip, port, config) { ctx, msg ->
            handle(ctx, msg)
        }
        server!!.start(wait)
        return this
    }

    fun stop() {
        if (server == null)
            throw IllegalStateException("Not listening")
        server?.stop()
        server = null
    }

    private suspend fun handle(appReq: ApplicationRequest, appRes: ApplicationResponse) {
        try {
            handle(config, appReq, appRes, this::execute, this::find)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class AppConfig {
    var trustProxy = false
    var proxyHeader = "X-Forwarded-For"
    var hideBranding = false
    var threadPoolSize = 4
    var handlePreflight = true
    var silentJson = false
    var routes: (Router.() -> Unit)? = null
    var coroutineContext = Default
    fun routes(routes: Router.() -> Unit) {
        this.routes = routes
    }
}

fun application(cfg: AppConfig.() -> Unit): Application {
    val config = AppConfig()
    cfg(config)
    return Application(config)
}

fun application(): Application {
    return Application(AppConfig())
}