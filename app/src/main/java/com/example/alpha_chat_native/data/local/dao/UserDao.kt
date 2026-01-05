package com.example.alpha_chat_native.data.local.dao

import androidx.room.*
import com.example.alpha_chat_native.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached users.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun observeAll(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    suspend fun getAll(): List<UserEntity>
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getById(userId: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    
    @Query("DELETE FROM users")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
