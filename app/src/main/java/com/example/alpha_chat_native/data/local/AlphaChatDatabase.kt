package com.example.alpha_chat_native.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.alpha_chat_native.data.local.dao.ConversationDao
import com.example.alpha_chat_native.data.local.dao.MessageDao
import com.example.alpha_chat_native.data.local.dao.UserDao
import com.example.alpha_chat_native.data.local.entities.ConversationEntity
import com.example.alpha_chat_native.data.local.entities.MessageEntity
import com.example.alpha_chat_native.data.local.entities.UserEntity

/**
 * Room database for offline data persistence.
 * Caches users, conversations, and messages for offline viewing.
 */
@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        ConversationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AlphaChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}
