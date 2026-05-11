package com.avrahamChat.messaging

import com.avrahamChat.config.SerializationConfig.defaultJson
import com.avrahamChat.config.AppConfig.RABBIT_HOST
import com.avrahamChat.models.GoInitPayload
import com.avrahamChat.models.MessageEnvelope
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import kotlinx.serialization.encodeToString

object RabbitManager {
    const val EXCHANGE_NAME = "signon_events"
    lateinit var channel: Channel
        private set

    fun init(): Channel {
        val factory = ConnectionFactory().apply {
            host = RABBIT_HOST
            port = 5672
            isAutomaticRecoveryEnabled = true
        }

        return try {
            val connection = factory.newConnection()
            channel = connection.createChannel()

            channel.exchangeDeclare(EXCHANGE_NAME, "fanout")
            channel.queueDeclare("go_init", false, false, false, null)

            println("Successfully connected to RabbitMQ at $RABBIT_HOST")
            channel
        } catch (e: Exception) {
            println("Failed to connect to RabbitMQ: ${e.message}")
            throw e
        }
    }

    fun initGoMedium(username: String, tcpPort: String, action: String = "LOGIN") {
        val payload = GoInitPayload(username, tcpPort, action)
        channel.basicPublish("", "go_init", null, defaultJson.encodeToString(payload).toByteArray())
    }

    fun emitEvent(type: String, userName: String) {
        channel.basicPublish(EXCHANGE_NAME, "", null, "$type:$userName".toByteArray())
    }

    fun sendMessageToGo(senderUsername: String, envelope: MessageEnvelope) {
        val queueName = "go_outgoing_$senderUsername"
        channel.queueDeclare(queueName, false, false, false, null)
        val body = defaultJson.encodeToString(envelope).toByteArray()
        channel.basicPublish("", queueName, null, body)
    }
}