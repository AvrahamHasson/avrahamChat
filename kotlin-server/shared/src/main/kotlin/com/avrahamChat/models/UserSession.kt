package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val username: String,
    val address: String,
    val backendPort: Int,
    val tcpPort: Int,
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val status: String = "ONLINE"
    )
