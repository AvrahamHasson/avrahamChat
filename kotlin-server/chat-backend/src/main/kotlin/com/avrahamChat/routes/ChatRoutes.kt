package com.avrahamChat.routes

import com.avrahamChat.config.AppConfig
import com.avrahamChat.config.AppConfig.SIGNON_URL
import com.avrahamChat.config.SerializationConfig.defaultJson
import com.avrahamChat.database.ChatRepository
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.models.Message
import com.avrahamChat.models.MessageEnvelope
import com.avrahamChat.models.MessageStatus
import com.avrahamChat.signonClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

val activeSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

fun Route.chat() {
    get("/chats") {
        val username = call.request.queryParameters["username"]
            ?: return@get call.respond(
                status = HttpStatusCode.BadRequest,
                message = "Missing username"
            )

        val chats = ChatRepository.getChatsForUser(username = username)
        call.respond(message = chats)
    }

    webSocket("/chat-ws/{username}") {
        val username = call.parameters["username"] ?: return@webSocket
        activeSessions[username] = this

        val heartbeatJob = launch {
            while (isActive) {
                runCatching {
                    signonClient.post(urlString = "$SIGNON_URL/heartbeat") {
                        contentType(type = ContentType.Application.Json)
                        setBody(body = username)
                    }
                }.onFailure { e ->
                    if (e is CancellationException) throw e
                    System.err.println("Heartbeat failed for user $username: ${e.message}")
                }
                delay(duration = 30.seconds)
            }
        }

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                runCatching {
                    val text = frame.readText()
                    val input = defaultJson.decodeFromString<Map<String, String>>(string = text)

                    val target = input["targetUsername"] ?: return@runCatching
                    val messageUuid = UUID.randomUUID().toString()

                    val newMessage = Message(
                        uuid = messageUuid,
                        text = input["text"] ?: "",
                        timestamp = System.currentTimeMillis(),
                        sender = username,
                        status = MessageStatus.SENT
                    )

                    ChatRepository.saveMessage(
                        target = target,
                        message = newMessage
                    )

                    val targetSession = activeSessions[target]
                    if (targetSession != null) {
                        targetSession.send(content = defaultJson.encodeToString(value = newMessage))
                    } else {
                        val targetAddress = ChatRepository.getUserAddress(username = target) ?: ""
                        val envelope = MessageEnvelope(
                            targetUsername = target,
                            message = newMessage,
                            targetAddress = targetAddress,
                            uuid = messageUuid
                        )

                        RabbitManager.sendMessageToGo(
                            senderUsername = username,
                            envelope = envelope
                        )
                    }
                }.onFailure { e ->
                    System.err.println("Error processing message frame for $username: ${e.message}")
                }
            }
        } finally {
            activeSessions.remove(username)
            heartbeatJob.cancel()

            RabbitManager.initGoMedium(
                username = username,
                tcpPort = AppConfig.TCP_PORT.toString(),
                action = "LOGOUT"
            )

            runCatching {
                signonClient.post(urlString = "$SIGNON_URL/disconnect") {
                    contentType(type = ContentType.Application.Json)
                    setBody(body = username)
                }
            }.onFailure { e ->
                System.err.println("Disconnect notification failed for user $username: ${e.message}")
            }
        }
    }
}