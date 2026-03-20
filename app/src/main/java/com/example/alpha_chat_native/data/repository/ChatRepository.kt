package com.example.alpha_chat_native.data.repository

import com.example.alpha_chat_native.data.local.dao.ConversationDao
import com.example.alpha_chat_native.data.local.dao.MessageDao
import com.example.alpha_chat_native.data.local.dao.UserDao
import com.example.alpha_chat_native.data.local.entities.toEntity
import com.example.alpha_chat_native.data.local.entities.toModel
import com.example.alpha_chat_native.data.models.*
import com.example.alpha_chat_native.data.remote.*
import android.content.Context
import androidx.work.*
import com.example.alpha_chat_native.worker.MessageSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for all chat operations.
 * Uses Retrofit for API calls and Socket.IO for real-time messaging.
 * Implements offline-first pattern with Room database caching.
 */
@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AlphaChatApi,
    private val socketManager: SocketManager,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    private var _currentUser: User? = null
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())

    private val _syncedMessages = MutableSharedFlow<Pair<String, Message>>()
    val syncedMessages = _syncedMessages.asSharedFlow()

    suspend fun notifyMessageSynced(oldId: String, newMessage: Message) {
        _syncedMessages.emit(Pair(oldId, newMessage))
    }

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
     * Fetch all users - offline-first pattern
     * 1. Load cached data immediately
     * 2. Try to fetch fresh data from API
     * 3. Cache new data on success
     */
    suspend fun fetchUsers(): List<User> {
        // Load from cache first
        try {
            val cached = userDao.getAll()
            if (cached.isNotEmpty()) {
                val users = cached.map { it.toModel() }
                _users.value = users
                Timber.d("Loaded ${users.size} users from cache")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load cached users")
        }
        
        // Try to fetch fresh data from API
        return try {
            val response = api.getAllUsers()
            if (response.success) {
                val users = response.users ?: emptyList()
                _users.value = users
                // Cache for offline access
                try {
                    userDao.insertAll(users.map { it.toEntity() })
                    Timber.d("Cached ${users.size} users")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cache users")
                }
                users
            } else {
                _users.value // Return cached data
            }
        } catch (e: Exception) {
            Timber.w(e, "Fetch users failed, using cached data")
            _users.value // Return cached data on network error
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
     * Fetch all conversations - offline-first pattern
     * 1. Load cached conversations immediately
     * 2. Try to fetch fresh data from API
     * 3. Cache new data on success
     */
    suspend fun fetchConversations(): List<Conversation> {
        // Load from cache first
        try {
            val cached = conversationDao.getAll()
            if (cached.isNotEmpty()) {
                val convos = cached.map { it.toModel() }
                _conversations.value = convos
                Timber.d("Loaded ${convos.size} conversations from cache")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load cached conversations")
        }
        
        // Try to fetch fresh data from API
        return try {
            val response = api.getConversations()
            if (response.success) {
                val conversations = response.conversations ?: emptyList()
                
                // Populate otherUser for each conversation
                // Backend pre-populates it; derive from participants as fallback
                val currentId = currentUserId()
                val populatedConvos = conversations.map { convo ->
                    if (convo.otherUser != null) {
                        convo // already populated from JSON
                    } else {
                        val other = convo.participants.find { it.id != currentId }
                        convo.copy(otherUser = other)
                    }
                }
                
                _conversations.value = populatedConvos
                
                // Cache for offline access
                try {
                    conversationDao.insertAll(populatedConvos.map { it.toEntity() })
                    Timber.d("Cached ${populatedConvos.size} conversations")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cache conversations")
                }
                
                populatedConvos
            } else {
                _conversations.value // Return cached data
            }
        } catch (e: Exception) {
            Timber.w(e, "Fetch conversations failed, using cached data")
            _conversations.value // Return cached data on network error
        }
    }

    /**
     * Get conversation with specific user - offline-first pattern
     * 1. Load cached messages immediately
     * 2. Try to fetch fresh data from API
     * 3. Cache new messages on success
     */
    suspend fun getConversation(recipientId: String, page: Int = 1): ConversationDetail? {
        // Load from cache first
        var cachedMessages: List<Message> = emptyList()
        try {
            val cached = messageDao.getByRecipient(recipientId)
            if (cached.isNotEmpty()) {
                cachedMessages = cached.map { it.toModel() }
                Timber.d("Loaded ${cachedMessages.size} cached messages for recipient $recipientId")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load cached messages")
        }
        
        // Try to fetch fresh data from API
        return try {
            val response = api.getConversation(recipientId, page)
            
            if (response.success && response.conversation != null) {
                val messages = response.messages ?: emptyList()
                
                // Cache messages for offline access
                try {
                    if (messages.isNotEmpty()) {
                        messageDao.insertAll(messages.map { it.toEntity() })
                        Timber.d("Cached ${messages.size} messages")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cache messages")
                }
                // Fetch any pending messages from cache to append to the fresh API list
                val pendingMessages = cachedMessages.filter { it.syncStatus == "PENDING" }
                val mergedMessages = if (pendingMessages.isNotEmpty()) {
                    (messages + pendingMessages).sortedBy { it.createdAt }
                } else {
                    messages
                }
                
                ConversationDetail(
                    conversation = response.conversation,
                    messages = mergedMessages,
                    pagination = response.pagination
                )
            } else {
                // Return cached data if API fails
                if (cachedMessages.isNotEmpty()) {
                    Timber.d("API returned no data, using ${cachedMessages.size} cached messages")
                    ConversationDetail(
                        conversation = Conversation(id = recipientId),
                        messages = cachedMessages,
                        pagination = null
                    )
                } else {
                    Timber.w("Get conversation: success=${response.success}, conversation=${response.conversation != null}")
                    null
                }
            }
        } catch (e: Exception) {
            // Return cached data on network error
            if (cachedMessages.isNotEmpty()) {
                Timber.w(e, "API failed, returning ${cachedMessages.size} cached messages")
                ConversationDetail(
                    conversation = Conversation(id = recipientId),
                    messages = cachedMessages,
                    pagination = null
                )
            } else {
                Timber.e(e, "Get conversation failed, no cached data: ${e.message}")
                null
            }
        }
    }

    /**
     * Internal helper to enqueue the sync worker
     */
    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<MessageSyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "OfflineMessageSync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    /**
     * Send direct message
     */
    suspend fun sendDirectMessage(recipientId: String, content: String, messageType: String = "text"): Message? {
        val tempId = "pending_\${UUID.randomUUID()}"
        
        // Use cached user or construct a minimal one for offline UI rendering
        var sender = currentUser()
        if (sender == null) {
            val cachedUserId = tokenManager.getUserId()
            if (cachedUserId != null) {
                sender = try {
                    userDao.getById(cachedUserId)?.toModel()
                } catch (e: Exception) {
                    null
                } ?: User(_id = cachedUserId, username = "Me")
            }
        }
        
        // 1. Create a pending Message object
        val pendingMessage = Message(
            id = tempId,
            sender = sender,
            receiver = recipientId,
            conversation = recipientId, // Simplified, the real conversation ID comes from backend
            content = content,
            messageType = messageType,
            syncStatus = "PENDING",
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(java.util.Date())
        )

        // 2. Save it locally as PENDING
        try {
            messageDao.insert(pendingMessage.toEntity())
        } catch (e: Exception) {
            Timber.w(e, "Failed to caching pending message")
        }

        // 3. Try to send it remotely
        return try {
            val request = SendMessageRequest(content, messageType)
            val response = api.sendDirectMessage(recipientId, request)
            if (response.success && response.messageData != null) {
                // Update local to SENT
                messageDao.updateSyncStatus(oldId = tempId, newId = response.messageData.id, status = "SENT")
                response.messageData
            } else {
                enqueueSyncWorker()
                pendingMessage
            }
        } catch (e: Exception) {
            Timber.e(e, "Send message failed - queued for offline")
            enqueueSyncWorker()
            pendingMessage
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
                // Fetch pending messages from local cache for this channel
                var pendingMessages: List<ChannelMessage> = emptyList()
                try {
                    val cached = messageDao.getByConversation(response.channel.id)
                    pendingMessages = cached
                        .map { it.toModel() }
                        .filter { it.syncStatus == "PENDING" }
                        .map { msg ->
                            ChannelMessage(
                                id = msg.id,
                                channel = msg.conversation,
                                sender = msg.sender,
                                content = msg.content,
                                messageType = msg.messageType,
                                syncStatus = msg.syncStatus,
                                createdAt = msg.createdAt
                            )
                        }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load cached pending messages for channel")
                }
                
                val allMessages = if (pendingMessages.isNotEmpty()) {
                    (response.messages + pendingMessages).sortedBy { it.createdAt }
                } else {
                    response.messages
                }

                // Build ChannelDetail from separate response fields
                ChannelDetail(
                    channel = response.channel,
                    messages = allMessages,
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
        val tempId = "pending_\${UUID.randomUUID()}"
        
        // Use cached user or construct a minimal one for offline UI rendering
        var sender = currentUser()
        if (sender == null) {
            val cachedUserId = tokenManager.getUserId()
            if (cachedUserId != null) {
                sender = try {
                    userDao.getById(cachedUserId)?.toModel()
                } catch (e: Exception) {
                    null
                } ?: User(_id = cachedUserId, username = "Me")
            }
        }
        
        // 1. Create a pending ChannelMessage 
        val pendingMessage = ChannelMessage(
            id = tempId,
            channel = channelId,
            sender = sender,
            content = content,
            messageType = messageType,
            syncStatus = "PENDING",
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(java.util.Date())
        )
        
        // Room Entity requires a regular Message format, so we convert it
        val pendingEntity = Message(
            id = tempId,
            sender = sender,
            receiver = "",
            conversation = channelId,
            content = content,
            messageType = messageType,
            syncStatus = "PENDING",
            createdAt = pendingMessage.createdAt
        ).toEntity()

        // 2. Save locally
        try {
            messageDao.insert(pendingEntity)
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache pending channel message")
        }

        // 3. Try to send remotely
        return try {
            val request = SendMessageRequest(content, messageType)
            val response = api.sendChannelMessage(channelId, request)
            if (response.success && response.message != null) {
                messageDao.updateSyncStatus(oldId = tempId, newId = response.message.id, status = "SENT")
                response.message
            } else {
                enqueueSyncWorker()
                pendingMessage
            }
        } catch (e: Exception) {
            Timber.e(e, "Send channel message failed - queued for offline")
            enqueueSyncWorker()
            pendingMessage
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
