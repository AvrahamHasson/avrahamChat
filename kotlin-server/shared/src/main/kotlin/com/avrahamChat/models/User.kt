package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class User(val username: String, val address: String)

