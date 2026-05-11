package com.avrahamChat.models

import kotlinx.serialization.Serializable

@Serializable
data class StatusUpdateReport(val uuid: String, val status: String)

