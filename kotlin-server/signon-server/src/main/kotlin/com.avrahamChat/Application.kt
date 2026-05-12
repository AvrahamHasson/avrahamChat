package com.avrahamChat

import com.avrahamChat.config.AppConfig.SIGNON_PORT
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.*
import com.avrahamChat.routes.*
import com.avrahamChat.monitoring.*
import com.avrahamChat.messaging.RabbitManager

fun main() {
    embeddedServer(Netty, port = SIGNON_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    RabbitManager.init()

    launch {
        startExpiredSessionMonitoring()
    }

    routing {
        loginNotify()
        heartbeat()
        disconnect()
    }
}