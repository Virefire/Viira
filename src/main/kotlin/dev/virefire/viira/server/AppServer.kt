package dev.virefire.viira.server

import dev.virefire.viira.AppConfig
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.withContext

@Suppress("ExtractKtorModule")
class AppServer(ip: String, port: Int, config: AppConfig, handler: suspend (ApplicationRequest, ApplicationResponse) -> Unit) {
    private val engine: CIOApplicationEngine = embeddedServer(CIO, port, ip, configure = {
        this.callGroupSize = config.threadPoolSize
        this.connectionGroupSize = config.threadPoolSize / 2 + 1
        this.workerGroupSize = config.threadPoolSize / 2 + 1
    }) {
        install(createApplicationPlugin(name = "viira") {
            onCall {
                withContext(config.coroutineContext) {
                    handler(it.request, it.response)
                }
            }
        })
    }

    fun start(wait: Boolean) {
        engine.start(wait)
    }

    fun stop () {
        engine.stop()
    }
}
