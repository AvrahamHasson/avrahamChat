package com.avrahamChat.messaging

import com.avrahamChat.routes.activeSessions
import com.avrahamChat.database.ChatRepository
import com.avrahamChat.models.Message
import com.rabbitmq.client.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

object RabbitConsumer {
    private val consumerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startEventListening() {
        val channel = RabbitManager.getChannel()
        consumerScope.launch {
            try {
                val exchange = RabbitManager.getExchangeName()
                val queueName = channel.queueDeclare().queue
                channel.queueBind(queueName, exchange, "")
                val deliverCallback = DeliverCallback { _, delivery ->
                    val message = String(delivery.body, Charsets.UTF_8)
                    handleEvent(message)
                }
                channel.basicConsume(queueName, false, deliverCallback, CancelCallback { })
            } catch (e: Exception) {}
        }
    }

    fun startUserListening(username: String) {
        val channel = RabbitManager.getChannel()

        consumerScope.launch {
            try {
                val incomingQueue = "go_incoming_$username"
                channel.queueDeclare(incomingQueue, false, false, false, null)
                val deliverCallback = DeliverCallback { _, delivery ->
                    val jsonContent = String(delivery.body, Charsets.UTF_8)
                    handleIncomingMessageFromGo(jsonContent)
                }
                channel.basicConsume(incomingQueue, true, deliverCallback, CancelCallback { })
            } catch (e: Exception) {}
        }

        consumerScope.launch {
            try {
                val statusQueue = "go_status_$username"
                channel.queueDeclare(statusQueue, false, false, false, null)
                val deliverCallback = DeliverCallback { _, delivery ->
                    val jsonContent = String(delivery.body, Charsets.UTF_8)
                    handleStatusUpdate(jsonContent)
                }
                channel.basicConsume(statusQueue, true, deliverCallback, CancelCallback { })
            } catch (e: Exception) {}
        }
    }

    private fun handleIncomingMessageFromGo(jsonContent: String) {
        try {
            val jsonElement = Json.parseToJsonElement(jsonContent).jsonObject
            val target = jsonElement["targetUsername"]?.jsonPrimitive?.content ?: return
            val rawMessage = jsonElement["message"]?.let {
                Json.decodeFromJsonElement<Message>(it)
            } ?: return

            val updatedMessage = rawMessage.copy(status = "received")

            consumerScope.launch {
                ChatRepository.saveMessage(target, updatedMessage)
                activeSessions[target]?.let { session ->
                    try {
                        session.send(Json.encodeToString(updatedMessage))
                    } catch (e: Exception) {
                        activeSessions.remove(target)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    private fun handleStatusUpdate(jsonContent: String) {
        try {
            val json = Json.parseToJsonElement(jsonContent).jsonObject
            val uuid = json["uuid"]?.jsonPrimitive?.content ?: return
            val status = json["status"]?.jsonPrimitive?.content ?: "delivered"

            consumerScope.launch {
                ChatRepository.updateMessageStatus(uuid, status)
            }
        } catch (e: Exception) {}
    }

    private fun handleEvent(message: String) {
        try {
            val parts = message.split(":")
            if (parts.size < 2 || parts[0] != "USER_LOGOUT") return
            val username = parts[1]
            activeSessions[username]?.let { session ->
                consumerScope.launch {
                    try {
                        session.close(CloseReason(CloseReason.Codes.NORMAL, "Logged out"))
                    } finally {
                        activeSessions.remove(username)
                    }
                }
            }
        } catch (e: Exception) {}
    }
}