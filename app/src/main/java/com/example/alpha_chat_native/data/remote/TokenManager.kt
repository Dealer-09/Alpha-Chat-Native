package com.example.alpha_chat_native.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alpha_chat_prefs")

/**
 * Manages session cookie storage for authenticated API requests.
 * The cookie is captured from GitHub OAuth WebView callback.
 *
 * Uses an in-memory cache to avoid runBlocking on the OkHttp I/O thread.
 * The cache is warmed once on creation; writes update both DataStore and the cache atomically.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SESSION_COOKIE_KEY = stringPreferencesKey("session_cookie")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    // In-memory cache — avoids runBlocking on the OkHttp dispatch thread
    @Volatile private var cachedCookie: String? = null
    @Volatile private var cachedUserId: String? = null

    init {
        // Warm the cache once from DataStore on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStore.data.first()
                cachedCookie = prefs[SESSION_COOKIE_KEY]
                cachedUserId = prefs[USER_ID_KEY]
                Timber.d("TokenManager: Cache warmed — cookie=${cachedCookie != null}, userId=$cachedUserId")
            } catch (e: Exception) {
                Timber.e(e, "TokenManager: Failed to warm cache from DataStore")
            }
        }
    }

    /**
     * Save the session cookie from OAuth callback.
     * Updates both DataStore and the in-memory cache immediately.
     */
    suspend fun saveSessionCookie(cookie: String) {
        cachedCookie = cookie  // Update cache first for immediate availability
        context.dataStore.edit { prefs ->
            prefs[SESSION_COOKIE_KEY] = cookie
        }
    }

    /**
     * Get the session cookie for API requests.
     * Returns from in-memory cache — safe to call on any thread with no blocking.
     */
    fun getSessionCookie(): String? = cachedCookie

    /**
     * Save the current user ID for quick access.
     * Updates both DataStore and the in-memory cache immediately.
     */
    suspend fun saveUserId(userId: String) {
        cachedUserId = userId  // Update cache first
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    /**
     * Get the cached user ID.
     * Returns from in-memory cache — safe to call on any thread with no blocking.
     */
    fun getUserId(): String? = cachedUserId

    /**
     * Check if user has a session (cached — no I/O).
     */
    fun hasSession(): Boolean = cachedCookie != null

    /**
     * Clear all session data on logout.
     * Clears both in-memory cache and DataStore.
     */
    suspend fun clearSession() {
        cachedCookie = null
        cachedUserId = null
        context.dataStore.edit { it.clear() }
    }
}
