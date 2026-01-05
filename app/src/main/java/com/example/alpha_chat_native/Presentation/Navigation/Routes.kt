package com.example.alpha_chat_native.Presentation.Navigation

import kotlinx.serialization.Serializable

sealed class Routes {

    @Serializable
    data object SplashScreen:Routes()
    @Serializable
    data object UserRegistrationScreen:Routes()
    @Serializable
    data object HomeScreen:Routes()
    @Serializable
    data object UpdateScreen:Routes()
    @Serializable
    data object CommunityScreen:Routes()
    @Serializable
    data object CallScreen:Routes()
    @Serializable
    data object LOGIN:Routes()
    @Serializable
    data class CHAT(val chatId: String):Routes()
    @Serializable
    data object SelectUserScreen:Routes()
    @Serializable
    data object ProfileScreen:Routes()
}
