package com.avrahamChat.database

import com.avrahamChat.config.AppConfig.MONGO_URI
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase

object DatabaseFactory {
    private val mongoUri = MONGO_URI
    private val client = MongoClient.create(mongoUri)

    fun getDatabase(dbName: String): MongoDatabase = client.getDatabase(dbName)
}