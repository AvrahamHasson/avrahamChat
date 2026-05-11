package com.avrahamChat

import com.avrahamChat.config.AppConfig.HTTP_PORT
import com.avrahamChat.messaging.RabbitConsumer
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.routes.*

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

import kotlinx.serialization.json.Json

val signonClient = HttpClient(engineFactory = CIO) {
    install(plugin = ClientContentNegotiation) {
        json()
    }
}

fun main() {
    RabbitManager.init()
    RabbitConsumer.startEventListening()

    embeddedServer(factory = Netty, port = HTTP_PORT) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(plugin = ServerContentNegotiation) {
        json()
    }

    install(plugin = CORS) {
        anyHost()
        allowMethod(method = HttpMethod.Post)
        allowMethod(method = HttpMethod.Options)
        allowHeader(header = HttpHeaders.ContentType)
        allowHeader(header = HttpHeaders.Authorization)
    }

    install(plugin = WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(format = Json)
    }

    routing {
        user()
        chat()
    }
}