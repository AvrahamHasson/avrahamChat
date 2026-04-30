package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val uuid: String,
    val text: String,
    val timestamp: String,
    val sender: String,
    val status: String
)

