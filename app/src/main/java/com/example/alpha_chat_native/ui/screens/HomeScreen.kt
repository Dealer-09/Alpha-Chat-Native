package com.example.alpha_chat_native.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// --- Theme Colors ---
private val SplashBackground = Color(0xFF012106)
private val SplashPrimary = Color(0xFF07AD52)
private val SplashSecondary = Color(0xFF04450F)

// --- AlphaChat Terminal Colors (matching CommunityScreen) ---
private val AlphaBackground = Color(0xFF0d1117)
private val AlphaTextPrimary = Color.White

@Composable
fun HomeScreen(
    onConversationClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    // Background based on Login/Splash theme
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            SplashBackground,
            Color(0xFF020E2A),
            SplashBackground
        )
    )

    // Navigation Bar Colors
    val navBarColor = Color(0xFF020E2A).copy(alpha = 0.9f)
    val navContentColor = Color.White
    val indicatorColor = SplashPrimary.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        )
        // Shared Background Animation
        HomeParticleBackground()

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = if (selectedIndex == 2) AlphaBackground else navBarColor,
                    contentColor = if (selectedIndex == 2) AlphaTextPrimary else navContentColor
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Chats") },
                        label = { Text("Chats") },
                        selected = selectedIndex == 0,
                        onClick = { selectedIndex = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = indicatorColor,
                            selectedIconColor = SplashPrimary,
                            selectedTextColor = SplashPrimary,
                            unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                            unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Update, contentDescription = "Updates") },
                        label = { Text("Updates") },
                        selected = selectedIndex == 1,
                        onClick = { selectedIndex = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = indicatorColor,
                            selectedIconColor = SplashPrimary,
                            selectedTextColor = SplashPrimary,
                            unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                            unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Community") },
                        label = { Text("Community") },
                        selected = selectedIndex == 2,
                        onClick = { selectedIndex = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = indicatorColor,
                            selectedIconColor = SplashPrimary,
                            selectedTextColor = SplashPrimary,
                            unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                            unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Call, contentDescription = "Calls") },
                        label = { Text("Calls") },
                        selected = selectedIndex == 3,
                        onClick = { selectedIndex = 3 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = indicatorColor,
                            selectedIconColor = SplashPrimary,
                            selectedTextColor = SplashPrimary,
                            unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                            unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedIndex) {
                    0 -> ChatListScreen(
                        onConversationClick = onConversationClick,
                        onNewChatClick = onNewChatClick,
                        onLogout = onLogout
                    )
                    1 -> UpdateScreen()
                    2 -> CommunityScreen()
                    3 -> CallScreen()
                }
            }
        }
    }
}

@Composable
fun HomeParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val particles = remember {
        List(15) {
            HomeParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4 + 2,
                speed = Random.nextFloat() * 0.003f + 0.001f,
                alpha = Random.nextFloat() * 0.4f + 0.1f
            )
        }
    }

    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val animatedY = (particle.y + particleProgress * particle.speed * 100) % 1f
            drawCircle(
                color = SplashPrimary.copy(alpha = particle.alpha),
                radius = particle.size.dp.toPx(),
                center = Offset(
                    x = particle.x * size.width,
                    y = animatedY * size.height
                )
            )
        }
        
        // Subtle ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SplashSecondary.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.2f),
                radius = 400f * pulseScale
            ),
            radius = 400f * pulseScale,
            center = Offset(size.width * 0.8f, size.height * 0.2f)
        )
        
         drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SplashPrimary.copy(alpha = 0.1f), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.8f),
                radius = 300f * pulseScale
            ),
            radius = 300f * pulseScale,
            center = Offset(size.width * 0.2f, size.height * 0.8f)
        )
    }
}

private data class HomeParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
