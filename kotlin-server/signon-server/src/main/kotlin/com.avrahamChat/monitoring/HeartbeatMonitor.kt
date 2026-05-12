package com.avrahamChat.monitoring

import com.avrahamChat.database.SessionRepository
import com.avrahamChat.messaging.RabbitManager
import com.avrahamChat.models.UserSessionStatus
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration.Companion.seconds
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlinx.coroutines.runBlocking


fun startExpiredSessionMonitoring(): Timer {
    return fixedRateTimer(
        name = "expired-session-monitor",
        daemon = true,
        initialDelay = 5.seconds.inWholeMilliseconds,
        period = 30.seconds.inWholeMilliseconds
    ) {
        runCatching {
            runBlocking {
                val now = System.currentTimeMillis()
                val timeoutMillis = 90.seconds.inWholeMilliseconds

                val expiredUsers = SessionRepository.sessions.find(
                    filter = Filters.and(
                        Filters.eq("status", UserSessionStatus.ONLINE),
                        Filters.lt("lastHeartbeat",now - timeoutMillis)
                    )
                ).toList()

                expiredUsers.forEach { session ->
                    val username = session.userId.username
                    println("User $username expired. Marking OFFLINE.")

                    SessionRepository.sessions.updateOne(
                        filter = Filters.eq("userId.username", username),
                        update = Updates.set("status", UserSessionStatus.OFFLINE)
                    )

                    RabbitManager.emitEvent(
                        type = "USER_LOGOUT",
                        userName = username
                    )
                }
            }
        }.onFailure { e ->
            System.err.println("Monitoring Error: ${e.message}")
            if (e.message?.contains("primary constructor") == true) {
                System.err.println("Hint: Old session format detected in DB. Please clear 'sessions' collection.")
            }
        }
    }
}