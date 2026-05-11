package com.avrahamChat.config


import kotlinx.serialization.json.Json

object SerializationConfig {
    val defaultJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
}