package com.avrahamChat.database

import com.avrahamChat.models.UserSession
import com.mongodb.kotlin.client.coroutine.MongoCollection

object SessionRepository {
    private val db = DatabaseFactory.getDatabase("signon_db")
    val sessions: MongoCollection<UserSession> = db.getCollection<UserSession>("active_sessions")
}