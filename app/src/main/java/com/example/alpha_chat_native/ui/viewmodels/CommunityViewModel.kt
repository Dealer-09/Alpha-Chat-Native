package com.example.alpha_chat_native.ui.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_chat_native.data.models.Channel
import com.example.alpha_chat_native.data.models.ChannelMessage
import com.example.alpha_chat_native.data.repository.ChatRepository
import com.example.alpha_chat_native.ui.screens.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Discord Colors
private val DiscordAccent = Color(0xFF5865F2)
private val DiscordRed = Color(0xFFED4245)

/**
 * ViewModel for Community screen.
 * Now uses ChatRepository which connects to Express backend.
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    // Channel Name -> List of Messages
    private val _channelMessages = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()
    val channelMessages: Map<String, List<ChatMessage>> = _channelMessages

    // Channel Name -> Permissions (true = allowed, false = restricted)
    private val _channelPermissions = mutableStateMapOf<String, Boolean>()
    val channelPermissions: Map<String, Boolean> = _channelPermissions

    // Actual channels from backend
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    init {
        // Initialize default local permissions (can be fetched from backend later)
        _channelPermissions["announcements"] = false
        _channelPermissions["rules"] = false
        _channelPermissions["general"] = true
        _channelPermissions["web-dev"] = true
        _channelPermissions["android"] = true

        // Fetch channels from backend
        fetchChannels()

        // Listen for incoming channel messages from Socket.IO
        viewModelScope.launch {
            repository.observeChannelMessages().collect { message ->
                addMessageToChannel(message.channel, message)
            }
        }
    }

    /**
     * Fetch channels from backend
     */
    private fun fetchChannels() {
        viewModelScope.launch {
            try {
                val channelList = repository.fetchChannels()
                _channels.value = channelList
            } catch (e: Exception) {
                Timber.e(e, "Error fetching channels")
            }
        }
    }

    /**
     * Get messages for a channel
     */
    /**
     * Get messages for a channel
     * @param channelId Used as key for local storage (matches socket events)
     * @param channelSlug Used for API call to fetch history
     */
    fun getMessages(channelId: String, channelSlug: String): SnapshotStateList<ChatMessage> {
        // If we don't have a list for this channel yet, create it and load messages
        if (!_channelMessages.containsKey(channelId)) {
            val list = SnapshotStateList<ChatMessage>()
            _channelMessages[channelId] = list
            loadChannelMessages(channelId, channelSlug, list)
        }
        return _channelMessages[channelId]!!
    }

    /**
     * Load messages from backend API
     */
    private fun loadChannelMessages(channelId: String, channelSlug: String, list: SnapshotStateList<ChatMessage>) {
        viewModelScope.launch {
            try {
                // Join the channel's socket room for real-time updates
                repository.joinChannelRoom(channelId)

                // Fetch messages from API using SLUG (backend requirement)
                val detail = repository.getChannel(channelSlug)
                if (detail != null) {
                    list.clear()
                    detail.messages.forEach { msg ->
                        list.add(convertToUIMessage(msg))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading messages for channel: $channelId")
            }
        }
    }

    /**
     * Add message from Socket.IO to local list
     * Includes duplicate check to handle case where sender gets their own message echoed back
     */
    private fun addMessageToChannel(channelId: String, message: ChannelMessage) {
        val list = _channelMessages[channelId] ?: return
        // Only add if not already present (avoid duplicates when socket echoes back sender's message)
        if (list.none { it.id == message.id }) {
            list.add(convertToUIMessage(message))
        }
    }

    /**
     * Convert backend ChannelMessage to UI ChatMessage
     */
    private fun convertToUIMessage(msg: ChannelMessage): ChatMessage {
        val sender = msg.sender
        val authorName = sender?.displayName ?: sender?.username ?: "Unknown"
        val isAdmin = sender?.role == "cofounder" || sender?.role == "core"
        
        val timestampStr = try {
            msg.createdAt?.let {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(it)
                SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault()).format(date ?: Date())
            } ?: "Now"
        } catch (e: Exception) {
            "Now"
        }

        return ChatMessage(
            id = msg.id,
            author = authorName,
            avatarColor = if (isAdmin) DiscordRed else DiscordAccent,
            timestamp = timestampStr,
            content = msg.content,
            isCode = msg.messageType == "code",
            language = msg.codeLanguage ?: "text",
            isAdmin = isAdmin
        )
    }

    /**
     * Get permission for a channel
     */
    fun getPermission(channel: String): Boolean {
        return _channelPermissions.getOrPut(channel) { true }
    }

    /**
     * Toggle permission for a channel (admin only)
     */
    fun togglePermission(channel: String) {
        val current = getPermission(channel)
        _channelPermissions[channel] = !current
        // In a real implementation, this would update the backend
    }

    /**
     * Send a message to a channel
     * @param channelId The channel's _id for both API call and local storage key
     *                  (matches socket listener which uses message.channel = channelId)
     */
    fun sendMessage(channelId: String, text: String, isCode: Boolean, isAdmin: Boolean) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            try {
                val messageType = if (isCode) "code" else "text"
                val result = repository.sendChannelMessage(channelId, text, messageType)
                
                if (result != null) {
                    // Add the message locally from API response for immediate display
                    // Use channelId as key (matches socket listener's message.channel)
                    val list = _channelMessages[channelId]
                    if (list != null) {
                        // Only add if not already present (avoid duplicates)
                        if (list.none { it.id == result.id }) {
                            list.add(convertToUIMessage(result))
                        }
                    }
                    Timber.d("Channel message sent and added: ${result.id}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending channel message")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Leave all channel rooms when ViewModel is cleared
        _channelMessages.keys.forEach { channelId ->
            repository.leaveChannelRoom(channelId)
        }
    }
}
