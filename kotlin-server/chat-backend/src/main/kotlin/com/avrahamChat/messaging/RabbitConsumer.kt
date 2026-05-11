package com.avrahamChat.messaging

import com.avrahamChat.config.SerializationConfig.defaultJson
import com.avrahamChat.database.ChatRepository
import com.avrahamChat.models.MessageEnvelope
import com.avrahamChat.models.MessageStatus
import com.avrahamChat.models.StatusUpdateReport
import com.avrahamChat.routes.activeSessions
import com.rabbitmq.client.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

object RabbitConsumer {
    private val consumerScope = CoroutineScope(context = Dispatchers.IO + SupervisorJob())

    fun startEventListening() {
        val channel = RabbitManager.channel
        consumerScope.launch {
            runCatching {
                val exchange = RabbitManager.EXCHANGE_NAME
                val queueName = channel.queueDeclare().queue
                channel.queueBind(queueName, exchange, "")

                val deliverCallback = DeliverCallback { _, delivery ->
                    handleEvent(message = delivery.body.toString(Charsets.UTF_8))
                }

                channel.basicConsume(queueName, false, deliverCallback) { _ -> }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                System.err.println("Error in event listening: ${e.message}")
            }
        }
    }

    fun startUserListening(username: String) {
        val channel = RabbitManager.channel

        setupQueueConsumer(channel = channel, queueName = "go_incoming_$username") { json ->
            handleIncomingMessageFromGo(jsonContent = json)
        }

        setupQueueConsumer(channel = channel, queueName = "go_status_$username") { json ->
            handleStatusUpdate(jsonContent = json)
        }
    }

    private fun setupQueueConsumer(channel: Channel, queueName: String, onMessage: (String) -> Unit) {
        consumerScope.launch {
            runCatching {
                channel.queueDeclare(queueName, false, false, false, null)
                val deliverCallback = DeliverCallback { _, delivery ->
                    onMessage(delivery.body.toString(Charsets.UTF_8))
                }
                channel.basicConsume(queueName, true, deliverCallback) { _ -> }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                System.err.println("Error consuming from $queueName: ${e.message}")
            }
        }
    }

    private fun handleIncomingMessageFromGo(jsonContent: String) {
        runCatching { defaultJson.decodeFromString<MessageEnvelope>(jsonContent) }
            .onSuccess { envelope ->
                val updatedMessage = envelope.message.copy(status = MessageStatus.RECEIVED)
                consumerScope.launch {
                    ChatRepository.saveMessage(target = envelope.targetUsername, message = updatedMessage)

                    activeSessions[envelope.targetUsername]?.let { session ->
                        runCatching {
                            session.send(content = defaultJson.encodeToString(value = updatedMessage))
                        }.onFailure {
                            activeSessions.remove(key = envelope.targetUsername)
                        }
                    }
                }
            }
            .onFailure { e ->
                System.err.println("Failed to parse incoming message: ${e.message}")
            }
    }

    private fun handleStatusUpdate(jsonContent: String) {
        runCatching { defaultJson.decodeFromString<StatusUpdateReport>(jsonContent) }
            .onSuccess { report ->
                consumerScope.launch {
                    ChatRepository.updateMessageStatus(uuid = report.uuid, newStatus = report.status)
                }
            }
            .onFailure { e ->
                System.err.println("Failed to update status: ${e.message}")
            }
    }

    private fun handleEvent(message: String) {
        val parts = message.split(":")
        if (parts.size < 2 || parts[0] != "USER_LOGOUT") return

        val username = parts[1]
        activeSessions[username]?.let { session ->
            consumerScope.launch {
                try {
                    session.close(reason = CloseReason(code = CloseReason.Codes.NORMAL, message = "Logged out"))
                } finally {
                    activeSessions.remove(key = username)
                }
            }
        }
    }
}