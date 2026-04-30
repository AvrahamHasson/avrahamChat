package com.avrahamChat.monitoring

import com.avrahamChat.database.SessionRepository
import com.avrahamChat.messaging.RabbitManager
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList

suspend fun checkExpiredSessions(){
    while(true){
        val now = System.currentTimeMillis()
        val timeout = 60000
        val expiredUsers = SessionRepository.sessions.find(
            Filters.and(
                Filters.eq("status", "ONLINE"),
                Filters.lt("lastHeartbeat", now - timeout)
            )
        ).toList()
        expiredUsers.forEach { user ->
            println("${user.username} didnt respond for a minute, cleaning up...")
            SessionRepository.sessions.
            updateOne(
                Filters.eq("username", user.username),
                Updates.set("status", "OFFLINE"),
            )
            RabbitManager.emitEvent("USER_LOGOUT", user.username)
        }
        delay(20_000)
    }
}