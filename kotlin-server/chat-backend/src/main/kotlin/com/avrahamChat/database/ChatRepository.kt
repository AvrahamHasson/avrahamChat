package com.avrahamChat.database

import com.avrahamChat.models.*

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

object ChatRepository {
    private val db = DatabaseFactory.getDatabase("chat_app")
    private val chatsCollection: MongoCollection<Chat> = db.getCollection<Chat>("chats")
    private val usersCollection: MongoCollection<User> = db.getCollection<User>("users")

    suspend fun getUserAddress(username: String): String? =
        usersCollection.find(Filters.eq("username", username)).firstOrNull()?.address

    suspend fun getChatsForUser(username: String): List<Chat> =
        chatsCollection.find(
            Filters.elemMatch("participants", Filters.eq("username", username)))
            .sort(Sorts.descending("lastMessageTime")).toList()

    suspend fun saveMessage(target: String, message: Message) {
        val sender = message.sender
        val roomId = listOf(sender, target).sorted().joinToString("_")

        val participants = usersCollection
            .find(Filters.`in`("username", sender, target))
            .toList()

        if (participants.size != 2) return

        chatsCollection.updateOne(
            Filters.eq("id", roomId),
            Updates.combine(
                Updates.push("messages", message),
                Updates.set("lastMessageTime", message.timestamp),
                Updates.setOnInsert("id", roomId),
                Updates.setOnInsert("participants", participants)
            ),
            UpdateOptions().upsert(true)
        )
    }

    suspend fun updateMessageStatus(uuid: String, newStatus: String) {
        chatsCollection.updateOne(
            Filters.eq("messages.uuid", uuid),
            Updates.set("messages.$.status", newStatus)
        )
    }
}