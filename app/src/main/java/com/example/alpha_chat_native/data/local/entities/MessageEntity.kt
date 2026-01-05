package com.example.alpha_chat_native.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alpha_chat_native.data.models.Message
import com.example.alpha_chat_native.data.models.User

/**
 * Room entity for cached messages.
 * Stores messages for offline viewing.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val senderAvatar: String = "",
    val receiver: String = "",
    val conversation: String = "",
    val content: String = "",
    val messageType: String = "text",
    val codeLanguage: String? = null,
    val imageUrl: String? = null,
    val delivered: Boolean = false,
    val read: Boolean = false,
    val readAt: String? = null,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

// Extension functions
fun MessageEntity.toModel(): Message = Message(
    id = id,
    sender = if (senderId.isNotEmpty()) User(
        _id = senderId,
        username = senderUsername,
        displayName = senderDisplayName,
        avatar = senderAvatar
    ) else null,
    receiver = receiver,
    conversation = conversation,
    content = content,
    messageType = messageType,
    codeLanguage = codeLanguage,
    imageUrl = imageUrl,
    delivered = delivered,
    read = read,
    readAt = readAt,
    isDeleted = isDeleted,
    isEdited = isEdited,
    editedAt = editedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    senderId = sender?.id ?: "",
    senderUsername = sender?.username ?: "",
    senderDisplayName = sender?.displayName ?: "",
    senderAvatar = sender?.avatar ?: "",
    receiver = receiver,
    conversation = conversation,
    content = content,
    messageType = messageType,
    codeLanguage = codeLanguage,
    imageUrl = imageUrl,
    delivered = delivered,
    read = read,
    readAt = readAt,
    isDeleted = isDeleted,
    isEdited = isEdited,
    editedAt = editedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
