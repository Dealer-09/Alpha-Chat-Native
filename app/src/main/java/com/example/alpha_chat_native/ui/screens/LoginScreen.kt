package com.example.alpha_chat_native.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alpha_chat_native.R
import com.example.alpha_chat_native.ui.viewmodels.LoginState
import com.example.alpha_chat_native.ui.viewmodels.LoginViewModel
import timber.log.Timber
import kotlin.random.Random

// --- Colors from SplashScreen ---
private val SplashBackground = Color(0xFF012106)
private val SplashPrimary = Color(0xFF07AD52)
private val SplashSecondary = Color(0xFF04450F)
private val GitHubBlack = Color(0xFF24292e)

private data class LoginParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit = {},  // Not used anymore - GitHub OAuth only
    viewModel: LoginViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    
    var showWebView by remember { mutableStateOf(false) }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }
            is LoginState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicParticleBackground()

        if (showWebView) {
            // GitHub OAuth WebView
            GitHubOAuthWebView(
                oAuthUrl = viewModel.getOAuthUrl(),
                onCookiesCaptured = { cookies ->
                    viewModel.handleOAuthCallback(cookies)
                    showWebView = false
                },
                onCancel = { showWebView = false },
                isCallbackUrl = { url -> viewModel.isOAuthCallback(url) }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TerminalLoginWindow(
                    loginState = loginState,
                    onGithubSignIn = { showWebView = true }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GitHubOAuthWebView(
    oAuthUrl: String,
    onCookiesCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    isCallbackUrl: (String) -> Boolean
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground)
    ) {
        // Back button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Loading indicator
        var isLoading by remember { mutableStateOf(true) }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SplashPrimary
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(false)
                    
                    // Set custom User Agent for backend detection
                    settings.userAgentString = "AlphaChatMobile"
                    
                    // Enable cookies
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            Timber.d("WebView loaded: $url")
                            
                            // Check if we landed on the mobile success page
                            url?.let {
                                if (it.contains("/mobile/success")) {
                                    val cookies = CookieManager.getInstance()
                                        .getCookie("https://alphachat-v2-backend.onrender.com")
                                    if (cookies != null && cookies.contains("connect.sid")) {
                                        Timber.d("OAuth success! Cookies: ${cookies.take(50)}...")
                                        onCookiesCaptured(cookies)
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            Timber.d("WebView navigating to: $url")

                            // Intercept mobile success page redirect
                            if (url.contains("/mobile/success") || url.contains("/api/auth/mobile/success")) {
                                // Let it load - we'll capture cookies in onPageFinished
                                // This ensures the session cookie is fully set
                                return false
                            }
                            
                            return false
                        }
                    }

                    // Load OAuth URL
                    loadUrl(oAuthUrl)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp)  // Leave room for back button
        )
    }
}

@Composable
fun TerminalLoginWindow(
    loginState: LoginState,
    onGithubSignIn: () -> Unit
) {
    val terminalBg = Brush.linearGradient(
        listOf(
            Color(0xFF232526).copy(alpha = 0.9f),
            Color(0xFF1E130C).copy(alpha = 0.9f),
            Color(0xFF012106).copy(alpha = 0.8f)
        )
    )
    
    val borderColor = SplashPrimary
    val textColor = Color.White
    val accentColor = SplashPrimary
    val secondaryAccent = SplashSecondary

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
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "alpha-chat@terminal ~ login",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SplashPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                HorizontalDivider(color = borderColor, thickness = 1.dp)

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AlphaChat",
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
                        text = "$ git auth login --with-github",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SplashPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Sign in with your GitHub account to continue. Your profile will be synced from GitHub.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // GitHub Login Button
                    Button(
                        onClick = onGithubSignIn,
                        colors = ButtonDefaults.buttonColors(containerColor = GitHubBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = loginState !is LoginState.Loading
                    ) {
                        if (loginState is LoginState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // GitHub icon (using text as fallback)
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_github),
                                    contentDescription = "GitHub Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continue with GitHub",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "By continuing, you agree to sync with AlphaChat servers",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicParticleBackground() {
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
            LoginParticle(
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
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashPrimary, Color.Transparent)
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
                .size(200.dp)
                .scale(pulseScale2)
                .alpha(pulseAlpha2)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashSecondary, Color.Transparent)
                ),
                radius = size.minDimension / 2
            )
        }
    }
}
