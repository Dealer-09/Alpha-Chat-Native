package com.example.alpha_chat_native.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_chat_native.data.models.Conversation
import com.example.alpha_chat_native.data.models.Message
import com.example.alpha_chat_native.data.models.User
import com.example.alpha_chat_native.data.models.Channel
import com.example.alpha_chat_native.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main ViewModel for chat functionality.
 * Uses ChatRepository which connects to Express backend via Retrofit + Socket.IO.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    private val _currentChatId = MutableStateFlow("global")
    val currentChatId = _currentChatId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    // Users from API
    val users = repo.observeUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Conversations from API with otherUser populated
    val conversations = repo.observeConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Channels from API
    val channels = repo.observeChannels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val currentUserId: String?
        get() = repo.currentUserId()

    init {
        // Listen for incoming messages from Socket.IO
        viewModelScope.launch {
            repo.observeIncomingMessages().collect { message ->
                val chatPartnerId = _currentChatId.value
                
                // Add to messages if it's from the current chat partner
                // (Sender doesn't receive socket events - they get message from API response)
                val isFromChatPartner = chatPartnerId != null && message.fromId == chatPartnerId
                
                if (isFromChatPartner) {
                    // Prepend so newest is at index 0 (bottom with reverseLayout=true)
                    _messages.value = listOf(message) + _messages.value
                }
            }
        }

        // Initial data fetch if logged in
        if (repo.hasSession()) {
            refreshData()
        }

        // Listen for locally synced messages
        viewModelScope.launch {
            repo.syncedMessages.collect { (oldId, newMessage) ->
                val currentMessages = _messages.value
                val updatedList = currentMessages.map { if (it.id == oldId) newMessage else it }
                _messages.value = updatedList
            }
        }
    }

    /**
     * Refresh all data from API
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                repo.fetchUsers()
                repo.fetchConversations()
                repo.fetchChannels()
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing data")
            }
        }
    }

    /**
     * Load messages for a specific chat/conversation
     * chatId is the recipientId (other user's ID)
     */
    fun loadMessages(chatId: String) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val detail = repo.getConversation(chatId)
                // Reverse so newest is at index 0 — required for reverseLayout=true in LazyColumn
                _messages.value = (detail?.messages ?: emptyList()).reversed()
            } catch (e: Exception) {
                Timber.e(e, "Error loading messages")
                _error.value = "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Send a message
     */
    fun send(text: String, toId: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val message = repo.sendDirectMessage(toId, text)
                if (message != null) {
                    // Prepend so newest appears at bottom (reverseLayout=true, index 0 = bottom)
                    _messages.value = listOf(message) + _messages.value
                }
            } catch (e: Exception) {
                _error.value = "Failed to send message"
                Timber.e(e, "Error sending message")
            }
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return repo.hasSession()
    }

    /**
     * Sign out
     */
    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.logout()
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Sign out failed"
                Timber.e(e, "Error signing out")
            }
        }
    }

    /**
     * Update profile — limited for GitHub OAuth users.
     * GitHub controls display name and avatar; they cannot be changed through this app.
     * Emits an error state to inform the caller rather than silently faking success.
     */
    fun updateProfile(name: String, imageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = "Profile is managed by GitHub. Update your name and avatar on github.com and re-login to sync changes."
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Send typing indicator
     */
    fun sendTyping(recipientId: String, isTyping: Boolean) {
        repo.sendTypingIndicator(recipientId, isTyping)
    }

    /**
     * Mark messages as read
     */
    fun markAsRead(conversationId: String, senderId: String) {
        repo.markMessagesAsRead(conversationId, senderId)
    }

}
