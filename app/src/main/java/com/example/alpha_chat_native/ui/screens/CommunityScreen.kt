package com.example.alpha_chat_native.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alpha_chat_native.data.models.Channel
import com.example.alpha_chat_native.ui.viewmodels.CommunityViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// ALPHACHAT TERMINAL COLORS (matching web frontend)
// ═══════════════════════════════════════════════════════════════════════════
private val AlphaBackground = Color(0xFF0d1117)
private val AlphaGreen = Color(0xFF39ff14)
private val AlphaGreenDim = Color(0xFF39ff14).copy(alpha = 0.7f)
private val AlphaGreenBorder = Color(0xFF39ff14).copy(alpha = 0.2f)
private val AlphaGreenHover = Color(0xFF39ff14).copy(alpha = 0.1f)
private val AlphaTextPrimary = Color.White
private val AlphaTextSecondary = Color(0xFF888888)
private val AlphaCardBg = Color(0xFF0d1117)

/**
 * UI model for chat messages (kept for compatibility)
 */
data class ChatMessage(
    val id: String,
    val author: String,
    val avatarColor: Color,
    val timestamp: String,
    val content: String,
    val isCode: Boolean = false,
    val language: String = "text",
    val isAdmin: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val channels by viewModel.channels.collectAsState()
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter channels by search
    val filteredChannels = channels.filter { 
        it.name.lowercase().contains(searchQuery.lowercase()) 
    }

    // Scaffold to match ChatListScreen structure
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
             if (selectedChannel == null) {
                 // Custom Compact Top Bar
                 Row(
                     modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     AlphaHeader()
                 }
             }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        if (selectedChannel == null) {
            // Channel List View
            ChannelListView(
                channels = filteredChannels,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onChannelClick = { selectedChannel = it }
            )
        } else {
            // Channel Chat View - use id as key (matches socket listener)
            val messages = viewModel.getMessages(
                channelId = selectedChannel!!.id,
                channelSlug = selectedChannel!!.slug
            )
            
            ChannelChatView(
                channel = selectedChannel!!,
                messages = messages,
                onBack = { selectedChannel = null },
                onSendMessage = { text, isCode ->
                    // Use channel.id for both API call and local storage (matches socket listener)
                    viewModel.sendMessage(
                        channelId = selectedChannel!!.id,
                        text = text,
                        isCode = isCode,
                        isAdmin = false
                    )
                }
            )
        }
    }
}
}

// ═══════════════════════════════════════════════════════════════════════════
// CHANNEL LIST VIEW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChannelListView(
    channels: List<Channel>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header moved to TopAppBar
        // AlphaHeader()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        AlphaSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Search channels..."
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Channel List
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No channels found" else "Loading channels...",
                    color = AlphaTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(channels) { channel ->
                    ChannelListItem(
                        channel = channel,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Terminal icon
        Text(
            text = ">_",
            color = AlphaGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Alpha",
            color = AlphaTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Chats",
            color = AlphaGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AlphaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, AlphaGreenBorder, RoundedCornerShape(8.dp))
            .background(AlphaBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = AlphaGreenDim,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = AlphaTextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(AlphaGreen),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = AlphaTextSecondary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = AlphaGreenDim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChannelListItem(
    channel: Channel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hash symbol
        Text(
            text = "#",
            color = AlphaGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Channel name
        Text(
            text = channel.name,
            color = AlphaTextPrimary,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Member count if available
        if (channel.memberCount > 0) {
            Text(
                text = "${channel.memberCount}",
                color = AlphaTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = AlphaTextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CHANNEL CHAT VIEW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChannelChatView(
    channel: Channel,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (String, Boolean) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlphaBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AlphaBackground)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, AlphaGreenBorder)
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AlphaGreen
                )
            }
            
            Text(
                text = "#",
                color = AlphaGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = channel.name,
                color = AlphaTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "# ${channel.name}",
                                color = AlphaGreen,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Welcome! Start the conversation.",
                                color = AlphaTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AlphaBackground)
                .border(1.dp, AlphaGreenBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(AlphaGreenHover, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textStyle = TextStyle(
                    color = AlphaTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(AlphaGreen),
                decorationBox = { innerTextField ->
                    Box {
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Message #${channel.name}",
                                color = AlphaTextSecondary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText, false)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (messageText.isNotBlank()) AlphaGreen else AlphaGreenBorder,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (messageText.isNotBlank()) Color.Black else AlphaTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Author and timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(message.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.author.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = message.author,
                color = if (message.isAdmin) AlphaGreen else AlphaTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = message.timestamp,
                color = AlphaTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Message content
        if (message.isCode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .background(Color(0xFF1a1a2e), RoundedCornerShape(8.dp))
                    .border(1.dp, AlphaGreenBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = AlphaGreen,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Text(
                text = message.content,
                color = AlphaTextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 40.dp)
            )
        }
    }
}
