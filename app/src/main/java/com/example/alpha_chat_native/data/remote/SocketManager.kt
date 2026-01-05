package com.example.alpha_chat_native.data.remote

import com.example.alpha_chat_native.data.models.Message
import com.example.alpha_chat_native.data.models.ChannelMessage
import com.example.alpha_chat_native.data.models.User
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket.IO manager for real-time messaging.
 * Handles connection, events, and message flow.
 */
@Singleton
class SocketManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    companion object {
        private const val BASE_URL = "https://alphachat-v2-backend.onrender.com"
    }

    private var socket: Socket? = null
    private var currentUserId: String? = null

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    // Incoming direct messages
    private val _directMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val directMessages = _directMessages.asSharedFlow()

    // Incoming channel messages
    private val _channelMessages = MutableSharedFlow<ChannelMessage>(extraBufferCapacity = 64)
    val channelMessages = _channelMessages.asSharedFlow()

    // Typing indicators: Map<senderId, isTyping>
    private val _typingUsers = MutableStateFlow<Map<String, TypingInfo>>(emptyMap())
    val typingUsers = _typingUsers.asStateFlow()

    // Online users
    private val _onlineUsers = MutableStateFlow<List<OnlineUserInfo>>(emptyList())
    val onlineUsers = _onlineUsers.asStateFlow()

    // Read receipts
    private val _messagesRead = MutableSharedFlow<MessagesReadEvent>(extraBufferCapacity = 64)
    val messagesRead = _messagesRead.asSharedFlow()

    /**
     * Connect to Socket.IO server
     */
    fun connect(userId: String) {
        if (socket?.connected() == true && currentUserId == userId) {
            Timber.d("Socket already connected for user: $userId")
            return
        }

        // Disconnect existing connection
        disconnect()

        currentUserId = userId
        _connectionState.value = ConnectionState.CONNECTING

        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 20000
                
                // Add session cookie for auth
                tokenManager.getSessionCookie()?.let { cookie ->
                    extraHeaders = mapOf("Cookie" to listOf(cookie))
                }
            }

            socket = IO.socket(BASE_URL, options)
            setupEventListeners()
            socket?.connect()

        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to Socket.IO")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    private fun setupEventListeners() {
        socket?.apply {
            // Connection events
            on(Socket.EVENT_CONNECT) {
                Timber.d("Socket connected")
                _connectionState.value = ConnectionState.CONNECTED
                
                // Emit join event with user ID
                currentUserId?.let { userId ->
                    emit("join", userId)
                    Timber.d("Emitted join event for user: $userId")
                }
            }

            on(Socket.EVENT_DISCONNECT) {
                Timber.d("Socket disconnected")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.getOrNull(0)
                Timber.e("Socket connection error: $error")
                _connectionState.value = ConnectionState.ERROR
            }

            // Direct message received
            // Backend sends: { message: {...}, senderId: "..." } or { conversationId: "...", message: {...} }
            on("directMessage") { args ->
                try {
                    val wrapper = args.getOrNull(0) as? JSONObject ?: return@on
                    // Extract the message object from the wrapper
                    val messageJson = wrapper.optJSONObject("message") ?: wrapper
                    val message = parseDirectMessage(messageJson)
                    _directMessages.tryEmit(message)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing direct message")
                }
            }

            // Channel message received
            on("channelMessage") { args ->
                try {
                    val data = args.getOrNull(0) as? JSONObject ?: return@on
                    val message = parseChannelMessage(data)
                    _channelMessages.tryEmit(message)
                    Timber.d("Received channel message: ${message.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing channel message")
                }
            }

            // Online users update
            // Backend sends: { users: ["userId1", "userId2", ...], count: N }
            on("onlineUsers") { args ->
                try {
                    val wrapper = args.getOrNull(0) as? JSONObject ?: return@on
                    val usersArray = wrapper.optJSONArray("users")
                    if (usersArray != null) {
                        val userIds = mutableListOf<String>()
                        for (i in 0 until usersArray.length()) {
                            userIds.add(usersArray.getString(i))
                        }
                        // Convert string IDs to OnlineUserInfo
                        _onlineUsers.value = userIds.map { OnlineUserInfo(userId = it, status = "online", joinedAt = "") }
                        Timber.d("Online users updated: ${userIds.size} users online")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing online users: ${args.getOrNull(0)}")
                }
            }

            // Typing indicator
            on("userTyping") { args ->
                try {
                    val data = args.getOrNull(0) as? JSONObject ?: return@on
                    val senderId = data.getString("senderId")
                    val isTyping = data.getBoolean("isTyping")
                    val channelId = data.optString("channelId", "").takeIf { it.isNotEmpty() }
                    val recipientId = data.optString("recipientId", "").takeIf { it.isNotEmpty() }

                    _typingUsers.update { current ->
                        if (isTyping) {
                            current + (senderId to TypingInfo(channelId, recipientId))
                        } else {
                            current - senderId
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing typing event")
                }
            }

            // Messages read event
            on("messagesRead") { args ->
                try {
                    val data = args.getOrNull(0) as? JSONObject ?: return@on
                    val conversationId = data.getString("conversationId")
                    val readerId = data.getString("readerId")
                    _messagesRead.tryEmit(MessagesReadEvent(conversationId, readerId))
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing messages read event")
                }
            }
        }
    }

    /**
     * Join a channel room to receive messages
     */
    fun joinChannel(channelId: String) {
        socket?.emit("joinChannel", channelId)
        Timber.d("Joined channel: $channelId")
    }

    /**
     * Leave a channel room
     */
    fun leaveChannel(channelId: String) {
        socket?.emit("leaveChannel", channelId)
        Timber.d("Left channel: $channelId")
    }

    /**
     * Send typing indicator
     */
    fun sendTyping(recipientId: String? = null, channelId: String? = null, isTyping: Boolean) {
        val data = JSONObject().apply {
            put("isTyping", isTyping)
            recipientId?.let { put("recipientId", it) }
            channelId?.let { put("channelId", it) }
        }
        socket?.emit("typing", data)
    }

    /**
     * Mark messages as read
     */
    fun markAsRead(conversationId: String, senderId: String) {
        val data = JSONObject().apply {
            put("conversationId", conversationId)
            put("senderId", senderId)
        }
        socket?.emit("markAsRead", data)
    }

    /**
     * Disconnect from Socket.IO
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        currentUserId = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _typingUsers.value = emptyMap()
        _onlineUsers.value = emptyList()
        Timber.d("Socket disconnected and cleaned up")
    }

    /**
     * Check if socket is connected
     */
    fun isConnected(): Boolean = socket?.connected() == true

    // ═══════════════════════════════════════════════════════════════════════════
    // PARSING HELPERS
    // ═══════════════════════════════════════════════════════════════════════════


    private fun parseDirectMessage(json: JSONObject): Message {
        val senderJson = json.optJSONObject("sender")
        
        return Message(
            id = json.optString("_id", ""),
            sender = senderJson?.let { parseUser(it) },
            receiver = json.optString("receiver", ""),  // Now just a string ID
            conversation = json.optString("conversation", ""),
            content = json.optString("content", ""),
            messageType = json.optString("messageType", "text"),
            delivered = json.optBoolean("delivered", false),
            read = json.optBoolean("read", false),
            createdAt = json.optString("createdAt", "").takeIf { it.isNotEmpty() }
        )
    }

    private fun parseChannelMessage(json: JSONObject): ChannelMessage {
        val senderJson = json.optJSONObject("sender")
        
        return ChannelMessage(
            id = json.optString("_id", ""),
            channel = json.optString("channel", ""),
            sender = senderJson?.let { parseUser(it) },
            content = json.optString("content", ""),
            messageType = json.optString("messageType", "text"),
            isPinned = json.optBoolean("isPinned", false),
            createdAt = json.optString("createdAt", "").takeIf { it.isNotEmpty() }
        )
    }

    private fun parseUser(json: JSONObject): User {
        return User(
            _id = json.optString("_id", ""),
            username = json.optString("username", ""),
            displayName = json.optString("displayName", ""),
            avatar = json.optString("avatar", ""),
            role = json.optString("role", "member"),
            isOnline = json.optBoolean("isOnline", false)
        )
    }
}

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

data class TypingInfo(
    val channelId: String?,
    val recipientId: String?
)

data class OnlineUserInfo(
    val userId: String,
    val status: String,
    val joinedAt: String
)

data class MessagesReadEvent(
    val conversationId: String,
    val readerId: String
)
