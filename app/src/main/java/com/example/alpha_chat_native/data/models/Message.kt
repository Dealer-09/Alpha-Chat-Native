package com.example.alpha_chat_native.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Direct Message model matching AlphaChat-V2 backend schema.
 */
@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "_id") val id: String = "",
    val sender: User? = null,
    val receiver: String = "",  // User ID as string
    val conversation: String = "",
    val content: String = "",
    val messageType: String = "text",  // text, code, image, file
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
    val syncStatus: String = "SENT"
) {
    // Backwards compatibility
    val text: String get() = content
    val fromId: String get() = sender?.id ?: ""
    val toId: String get() = receiver
    val chatId: String get() = conversation
    val timestamp: java.util.Date? 
        get() = createdAt?.let { 
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    .parse(it)
            } catch (e: Exception) { null }
        }
}

/**
 * Channel Message model for group chats
 */
@JsonClass(generateAdapter = true)
data class ChannelMessage(
    @Json(name = "_id") val id: String = "",
    val channel: String = "",
    val sender: User? = null,
    val content: String = "",
    val messageType: String = "text",  // text, code, image, file, system
    val codeLanguage: String? = null,
    val imageUrl: String? = null,
    val replyTo: ChannelMessage? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val createdAt: String? = null,
    val syncStatus: String = "SENT"
)

/**
 * Request body for sending messages
 */
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val content: String,
    val messageType: String = "text",
    val codeLanguage: String? = null
)
