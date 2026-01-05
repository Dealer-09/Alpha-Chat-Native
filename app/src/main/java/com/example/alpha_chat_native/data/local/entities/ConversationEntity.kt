package com.example.alpha_chat_native.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alpha_chat_native.data.models.Conversation
import com.example.alpha_chat_native.data.models.Message
import com.example.alpha_chat_native.data.models.User

/**
 * Room entity for cached conversations.
 * Stores conversation metadata with last message info.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val lastActivity: String? = null,
    val unreadCount: Int = 0,
    // Other user info (for DM)
    val otherUserId: String = "",
    val otherUserUsername: String = "",
    val otherUserDisplayName: String = "",
    val otherUserAvatar: String = "",
    val otherUserIsOnline: Boolean = false,
    // Last message info
    val lastMessageId: String? = null,
    val lastMessageContent: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageCreatedAt: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

// Extension functions
fun ConversationEntity.toModel(): Conversation {
    val otherUser = if (otherUserId.isNotEmpty()) User(
        _id = otherUserId,
        username = otherUserUsername,
        displayName = otherUserDisplayName,
        avatar = otherUserAvatar,
        isOnline = otherUserIsOnline
    ) else null
    
    val lastMsg = if (lastMessageId != null) Message(
        id = lastMessageId,
        content = lastMessageContent ?: "",
        sender = if (lastMessageSenderId != null) User(_id = lastMessageSenderId) else null,
        createdAt = lastMessageCreatedAt
    ) else null
    
    return Conversation(
        id = id,
        lastActivity = lastActivity,
        otherUser = otherUser,
        lastMessage = lastMsg
    )
}

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    lastActivity = lastActivity,
    otherUserId = otherUser?.id ?: "",
    otherUserUsername = otherUser?.username ?: "",
    otherUserDisplayName = otherUser?.displayName ?: "",
    otherUserAvatar = otherUser?.avatar ?: "",
    otherUserIsOnline = otherUser?.isOnline ?: false,
    lastMessageId = lastMessage?.id,
    lastMessageContent = lastMessage?.content,
    lastMessageSenderId = lastMessage?.sender?.id,
    lastMessageCreatedAt = lastMessage?.createdAt
)
