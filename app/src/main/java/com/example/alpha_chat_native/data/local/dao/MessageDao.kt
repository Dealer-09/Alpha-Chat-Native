package com.example.alpha_chat_native.data.local.dao

import androidx.room.*
import com.example.alpha_chat_native.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached messages.
 */
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversation = :conversationId ORDER BY createdAt ASC")
    fun observeByConversation(conversationId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversation = :conversationId ORDER BY createdAt ASC")
    suspend fun getByConversation(conversationId: String): List<MessageEntity>
    
    /**
     * Get messages by recipient ID (for DM conversations)
     * A message is for a conversation if sender or receiver matches the other user
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :otherUserId OR receiver = :otherUserId)
        ORDER BY createdAt ASC
    """)
    suspend fun getByRecipient(otherUserId: String): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: String): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversation = :conversationId")
    suspend fun deleteByConversation(conversationId: String)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
