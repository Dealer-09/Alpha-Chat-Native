package com.example.alpha_chat_native.data.remote

import com.example.alpha_chat_native.data.models.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

/**
 * Retrofit API interface for AlphaChat-V2 backend.
 * Base URL: https://alphachat-v2-backend.onrender.com/
 */
interface AlphaChatApi {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AUTH ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get current authenticated user
     * Returns user data if session is valid
     */
    @GET("api/auth/me")
    suspend fun getCurrentUser(): CurrentUserResponse
    
    /**
     * Check authentication status
     */
    @GET("api/auth/check")
    suspend fun checkAuth(): AuthCheckResponse
    
    /**
     * Logout current user
     */
    @POST("api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
    
    // ═══════════════════════════════════════════════════════════════════════════
    // USER ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all users (for DM list)
     */
    @GET("api/users")
    suspend fun getAllUsers(): ApiResponse<UsersListResponse>
    
    /**
     * Get online users
     */
    @GET("api/users/online")
    suspend fun getOnlineUsers(): OnlineUsersResponse
    
    /**
     * Search users by username or display name
     */
    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): ApiResponse<UsersListResponse>
    
    /**
     * Get team members (cofounders and core team)
     */
    @GET("api/users/team")
    suspend fun getTeamMembers(): TeamMembersResponse
    
    /**
     * Get user profile by username
     */
    @GET("api/users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): ApiResponse<UserResponse>
    
    /**
     * Update current user's status
     */
    @PATCH("api/users/status")
    suspend fun updateStatus(@Body request: UpdateStatusRequest): ApiResponse<Unit>
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHANNEL ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all channels with membership status
     */
    @GET("api/channels")
    suspend fun getAllChannels(): ApiResponse<ChannelsListResponse>
    
    /**
     * Get channel details with messages
     */
    @GET("api/channels/{slug}")
    suspend fun getChannel(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ChannelDetailResponse
    
    /**
     * Join a channel
     */
    @POST("api/channels/{channelId}/join")
    suspend fun joinChannel(@Path("channelId") channelId: String): ApiResponse<Unit>
    
    /**
     * Leave a channel
     */
    @POST("api/channels/{channelId}/leave")
    suspend fun leaveChannel(@Path("channelId") channelId: String): ApiResponse<Unit>
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIRECT MESSAGE ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all conversations for current user
     */
    @GET("api/messages/dm/conversations")
    suspend fun getConversations(): ApiResponse<ConversationsListResponse>
    
    /**
     * Get conversation with specific user (creates if doesn't exist)
     */
    @GET("api/messages/dm/{recipientId}")
    suspend fun getConversation(
        @Path("recipientId") recipientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<ConversationDetail>
    
    /**
     * Send direct message
     */
    @POST("api/messages/dm/{recipientId}")
    suspend fun sendDirectMessage(
        @Path("recipientId") recipientId: String,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageResponse>
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHANNEL MESSAGE ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Send message to channel
     */
    @POST("api/messages/channel/{channelId}")
    suspend fun sendChannelMessage(
        @Path("channelId") channelId: String,
        @Body request: SendMessageRequest
    ): ChannelMessageResponse
    
    /**
     * Toggle reaction on message
     */
    @PATCH("api/messages/reaction/{messageId}")
    suspend fun toggleReaction(
        @Path("messageId") messageId: String,
        @Body request: ReactionRequest
    ): ApiResponse<Unit>
}

// ═══════════════════════════════════════════════════════════════════════════════
// RESPONSE MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Generic API response wrapper
 * Used for endpoints that return various data types
 * Each endpoint uses different fields, so all fields are nullable
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    @Json(name = "error") val errorMessage: String? = null,
    // Different endpoints return data in different fields
    @Json(name = "users") val users: List<User>? = null,
    @Json(name = "channels") val channels: List<Channel>? = null,
    @Json(name = "channel") val channel: ChannelDetail? = null,
    @Json(name = "conversations") val conversations: List<Conversation>? = null,
    @Json(name = "conversation") val conversation: Conversation? = null,  // Changed from ConversationDetail
    @Json(name = "messages") val messages: List<Message>? = null,  // Separate messages field
    @Json(name = "message") val messageData: Message? = null,  // Backend returns "message" for sent DM
    @Json(name = "pagination") val pagination: Pagination? = null,  // Pagination info
    // For channel messages - uses "channelMessages" to avoid conflict
    @Json(name = "channelMessages") val channelMessages: List<ChannelMessage>? = null
)

/**
 * Generic API response with success flag
 */
@JsonClass(generateAdapter = true)
data class BaseResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * Response from /api/auth/me
 * Returns { success, user: {...} }
 */
@JsonClass(generateAdapter = true)
data class CurrentUserResponse(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null
)

/**
 * Response from /api/users
 * Returns { success, users: [...] }
 */
@JsonClass(generateAdapter = true)
data class UsersListResponse(
    val success: Boolean,
    val users: List<User> = emptyList()
)

/**
 * Wrapper for backwards compatibility
 */
@JsonClass(generateAdapter = true)
data class UserResponse(
    val user: User
)

@JsonClass(generateAdapter = true)
data class ChannelsListResponse(
    val channels: List<Channel>
)

@JsonClass(generateAdapter = true)
data class ConversationsListResponse(
    val conversations: List<Conversation>
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    val message: Message
)

@JsonClass(generateAdapter = true)
data class ChannelMessageResponse(
    val success: Boolean,
    val message: ChannelMessage? = null
)

/**
 * Response from /api/channels/{slug}
 * Backend returns channel and messages as separate top-level fields
 */
@JsonClass(generateAdapter = true)
data class ChannelDetailResponse(
    val success: Boolean,
    val channel: ChannelWithMembers? = null,
    val messages: List<ChannelMessage> = emptyList(),
    val pagination: Pagination? = null
)

@JsonClass(generateAdapter = true)
data class AuthCheckResponse(
    val isAuthenticated: Boolean,
    val user: User? = null
)

@JsonClass(generateAdapter = true)
data class OnlineUsersResponse(
    val success: Boolean,
    val count: Int,
    val users: List<User>
)

@JsonClass(generateAdapter = true)
data class TeamMembersResponse(
    val success: Boolean,
    val cofounders: List<User>,
    val coreTeam: List<User>
)

// ═══════════════════════════════════════════════════════════════════════════════
// REQUEST MODELS
// ═══════════════════════════════════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class UpdateStatusRequest(
    val status: String  // online, offline, away, busy
)

@JsonClass(generateAdapter = true)
data class ReactionRequest(
    val emoji: String,
    val messageType: String = "dm"  // dm or channel
)
