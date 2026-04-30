package com.avrahamChat.routes

import com.avrahamChat.config.AppConfig.HTTP_PORT
import com.avrahamChat.config.AppConfig.SIGNON_URL
import com.avrahamChat.config.AppConfig.TCP_PORT
import com.avrahamChat.database.DatabaseFactory
import com.avrahamChat.models.UserData
import com.avrahamChat.models.UserSession
import com.avrahamChat.signonClient
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.messaging.RabbitConsumer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.client.request.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Filters
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.toList

fun Route.userRouting() {
    val db = DatabaseFactory.getDatabase("chat_app")
    val usersCollection = db.getCollection<UserData>("users")

    get("/users") {
        call.respond(usersCollection.find().toList())
    }

    post("/sign-on") {
        try {
            val user = call.receive<UserData>()
            val signonResponse = signonClient.post("$SIGNON_URL/login-notify") {
                contentType(ContentType.Application.Json)
                setBody(UserSession(
                    username = user.username,
                    address = user.address,
                    backendPort = HTTP_PORT,
                    tcpPort = TCP_PORT,
                    lastHeartbeat = System.currentTimeMillis(),
                    status = "ONLINE"
                ))
            }

            if (signonResponse.status != HttpStatusCode.OK) {
                call.respond(signonResponse.status, mapOf("error" to signonResponse.bodyAsText()))
                return@post
            }

            usersCollection.replaceOne(
                Filters.eq("username", user.username),
                user,
                ReplaceOptions().upsert(true)
            )

            RabbitManager.initGoMedium(user.username, TCP_PORT.toString(), "LOGIN")
            RabbitConsumer.startUserListening(user.username)

            call.respond(HttpStatusCode.Created, user)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Sign-on error")
        }
    }
}