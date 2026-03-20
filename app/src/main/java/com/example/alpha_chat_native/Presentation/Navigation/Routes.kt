package com.example.alpha_chat_native.Presentation.Navigation

import kotlinx.serialization.Serializable

sealed class Routes {

    @Serializable
    data object SplashScreen : Routes()
    @Serializable
    data object UserRegistrationScreen : Routes()
    @Serializable
    data object HomeScreen : Routes()
    @Serializable
    data object CommunityScreen : Routes()
    @Serializable
    data object LOGIN : Routes()
    @Serializable
    data class CHAT(val chatId: String) : Routes()
    @Serializable
    data object SelectUserScreen : Routes()

    // UpdateScreen, CallScreen, and ProfileScreen are not standalone NavGraph destinations:
    // UpdateScreen and CallScreen are rendered as tabs inside HomeScreen.
    // ProfileScreen was removed per request (commented out in MainActivity).
}
