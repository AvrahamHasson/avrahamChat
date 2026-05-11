package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String,
    val participants: List<User>,
    val messages: List<Message>,
    var lastMessageTime : Long
)
