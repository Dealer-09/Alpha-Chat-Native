package com.example.alpha_chat_native.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.alpha_chat_native.data.models.Conversation
import com.example.alpha_chat_native.vm.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// --- Theme Colors ---
private val SplashPrimary = Color(0xFF07AD52)
private val NeonGreen = Color(0xFF39FF14)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNewChatClick: () -> Unit,
    onConversationClick: (String) -> Unit,
    onLogout: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    val conversations by vm.conversations.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val textColor = Color.White
    val accentColor = SplashPrimary

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold, color = textColor) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search", tint = textColor) }
                    Box {
                        IconButton(onClick = { showMenu = true }) { 
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = textColor) 
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },

                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    vm.signOut {
                                        onLogout()
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = accentColor,
                contentColor = Color(0xFF012106)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(conversations) { conversation ->
                ConversationItem(conversation = conversation) {
                    // Pass otherUser's ID - the API uses recipientId, not conversation._id
                    conversation.otherUser?.id?.let { recipientId ->
                        onConversationClick(recipientId)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val user = conversation.otherUser ?: return

    val cardBgColor = Color(0xFF020E2A).copy(alpha = 0.5f)
    val textColor = Color.White
    val secondaryTextColor = Color.White.copy(alpha=0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageModel = user.imageUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.displayName}" }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.dp, SplashPrimary.copy(alpha = 0.5f)), CircleShape)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageModel),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifBlank { "Unknown User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.lastMessageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Show last activity time if available
            val formattedTime = conversation.lastActivity?.let { timestamp ->
                try {
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(timestamp)
                    date?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it) }
                } catch (e: Exception) {
                    null
                }
            }
            
            formattedTime?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
        }
    }
}
