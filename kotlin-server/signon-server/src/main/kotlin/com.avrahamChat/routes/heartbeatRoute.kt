package com.avrahamChat.routes

import com.avrahamChat.database.SessionRepository
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.models.UserSession
import com.avrahamChat.models.UserSessionStatus
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.heartbeat() {
    patch("/heartbeat") {
        runCatching {
            val username = call.receive<String>()
            SessionRepository.sessions.updateOne(
                Filters.eq("username", username),
                Updates.set("lastHeartbeat", System.currentTimeMillis())
            )
            call.respond(HttpStatusCode.OK)
        }.onFailure { e ->
            System.err.println("Heartbeat failed: ${e.message}")
            call.respond(status = HttpStatusCode.InternalServerError, e.message ?: "Unknown error occurred")
        }
    }
}