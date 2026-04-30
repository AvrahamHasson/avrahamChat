package com.avrahamChat.messaging

import com.avrahamChat.config.AppConfig.RABBIT_HOST
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object RabbitManager {
    private const val EXCHANGE_NAME = "signon_events"
    private lateinit var channel: Channel

    fun init(): Channel {
        val factory = ConnectionFactory().apply { host = RABBIT_HOST }
        channel = factory.newConnection().createChannel()
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout")
        channel.queueDeclare("go_init", false, false, false, null)
        return channel
    }

    fun initGoMedium(username: String, tcpPort: String, action: String = "LOGIN") {
        val initPayload = buildJsonObject {
            put("username", username)
            put("tcpPort", tcpPort)
            put("action", action)
        }
        channel.basicPublish("", "go_init", null, Json.encodeToString(initPayload).toByteArray())
    }

    fun getChannel() = channel
    fun getExchangeName() = EXCHANGE_NAME

    fun emitEvent(type: String, userName: String) {
        val message = "$type:$userName"
        channel.basicPublish(EXCHANGE_NAME, "", null, message.toByteArray())
    }

    fun sendMessageToGo(senderUsername: String, jsonMessage: String) {
        val queueName = "go_outgoing_$senderUsername"
        channel.queueDeclare(queueName, false, false, false, null)
        channel.basicPublish("", queueName, null, jsonMessage.toByteArray())
    }
}