package com.example.alpha_chat_native.data.repository

import com.example.alpha_chat_native.data.models.*
import com.example.alpha_chat_native.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for all chat operations.
 * Uses Retrofit for API calls and Socket.IO for real-time messaging.
 * Replaces the previous Firebase-based implementation.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val api: AlphaChatApi,
    private val socketManager: SocketManager,
    private val tokenManager: TokenManager
) {
    private var _currentUser: User? = null
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())

    // ═══════════════════════════════════════════════════════════════════════════
    // AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if user is authenticated and load user data
     */
    suspend fun checkAuth(): User? {
        return try {
            Timber.d("checkAuth: Making API call...")
            val response = api.checkAuth()
            Timber.d("checkAuth: isAuthenticated=${response.isAuthenticated}, user=${response.user?.username}")
            if (response.isAuthenticated && response.user != null) {
                _currentUser = response.user
                Timber.d("checkAuth: User ID = ${response.user.id}")
                tokenManager.saveUserId(response.user.id)
                socketManager.connect(response.user.id)
                response.user
            } else {
                Timber.w("checkAuth: Not authenticated or no user returned")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Auth check failed with exception")
            null
        }
    }

    /**
     * Get current user from API
     */
    suspend fun getCurrentUser(): User? {
        return try {
            val response = api.getCurrentUser()
            if (response.success && response.user != null) {
                _currentUser = response.user
                _currentUser
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Get current user failed")
            null
        }
    }

    /**
     * Handle OAuth callback - save cookies and verify auth
     */
    suspend fun handleOAuthCallback(cookies: String): User? {
        Timber.d("handleOAuthCallback: Saving cookies (length=${cookies.length})")
        Timber.d("handleOAuthCallback: Cookie preview: ${cookies.take(80)}...")
        tokenManager.saveSessionCookie(cookies)
        Timber.d("handleOAuthCallback: Cookies saved, calling checkAuth")
        return checkAuth()
    }

    /**
     * Logout - clear session and disconnect socket
     */
    suspend fun logout() {
        try {
            api.logout()
        } catch (e: Exception) {
            Timber.e(e, "Logout API call failed")
        } finally {
            socketManager.disconnect()
            tokenManager.clearSession()
            _currentUser = null
            _users.value = emptyList()
            _conversations.value = emptyList()
            _channels.value = emptyList()
        }
    }

    /**
     * Get cached current user ID
     */
    fun currentUserId(): String? = _currentUser?.id ?: tokenManager.getUserId()

    /**
     * Get cached current user
     */
    fun currentUser(): User? = _currentUser

    /**
     * Check if user has a session
     */
    fun hasSession(): Boolean = tokenManager.hasSession()

    // ═══════════════════════════════════════════════════════════════════════════
    // USERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all users as Flow
     */
    fun observeUsers(): Flow<List<User>> = _users.asStateFlow()

    /**
     * Fetch all users from API
     */
    suspend fun fetchUsers(): List<User> {
        return try {
            val response = api.getAllUsers()
            if (response.success) {
                val users = response.users ?: emptyList()
                _users.value = users
                users
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetch users failed")
            emptyList()
        }
    }

    /**
     * Search users
     */
    suspend fun searchUsers(query: String): List<User> {
        return try {
            val response = api.searchUsers(query)
            if (response.success) {
                response.users ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Search users failed")
            emptyList()
        }
    }

    /**
     * Observe online users from Socket.IO
     */
    fun observeOnlineUsers(): Flow<List<OnlineUserInfo>> = socketManager.onlineUsers

    // ═══════════════════════════════════════════════════════════════════════════
    // DIRECT MESSAGES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all conversations as Flow
     */
    fun observeConversations(): Flow<List<Conversation>> = _conversations.asStateFlow()

    /**
     * Fetch all conversations with other user populated
     */
    suspend fun fetchConversations(): List<Conversation> {
        return try {
            val response = api.getConversations()
            if (response.success) {
                val conversations = response.conversations ?: emptyList()
                
                // Populate otherUser for each conversation
                val currentId = currentUserId()
                val populatedConvos = conversations.map { convo ->
                    val other = convo.participants.find { it.id != currentId }
                    convo.copy(otherUser = other)
                }
                
                _conversations.value = populatedConvos
                populatedConvos
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetch conversations failed")
            emptyList()
        }
    }

    /**
     * Get conversation with specific user
     */
    suspend fun getConversation(recipientId: String, page: Int = 1): ConversationDetail? {
        return try {
            val response = api.getConversation(recipientId, page)
            
            if (response.success && response.conversation != null) {
                ConversationDetail(
                    conversation = response.conversation,
                    messages = response.messages ?: emptyList(),
                    pagination = response.pagination
                )
            } else {
                Timber.w("Get conversation: success=${response.success}, conversation=${response.conversation != null}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Get conversation failed: ${e.message}")
            null
        }
    }

    /**
     * Send direct message
     */
    suspend fun sendDirectMessage(recipientId: String, content: String, messageType: String = "text"): Message? {
        return try {
            val request = SendMessageRequest(content, messageType)
            val response = api.sendDirectMessage(recipientId, request)
            if (response.success) {
                response.messageData
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Send message failed")
            null
        }
    }

    /**
     * Observe incoming direct messages from Socket.IO
     */
    fun observeIncomingMessages(): Flow<Message> = socketManager.directMessages

    /**
     * Observe typing indicators
     */
    fun observeTyping(): Flow<Map<String, TypingInfo>> = socketManager.typingUsers

    /**
     * Send typing indicator
     */
    fun sendTypingIndicator(recipientId: String, isTyping: Boolean) {
        socketManager.sendTyping(recipientId = recipientId, isTyping = isTyping)
    }

    /**
     * Mark messages as read
     */
    fun markMessagesAsRead(conversationId: String, senderId: String) {
        socketManager.markAsRead(conversationId, senderId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHANNELS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all channels as Flow
     */
    fun observeChannels(): Flow<List<Channel>> = _channels.asStateFlow()

    /**
     * Fetch all channels
     */
    suspend fun fetchChannels(): List<Channel> {
        return try {
            val response = api.getAllChannels()
            if (response.success) {
                val channels = response.channels ?: emptyList()
                _channels.value = channels
                channels
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetch channels failed")
            emptyList()
        }
    }

    /**
     * Get channel details with messages
     */
    suspend fun getChannel(slug: String, page: Int = 1): ChannelDetail? {
        return try {
            val response = api.getChannel(slug, page)
            if (response.success && response.channel != null) {
                // Build ChannelDetail from separate response fields
                ChannelDetail(
                    channel = response.channel,
                    messages = response.messages,
                    pagination = response.pagination
                )
            } else {
                Timber.w("Get channel: success=${response.success}, channel=${response.channel != null}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Get channel failed")
            null
        }
    }

    /**
     * Join a channel
     */
    suspend fun joinChannel(channelId: String): Boolean {
        return try {
            val response = api.joinChannel(channelId)
            if (response.success) {
                socketManager.joinChannel(channelId)
                fetchChannels() // Refresh channel list
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Join channel failed")
            false
        }
    }

    /**
     * Leave a channel
     */
    suspend fun leaveChannel(channelId: String): Boolean {
        return try {
            val response = api.leaveChannel(channelId)
            if (response.success) {
                socketManager.leaveChannel(channelId)
                fetchChannels()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Leave channel failed")
            false
        }
    }

    /**
     * Send message to channel
     */
    suspend fun sendChannelMessage(channelId: String, content: String, messageType: String = "text"): ChannelMessage? {
        return try {
            val request = SendMessageRequest(content, messageType)
            val response = api.sendChannelMessage(channelId, request)
            if (response.success) {
                response.message
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Send channel message failed")
            null
        }
    }

    /**
     * Observe incoming channel messages from Socket.IO
     */
    fun observeChannelMessages(): Flow<ChannelMessage> = socketManager.channelMessages

    /**
     * Join channel socket room for real-time updates
     */
    fun joinChannelRoom(channelId: String) {
        socketManager.joinChannel(channelId)
    }

    /**
     * Leave channel socket room
     */
    fun leaveChannelRoom(channelId: String) {
        socketManager.leaveChannel(channelId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SOCKET CONNECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get socket connection state
     */
    fun observeConnectionState(): Flow<ConnectionState> = socketManager.connectionState

    /**
     * Check if socket is connected
     */
    fun isSocketConnected(): Boolean = socketManager.isConnected()

    /**
     * Reconnect socket (if user is authenticated)
     */
    fun reconnectSocket() {
        currentUserId()?.let { userId ->
            socketManager.connect(userId)
        }
    }
}
