package com.example.alpha_chat_native

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.example.alpha_chat_native.ui.theme.AlphaChatNativeTheme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.alpha_chat_native.Presentation.Navigation.Routes
import com.example.alpha_chat_native.ui.screens.*
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable edge to edge")
        }

        setContent {
            AlphaChatNativeTheme {
                Surface(modifier = Modifier) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = Routes.SplashScreen) {
                        composable<Routes.SplashScreen> {
                            SplashScreen(
                                onNavigateToNext = { isLoggedIn ->
                                    if (isLoggedIn) {
                                        nav.navigate(Routes.HomeScreen) {
                                            popUpTo(Routes.SplashScreen) { inclusive = true }
                                        }
                                    } else {
                                        // Go directly to LoginScreen, skip redundant WelcomeScreen
                                        nav.navigate(Routes.LOGIN) {
                                            popUpTo(Routes.SplashScreen) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                        composable<Routes.UserRegistrationScreen> {
                            UserRegistrationScreen(
                                onRegisterSuccess = {
                                    nav.navigate(Routes.HomeScreen) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    nav.navigate(Routes.LOGIN)
                                }
                            )
                        }
                        composable<Routes.LOGIN> {
                            LoginScreen(
                                onLoginSuccess = {
                                    nav.navigate(Routes.HomeScreen) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable<Routes.HomeScreen> {
                            HomeScreen(
                                onConversationClick = { chatId ->
                                    nav.navigate(Routes.CHAT(chatId))
                                },
                                onNewChatClick = {
                                    nav.navigate(Routes.SelectUserScreen)
                                },
                                onLogout = {
                                    nav.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.HomeScreen) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable<Routes.SelectUserScreen> {
                            SelectUserScreen(
                                onUserSelected = { chatId ->
                                    nav.navigate(Routes.CHAT(chatId)) {
                                        popUpTo(Routes.SelectUserScreen) { inclusive = true }
                                    }
                                },
                                onBack = {
                                    nav.popBackStack()
                                }
                            )
                        }
                        composable<Routes.CHAT> { backStackEntry ->
                            val chatRoute: Routes.CHAT = backStackEntry.toRoute()
                            ChatScreen(
                                chatId = chatRoute.chatId,
                                onBack = {
                                    nav.popBackStack()
                                }
                            )
                        }
                        // ProfileScreen removed as per request
                        /*
                        composable<Routes.ProfileScreen> {
                             ...
                        }
                        */
                        composable<Routes.CommunityScreen> {
                            CommunityScreen()
                        }
                    }
                }
            }
        }
    }
}
