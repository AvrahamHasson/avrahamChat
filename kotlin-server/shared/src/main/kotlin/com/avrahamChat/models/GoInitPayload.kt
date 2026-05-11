package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class GoInitPayload(val username: String, val tcpPort: String, val action: String)