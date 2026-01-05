package com.example.alpha_chat_native.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alpha_chat_native.vm.ChatViewModel
import kotlin.random.Random

// --- Colors from SplashScreen ---
private val SplashBackground = Color(0xFF012106)
private val SplashPrimary = Color(0xFF07AD52)
private val SplashSecondary = Color(0xFF04450F)

@Composable
fun UserRegistrationScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    // 1. Dynamic Background from SplashScreen
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
            )
    ) {
        // Shared Background Logic
        RegistrationParticleBackground()

        // 2. Main Signup Container
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Terminal Window
            TerminalWindow(
                userName = userName,
                onUserNameChange = { userName = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onToggleShowPassword = { showPassword = !showPassword },
                isLoading = isLoading,
                error = error,
                onSignup = {
                    // Registration is no longer supported - GitHub OAuth is the only auth method
                    // This screen should redirect to LoginScreen for GitHub OAuth
                    onNavigateToLogin()
                },
                onNavigateToLogin = onNavigateToLogin
            )
        }
    }
}

// Reusing the exact background logic structure from Login/Splash
@Composable
fun RegistrationParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

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

    val particles = remember {
        List(20) {
            RegistrationParticle(
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

    // Canvas Layers
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

    Canvas(
        modifier = Modifier
            .size(200.dp)
            .scale(pulseScale)
            .alpha(pulseAlpha)
            .wrapContentSize(Alignment.Center) 
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SplashPrimary, Color.Transparent)
            ),
            radius = size.minDimension / 2
        )
    }
    
    // Second Pulse
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
            .size(200.dp)
            .scale(pulseScale2)
            .alpha(pulseAlpha2)
            .wrapContentSize(Alignment.Center)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SplashSecondary, Color.Transparent)
            ),
            radius = size.minDimension / 2
        )
    }
}

// Renamed data class to avoid conflict with LoginScreen.kt's Particle
private data class RegistrationParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun TerminalWindow(
    userName: String,
    onUserNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShowPassword: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onSignup: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Colors based on theme but adapted for the dark splash background
    val terminalBg = Brush.linearGradient(
        listOf(
            Color(0xFF232526).copy(alpha = 0.9f),
            Color(0xFF1E130C).copy(alpha = 0.9f),
            Color(0xFF012106).copy(alpha = 0.8f) // Deep Green tint
        )
    )
    
    val borderColor = SplashPrimary
    val textColor = Color.White
    val accentColor = SplashPrimary
    val secondaryAccent = SplashSecondary
    val inputBg = Color(0xFF000000).copy(alpha = 0.5f)
    val inputBorder = SplashPrimary.copy(alpha = 0.5f)
    val inputText = Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.background(terminalBg)
        ) {
            Column {
                // Terminal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Window dots
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "alpha-chat@terminal ~ register",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SplashPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                HorizontalDivider(color = borderColor, thickness = 1.dp)

                // Content Area
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JoinAlphaChat",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = secondaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "$ sudo useradd --developer --group=alpha-coders",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SplashPrimary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Form Inputs
                    TerminalInput(
                        value = userName,
                        onValueChange = onUserNameChange,
                        placeholder = "Username",
                        textColor = inputText,
                        bgColor = inputBg,
                        borderColor = inputBorder
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TerminalInput(
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = "Email",
                        textColor = inputText,
                        bgColor = inputBg,
                        borderColor = inputBorder
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TerminalInput(
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = "Password",
                        textColor = inputText,
                        bgColor = inputBg,
                        borderColor = inputBorder,
                        isPassword = true,
                        showPassword = showPassword,
                        onToggleShowPassword = onToggleShowPassword
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (error != null) {
                        Text(
                            text = "Error: $error",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = onSignup,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                        } else {
                            Text("EXECUTE SIGNUP", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onNavigateToLogin) {
                         Text("< Back to Login", color = secondaryAccent, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onToggleShowPassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = textColor.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedContainerColor = bgColor,
            unfocusedContainerColor = bgColor,
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor.copy(alpha = 0.5f)
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword && onToggleShowPassword != null) {
                IconButton(onClick = onToggleShowPassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = textColor
                    )
                }
            }
        }
    )
}
