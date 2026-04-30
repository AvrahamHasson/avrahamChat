package com.avrahamChat

import com.avrahamChat.config.AppConfig.HTTP_PORT
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json
import com.avrahamChat.routes.*
import com.avrahamChat.messaging.RabbitConsumer
import com.avrahamChat.messaging.RabbitManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*

val signonClient = HttpClient(CIO) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        json()
    }
}

fun main() {
    RabbitManager.init()
    RabbitConsumer.startEventListening()

    embeddedServer(Netty, port = HTTP_PORT) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
        userRouting()
        chatRouting()
    }
}