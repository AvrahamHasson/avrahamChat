package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class MessageEnvelope(
    val targetUsername: String,
    val message: Message,
    val targetAddress: String? = null,
    val uuid: String? = null
)

