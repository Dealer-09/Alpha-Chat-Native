package com.example.alpha_chat_native.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alpha_chat_native.data.models.User

/**
 * Room entity for cached users.
 * Mirrors the User data model for offline persistence.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val githubId: String? = null,
    val username: String = "",
    val displayName: String = "",
    val email: String? = null,
    val avatar: String = "",
    val profileUrl: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    val role: String = "member",
    val isOnline: Boolean = false,
    val status: String = "offline",
    val lastSeen: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

// Extension functions to convert between Entity and Model
fun UserEntity.toModel(): User = User(
    _id = id,
    githubId = githubId,
    username = username,
    displayName = displayName,
    email = email,
    avatar = avatar,
    profileUrl = profileUrl,
    bio = bio,
    company = company,
    location = location,
    role = role,
    isOnline = isOnline,
    status = status,
    lastSeen = lastSeen
)

fun User.toEntity(): UserEntity = UserEntity(
    id = this.id,
    githubId = githubId,
    username = username,
    displayName = displayName,
    email = email,
    avatar = avatar,
    profileUrl = profileUrl,
    bio = bio,
    company = company,
    location = location,
    role = role,
    isOnline = isOnline,
    status = status,
    lastSeen = lastSeen
)
