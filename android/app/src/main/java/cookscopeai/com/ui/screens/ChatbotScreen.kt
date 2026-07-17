package cookscopeai.com.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cookscopeai.com.data.ChatService
import cookscopeai.com.ui.components.SmoothInput
import cookscopeai.com.R
import androidx.compose.ui.res.painterResource
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

private data class ChatBubble(
    val id: Long,
    val role: String,
    val content: String,
    val isTyping: Boolean = false,
)

private val allSuggestionPool = listOf(
    "What can I make with chicken and rice?",
    "Substitute for eggs in baking",
    "Easy 15-minute dinner ideas",
    "How to perfectly sear a steak?",
    "Healthy vegetarian meal prep ideas",
    "Best herbs for tomato sauce",
    "How to make homemade pasta?",
    "What wine pairs with salmon?",
    "Quick high-protein breakfast",
    "How to fix salty soup?",
    "Ideas for leftover rice",
    "Simple sauce for grilled fish",
)

@Composable
fun ChatbotScreen() {
    val messages = remember { mutableStateListOf<ChatBubble>() }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val chatService = remember { ChatService() }
    val listState = rememberLazyListState()

    var currentSuggestions by remember { mutableStateOf(allSuggestionPool.shuffled().take(4)) }

    // Randomize suggestions every time the chat tab comes back into view
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentSuggestions = allSuggestionPool.shuffled().take(4)
    }

    // Welcome message
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                ChatBubble(
                    id = 0,
                    role = "assistant",
                    content = "Hi! I'm CookScope, your AI culinary assistant. Ask me about recipes, ingredient substitutions, cooking techniques, or meal ideas. What are you cooking tonight?",
                )
            )
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return

        val userMsg = ChatBubble(
            id = System.currentTimeMillis(),
            role = "user",
            content = text,
        )
        messages.add(userMsg)
        isLoading = true

        // Add typing indicator
        val typingId = System.currentTimeMillis() + 1
        messages.add(ChatBubble(id = typingId, role = "assistant", content = "", isTyping = true))

        scope.launch {
            val history = messages
                .filter { !it.isTyping && it.content.isNotBlank() }
                .dropLast(1) // exclude the typing indicator
                .map { ChatService.ChatMessage(it.role, it.content) }

            val result = chatService.sendMessage(history) { reply ->
                // Replace typing indicator with real reply
                val index = messages.indexOfFirst { it.id == typingId }
                if (index >= 0) {
                    messages[index] = ChatBubble(
                        id = typingId,
                        role = "assistant",
                        content = reply,
                    )
                }
            }

            if (result.isFailure) {
                val index = messages.indexOfFirst { it.id == typingId }
                if (index >= 0) {
                    messages[index] = ChatBubble(
                        id = typingId,
                        role = "assistant",
                        content = "I'm having trouble connecting right now. Please try again in a moment.",
                    )
                }
            }

            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            // Header
            ChatHeader()

            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = messages,
                    key = { it.id },
                ) { bubble ->
                    ChatMessageBubble(
                        bubble = bubble,
                        maxWidthFraction = 0.75f,
                    )
                }

                // Suggestion chips (only shown at start)
                if (messages.size <= 1 && !isLoading) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Try asking:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        currentSuggestions.forEach { chip ->
                            SuggestionChip(
                                text = chip,
                                onClick = { sendMessage(chip) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Smooth glass input bar
            SmoothInput(
                onSend = { sendMessage(it) },
                placeholder = "Ask about recipes, techniques...",
                isLoading = isLoading,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ChatHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // AI chef mascot
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chatbot_mascot),
                    contentDescription = "CookScope AI Chef",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "CookScope Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldPrimary),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Online · Ready to help",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    bubble: ChatBubble,
    maxWidthFraction: Float,
) {
    val isUser = bubble.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldLight, GoldPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        if (bubble.isTyping) {
            // Typing indicator bubble — glass
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp,
                        )
                    )
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { i ->
                        DotPulse(delayMs = i * 200)
                    }
                }
            }
        } else {
            // Message bubble
            Box(
                modifier = Modifier
                    .widthIn(max = androidx.compose.ui.platform.LocalConfiguration.current.run {
                        (screenWidthDp * maxWidthFraction).dp
                    })
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp,
                        )
                    )
                    .background(
                        if (isUser) {
                            Brush.horizontalGradient(
                                colors = listOf(GoldPrimary, GoldDeep)
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                )
                            )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(
                    text = bubble.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun DotPulse(delayMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delayMs,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale$delayMs",
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                GoldPrimary.copy(alpha = 0.4f * scale + 0.2f)
            )
            .padding(0.dp),
    )
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Restaurant,
            contentDescription = null,
            tint = GoldPrimary.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
