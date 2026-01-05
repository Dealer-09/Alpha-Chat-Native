package com.example.alpha_chat_native.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.alpha_chat_native.data.models.Message
import com.example.alpha_chat_native.data.models.User
import com.example.alpha_chat_native.vm.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// --- Colors ---
private val SplashBackground = Color(0xFF012106)
private val SplashPrimary = Color(0xFF07AD52)
private val SplashSecondary = Color(0xFF04450F)
private val NeonGreen = Color(0xFF39FF14)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String = "global",
    vm: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    // Load messages when chatId changes
    LaunchedEffect(chatId) {
        vm.loadMessages(chatId)
    }

    val messages by vm.messages.collectAsState(initial = emptyList())
    val currentUserId = vm.currentUserId
    
    // Attempt to find the "other" user to display their name/avatar
    val users by vm.users.collectAsState()
    
    // chatId is already the recipientId (other user's ID)
    // No need to split - just use it directly
    val targetUserId = remember(chatId) {
        if (chatId == "global") null else chatId
    }

    val otherUser = remember(users, targetUserId) {
        if (targetUserId == null) {
            User(_id = "global", displayName = "Global Chat")
        } else {
            users.find { it.id == targetUserId } ?: User(displayName = "Chat", _id = targetUserId)
        }
    }

    val isDark = isSystemInDarkTheme()

    // Dynamic Background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            SplashBackground,
            Color(0xFF020E2A),
            SplashBackground
        )
    )

    // Chat Bubble Colors (Neon Style)
    val myMessageColor = SplashPrimary.copy(alpha = 0.8f)
    val otherMessageColor = Color(0xFF020E2A).copy(alpha = 0.8f) // Dark glass
    val textColor = Color.White
    
    // Accent Colors
    val accentColor = NeonGreen
    val textFieldColor = Color(0xFF020E2A).copy(alpha = 0.5f)

    val displayImage = if (otherUser.imageUrl.isNotEmpty()) otherUser.imageUrl else "https://ui-avatars.com/api/?name=${otherUser.displayName}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent, 
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(model = displayImage),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = otherUser.displayName, 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                if (chatId != "global") {
                                    Text(
                                        text = "Online", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Call */ }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = textColor)
                        }
                        IconButton(onClick = { /* Call */ }) {
                            Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = textColor)
                        }
                        IconButton(onClick = { /* Menu */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF020E2A).copy(alpha = 0.9f),
                        titleContentColor = textColor,
                        navigationIconContentColor = textColor,
                        actionIconContentColor = textColor
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.Bottom,
                    reverseLayout = true
                ) {                    items(messages.reversed()) { msg ->
                        val isMe = msg.fromId == currentUserId
                        
                        MessageBubble(
                            message = msg,
                            isMe = isMe,
                            backgroundColor = if (isMe) myMessageColor else otherMessageColor,
                            textColor = textColor
                        )
                    }
                }

                ChatInputArea(
                    vm = vm,
                    targetUserId = targetUserId,
                    textFieldColor = textFieldColor,
                    accentColor = accentColor,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    backgroundColor: Color,
    textColor: Color
) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 16.sp
                )
                
                message.timestamp?.let { ts ->
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text(
                        text = sdf.format(ts),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    vm: ChatViewModel,
    targetUserId: String?,
    textFieldColor: Color,
    accentColor: Color,
    textColor: Color
) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .background(Color(0xFF020E2A).copy(alpha = 0.9f))
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message", color = textColor.copy(alpha=0.6f)) },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = textFieldColor,
                unfocusedContainerColor = textFieldColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            maxLines = 4
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        FloatingActionButton(
            onClick = {
                if (text.isNotBlank() && targetUserId != null) {
                    vm.send(text, toId = targetUserId)
                    text = ""
                }
            },
            containerColor = accentColor,
            contentColor = Color(0xFF012106),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}
