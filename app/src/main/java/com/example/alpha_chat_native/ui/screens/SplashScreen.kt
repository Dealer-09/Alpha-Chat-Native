package com.example.alpha_chat_native.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alpha_chat_native.R
import com.example.alpha_chat_native.ui.theme.AlphaBackground
import com.example.alpha_chat_native.ui.theme.AlphaPrimary
import com.example.alpha_chat_native.ui.theme.AlphaSecondary
import com.example.alpha_chat_native.vm.ChatViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen(
    onNavigateToNext: (isLoggedIn: Boolean) -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Animations
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutBack),
        label = "logoScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "contentAlpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000) // Show splash for 2 seconds
        val isLoggedIn = vm.isLoggedIn()
        onNavigateToNext(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SplashBackground,
                        Color(0xFF020E2A),
                        SplashBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shared Particle Background Logic
        SplashParticleBackground()

        // Glass Card Container
        Card(
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize()
                .alpha(contentAlpha)
                .scale(logoScale),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.50f) // Semi-transparent glass
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo Container
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow behind logo
                    Canvas(modifier = Modifier.size(140.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SplashPrimary.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            ),
                            radius = 70.dp.toPx()
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Alpha Chat",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Secure. Fast. Native.",
                    fontSize = 16.sp,
                    color = SplashPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(48.dp))

                LoadingDots(
                    color = SplashPrimary
                )
            }
        }
    }
}

@Composable
private fun SplashParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulse Animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Particles Setup
    val particles = remember {
        List(25) {
            SplashParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 6 + 2,
                speed = Random.nextFloat() * 0.005f + 0.002f,
                alpha = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleProgress"
    )

    // Draw Particles
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
    }

    // Draw Pulsing Circles (Background Decor)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashPrimary.copy(alpha=0.3f), Color.Transparent)
                ),
                radius = size.minDimension / 2
            )
        }
        
        val pulseScale2 by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, delayMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseScale2"
        )
        val pulseAlpha2 by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, delayMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseAlpha2"
        )

        Canvas(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale2)
                .alpha(pulseAlpha2)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashSecondary.copy(alpha=0.3f), Color.Transparent)
                ),
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
private fun LoadingDots(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot$index"
            )

            Canvas(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(dotAlpha)
            ) {
                drawCircle(color = color)
            }
        }
    }
}

private data class SplashParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

// Color aliases — mapped from the AlphaChat theme
private val SplashBackground = AlphaBackground
private val SplashPrimary = AlphaPrimary
private val SplashSecondary = AlphaSecondary

