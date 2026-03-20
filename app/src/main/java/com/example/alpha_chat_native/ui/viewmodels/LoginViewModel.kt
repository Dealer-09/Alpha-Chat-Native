package com.example.alpha_chat_native.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_chat_native.data.models.User
import com.example.alpha_chat_native.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Login screen.
 * Handles GitHub OAuth authentication flow.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Check if already authenticated
        checkExistingSession()
    }

    /**
     * Check for existing session on app start
     */
    private fun checkExistingSession() {
        if (repository.hasSession()) {
            viewModelScope.launch {
                _loginState.value = LoginState.Loading
                try {
                    val user = repository.checkAuth()
                    if (user != null) {
                        _currentUser.value = user
                        _loginState.value = LoginState.Success
                    } else {
                        _loginState.value = LoginState.Idle
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Session check failed")
                    _loginState.value = LoginState.Idle
                }
            }
        }
    }

    /**
     * Handle OAuth callback after GitHub login
     * Called when WebView captures the session cookies
     */
    fun handleOAuthCallback(cookies: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val user = repository.handleOAuthCallback(cookies)
                if (user != null) {
                    _currentUser.value = user
                    _loginState.value = LoginState.Success
                    Timber.d("OAuth callback successful: ${user.username}")
                } else {
                    _loginState.value = LoginState.Error("Authentication failed. Please try again.")
                }
            } catch (e: Exception) {
                Timber.e(e, "OAuth callback failed")
                _loginState.value = LoginState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    /**
     * Reset state after navigation or error dismissal
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * Get the GitHub OAuth URL for mobile
     * Uses dedicated mobile endpoint that redirects to backend's success page
     */
    fun getOAuthUrl(): String {
        return "https://alphachat-v2.onrender.com/api/auth/github/mobile"
    }

    /**
     * Check if a URL is the OAuth success callback
     * Mobile flow redirects to /api/auth/mobile/success on the backend
     */
    fun isOAuthCallback(url: String): Boolean {
        return url.contains("/mobile/success") || 
               url.contains("/api/auth/mobile")
    }
}

/**
 * Login screen state
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
    
    // Removed PasswordResetEmailSent - no longer using email/password
}
