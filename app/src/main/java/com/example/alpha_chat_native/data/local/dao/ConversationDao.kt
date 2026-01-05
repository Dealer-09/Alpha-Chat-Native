package com.example.alpha_chat_native.data.local.dao

import androidx.room.*
import com.example.alpha_chat_native.data.local.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached conversations.
 */
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastActivity DESC")
    fun observeAll(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations ORDER BY lastActivity DESC")
    suspend fun getAll(): List<ConversationEntity>
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getById(conversationId: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE otherUserId = :otherUserId")
    suspend fun getByOtherUser(otherUserId: String): ConversationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}
