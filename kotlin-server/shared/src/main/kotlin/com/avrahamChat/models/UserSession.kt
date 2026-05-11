package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: User,
    val backendPort: Int,
    val tcpPort: Int,
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val status: UserSessionStatus
    )
