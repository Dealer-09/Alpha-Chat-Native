package com.example.alpha_chat_native.di

import android.content.Context
import androidx.room.Room
import com.example.alpha_chat_native.data.local.AlphaChatDatabase
import com.example.alpha_chat_native.data.local.dao.ConversationDao
import com.example.alpha_chat_native.data.local.dao.MessageDao
import com.example.alpha_chat_native.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room database dependencies.
 * Provides database instance and DAOs for offline persistence.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAlphaChatDatabase(
        @ApplicationContext context: Context
    ): AlphaChatDatabase {
        return Room.databaseBuilder(
            context,
            AlphaChatDatabase::class.java,
            "alphachat_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AlphaChatDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AlphaChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: AlphaChatDatabase): ConversationDao {
        return database.conversationDao()
    }
}
