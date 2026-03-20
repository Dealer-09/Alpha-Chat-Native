package com.example.alpha_chat_native.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Conversation model matching AlphaChat-V2 backend schema.
 * Represents a DM thread between two users.
 * Note: unreadCount is ignored because backend returns different types:
 *   - Conversation list: returns Int
 *   - Single conversation: returns Map<String, Int>
 */
@JsonClass(generateAdapter = true)
data class Conversation(
    @Json(name = "_id") val id: String = "",
    val participants: List<User> = emptyList(),
    val lastMessage: Message? = null,
    val lastActivity: String? = null,
    // Backend returns this pre-populated in getConversations
    val otherUser: User? = null,
    // Skip JSON parsing - backend returns  Map<String,Int> which Moshi can't handle generically
    @Json(ignore = true) val unreadCount: Int = 0,
) {
    val participantIds: List<String> get() = participants.map { it.id }
    val lastMessageText: String get() = lastMessage?.content ?: ""
}

/**
 * Conversation detail response with messages
 */
@JsonClass(generateAdapter = true)
data class ConversationDetail(
    val conversation: Conversation,
    val messages: List<Message> = emptyList(),
    val pagination: Pagination? = null
)

/**
 * Pagination info for message lists
 */
@JsonClass(generateAdapter = true)
data class Pagination(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalMessages: Int = 0,
    val hasMore: Boolean = false
)
