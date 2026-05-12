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

fun Route.loginNotify() {
    put("/login-notify") {
        runCatching {
            val session = call.receive<UserSession>()
            val ipPattern = Regex(pattern = """^(\d{1,3}\.){3}\d{1,3}:\d{1,5}$""")
            if (session.userId.username.isBlank() || (!ipPattern.matches(session.userId.address))) {
                call.respond(status = HttpStatusCode.BadRequest, message = "Validation Error")
                return@put
            }
            SessionRepository.sessions.updateOne(
                Filters.eq("username", session.userId.username),
                Updates.combine(
                    Updates.set("address", session.userId.address),
                    Updates.set("backendPort", session.backendPort),
                    Updates.set("tcpPort", session.tcpPort),
                    Updates.set("lastHeartbeat", System.currentTimeMillis()),
                    Updates.set("status", UserSessionStatus.ONLINE)
                ),
                UpdateOptions().upsert(true)
            )
            RabbitManager.emitEvent(type = "USER_LOGIN", userName = session.userId.username)
            call.respond(HttpStatusCode.OK)
        }.onFailure { e ->
            System.err.println("Login notify failed: ${e.message}")
            call.respond(status = HttpStatusCode.InternalServerError, e.message ?: "Unknown error occurred")
        }
    }
}