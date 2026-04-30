package com.avrahamChat.routes

import com.avrahamChat.config.AppConfig
import com.avrahamChat.config.AppConfig.SIGNON_URL
import com.avrahamChat.database.ChatRepository
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.models.*
import com.avrahamChat.signonClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime
import java.util.UUID

val activeSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

fun Route.chatRouting() {
    get("/chats") {
        val username = call.request.queryParameters["username"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing username")
        val userChats = ChatRepository.getChatsForUser(username)
        call.respond(userChats)
    }

    webSocket("/chat-ws/{username}") {
        val username = call.parameters["username"] ?: return@webSocket
        activeSessions[username] = this

        val heartbeatJob = launch {
            try {
                while (isActive) {
                    signonClient.post("$SIGNON_URL/heartbeat") {
                        contentType(ContentType.Application.Json)
                        setBody(username)
                    }
                    delay(30000)
                }
            } catch (e: Exception) {}
        }

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val data = Json.decodeFromString<Map<String, String>>(text)
                    val target = data["targetUsername"] ?: ""
                    val content = data["text"] ?: ""
                    val now = LocalDateTime.now().toString()
                    val messageUuid = UUID.randomUUID().toString()

                    val newMessage = Message(messageUuid, content, now, username, "sent")
                    ChatRepository.saveMessage(target, newMessage)

                    if (activeSessions.containsKey(target)) {
                        activeSessions[target]?.send(Json.encodeToString(newMessage))
                    } else {
                        val targetAddress = ChatRepository.getUserAddress(target) ?: ""
                        val payload = mapOf(
                            "targetAddress" to JsonPrimitive(targetAddress),
                            "targetUsername" to JsonPrimitive(target),
                            "uuid" to JsonPrimitive(messageUuid),
                            "message" to Json.encodeToJsonElement(newMessage)
                        )
                        RabbitManager.sendMessageToGo(username, Json.encodeToString(payload))
                    }
                }
            }
        } finally {
            activeSessions.remove(username)
            heartbeatJob.cancel()

            RabbitManager.initGoMedium(username, AppConfig.TCP_PORT.toString(), "LOGOUT")

            try {
                signonClient.post("$SIGNON_URL/disconnect") {
                    contentType(ContentType.Application.Json)
                    setBody(username)
                }
            } catch (e: Exception) {}
        }
    }
}