package com.avrahamChat.routes

import com.avrahamChat.database.SessionRepository
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.models.UserSessionStatus
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.disconnect() {
    patch("/disconnect") {
        runCatching {
            val username = call.receive<String>()
            SessionRepository.sessions.updateOne(
                Filters.eq("username", username),
                Updates.set("status", UserSessionStatus.OFFLINE),
            )
            RabbitManager.emitEvent(type = "USER_LOGOUT", userName = username)
            call.respond(HttpStatusCode.OK)
        }.onFailure { e ->
            System.err.println("Disconnect failed: ${e.message}")
            call.respond(status = HttpStatusCode.InternalServerError, e.message ?: "Unknown error occurred")
        }
    }
}