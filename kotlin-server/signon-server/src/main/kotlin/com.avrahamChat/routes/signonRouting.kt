package com.avrahamChat.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.avrahamChat.database.SessionRepository
import com.avrahamChat.models.UserSession
import com.avrahamChat.messaging.RabbitManager
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates

fun Route.signonRouting() {
    post("/login-notify"){
        val session = call.receive<UserSession>()
        /*val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}(:\d+)?$""")
        if(
            session.username.isBlank() ||
            (!ipPattern.matches(session.address) && session.address != "localhost" )
            ){
            call.respond(HttpStatusCode.BadRequest, "Validation Error")
            return@post
        }*/
        SessionRepository.sessions.
        updateOne(
            Filters.eq("username", session.username),
        Updates.combine(
            Updates.set("address", session.address),
            Updates.set("backendPort", session.backendPort),
            Updates.set("tcpPort", session.tcpPort),
            Updates.set("lastHeartbeat", session.lastHeartbeat),
            Updates.set("status", "ONLINE")
        ),
        UpdateOptions().upsert(true)
        )
        RabbitManager.emitEvent("USER_LOGIN", session.username)
        call.respond(HttpStatusCode.OK)
    }
    post("/heartbeat"){
        val username = call.receive<String>()
        SessionRepository.sessions.
                updateOne(Filters.eq("username", username),
                Updates.set("lastHeartbeat", System.currentTimeMillis())
                )
        call.respond(HttpStatusCode.OK)
    }
    post("/disconnect"){
        val username = call.receive<String>()
        SessionRepository.sessions.
        updateOne(
            Filters.eq("username", username),
            Updates.set("status", "OFFLINE"),
        )
        RabbitManager.emitEvent("USER_LOGOUT", username)
        call.respond(HttpStatusCode.OK)
    }
}