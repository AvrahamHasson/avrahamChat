package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class UserData(val username: String, val address: String)

