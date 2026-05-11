package com.avrahamChat.routes

import com.avrahamChat.config.AppConfig.HTTP_PORT
import com.avrahamChat.config.AppConfig.SIGNON_URL
import com.avrahamChat.config.AppConfig.TCP_PORT
import com.avrahamChat.messaging.RabbitConsumer
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.models.User
import com.avrahamChat.models.UserSession
import com.avrahamChat.models.UserSessionStatus
import com.avrahamChat.signonClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList

fun Route.user() {
    val db = DatabaseFactory.getDatabase(dbName = "chat_app")
    val usersCollection = db.getCollection<User>(collectionName = "users")

    get("/users") {
        val users = usersCollection.find().toList()
        call.respond(message = users)
    }

    post("/sign-on") {
        runCatching {
            val user = call.receive<User>()

            val signonResponse = signonClient.post(urlString = "$SIGNON_URL/login-notify") {
                contentType(type = ContentType.Application.Json)
                setBody(
                    body = UserSession(
                        userId = user,
                        backendPort = HTTP_PORT,
                        tcpPort = TCP_PORT,
                        lastHeartbeat = System.currentTimeMillis(),
                        status = UserSessionStatus.ONLINE
                    )
                )
            }

            if (signonResponse.status != HttpStatusCode.OK) {
                call.respond(
                    status = signonResponse.status,
                    message = mapOf("error" to signonResponse.bodyAsText())
                )
                return@post
            }

            usersCollection.replaceOne(
                filter = Filters.eq("username", user.username),
                replacement = user,
                options = ReplaceOptions().upsert(true)
            )

            RabbitManager.initGoMedium(
                username = user.username,
                tcpPort = TCP_PORT.toString(),
                action = "LOGIN"
            )

            RabbitConsumer.startUserListening(username = user.username)

            call.respond(
                status = HttpStatusCode.Created,
                message = user
            )
        }.onFailure { e ->
            System.err.println("Sign-on process failed: ${e.message}")
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = "Sign-on error"
            )
        }
    }
}