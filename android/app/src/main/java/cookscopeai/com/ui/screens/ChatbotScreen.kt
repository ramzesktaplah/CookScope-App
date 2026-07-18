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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cookscopeai.com.R
import cookscopeai.com.data.ChatHistoryManager
import cookscopeai.com.data.ChatHistoryManager.ChatMessage as StoredChatMessage
import cookscopeai.com.data.ChatService
import cookscopeai.com.data.ChatUsageManager
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.ui.components.SmoothInput
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)

    val conversations by ChatHistoryManager.conversations.collectAsState()
    val activeId by ChatHistoryManager.activeId.collectAsState()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val usedCount by ChatUsageManager.used.collectAsState()
    val remainingCount by ChatUsageManager.remaining.collectAsState()

    val messages = remember { mutableStateListOf<ChatBubble>() }
    var isLoading by remember { mutableStateOf(false) }
    var rateLimitHit by remember { mutableStateOf(false) }
    val chatService = remember { ChatService() }
    val listState = rememberLazyListState()

    var currentSuggestions by remember { mutableStateOf(allSuggestionPool.shuffled().take(4)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentSuggestions = allSuggestionPool.shuffled().take(4)
        ChatUsageManager.refreshRemaining()
    }

    // Load the active conversation's messages into the UI when activeId changes.
    LaunchedEffect(activeId) {
        messages.clear()
        val active = ChatHistoryManager.activeConversation()
        if (active != null && active.messages.isNotEmpty()) {
            active.messages.forEach { m ->
                messages.add(
                    ChatBubble(
                        id = m.timestamp,
                        role = m.role,
                        content = m.content,
                    )
                )
            }
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        } else if (active == null) {
            // No active conversation — start fresh with a welcome bubble only.
        }
    }

    // Welcome message when there are no loaded messages.
    LaunchedEffect(messages.size, activeId) {
        if (messages.isEmpty()) {
            messages.add(
                ChatBubble(
                    id = 0L,
                    role = "assistant",
                    content = "Hi! I'm CookScope, your AI culinary assistant. Ask me about recipes, ingredient substitutions, cooking techniques, or meal ideas. What are we cooking?",
                )
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Trim history when premium status changes.
    LaunchedEffect(isPremium) {
        val limit = if (isPremium) 30 else 10
        ChatHistoryManager.trimToLimit(limit)
        ChatUsageManager.refreshRemaining()
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return

        // Rate limit check
        if (!ChatUsageManager.canSendMessage()) {
            rateLimitHit = true
            return
        }
        rateLimitHit = false

        val userMsg = ChatBubble(
            id = System.currentTimeMillis(),
            role = "user",
            content = text,
        )
        messages.add(userMsg)
        isLoading = true

        // Persist the user message
        ChatHistoryManager.appendMessage("user", text)

        val typingId = System.currentTimeMillis() + 1
        messages.add(ChatBubble(id = typingId, role = "assistant", content = "", isTyping = true))

        scope.launch {
            val history = messages
                .filter { !it.isTyping && it.content.isNotBlank() && it.id != 0L }
                .map { ChatService.ChatMessage(it.role, it.content) }

            val result = chatService.sendMessage(
                messages = history,
                onToken = { reply ->
                val index = messages.indexOfFirst { it.id == typingId }
                if (index >= 0) {
                    messages[index] = ChatBubble(
                        id = typingId,
                        role = "assistant",
                        content = reply,
                    )
                }
                ChatHistoryManager.appendMessage("assistant", reply)
            },
            )

            if (result.isFailure) {
                val index = messages.indexOfFirst { it.id == typingId }
                if (index >= 0) {
                    messages[index] = ChatBubble(
                        id = typingId,
                        role = "assistant",
                        content = "I'm having trouble connecting right now. Please try again in a moment.",
                    )
                }
            } else {
                // Count a successful exchange against the monthly limit.
                ChatUsageManager.increment()
            }

            isLoading = false
        }
    }

    fun startNewChat() {
        messages.clear()
        ChatHistoryManager.startNewConversation()
        scope.launch { drawerState.close() }
    }

    fun openConversation(id: String) {
        ChatHistoryManager.setActive(id)
        scope.launch { drawerState.close() }
    }

    fun deleteConversation(id: String) {
        if (id == activeId) {
            messages.clear()
        }
        ChatHistoryManager.deleteConversation(id)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                conversations = conversations,
                activeId = activeId,
                isPremium = isPremium,
                onNewChat = ::startNewChat,
                onOpen = ::openConversation,
                onDelete = ::deleteConversation,
            )
        },
    ) {
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
                // Header with history button (left) and new-chat button (right)
                ChatHeader(
                    isPremium = isPremium,
                    usedCount = usedCount,
                    remainingCount = remainingCount,
                    onOpenHistory = { scope.launch { drawerState.open() } },
                    onNewChat = ::startNewChat,
                )

                // Rate-limit banner
                if (rateLimitHit) {
                    RateLimitBanner(
                        isPremium = isPremium,
                        remaining = remainingCount,
                        onUpgrade = { /* could navigate to paywall */ },
                    )
                }

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

                    if (messages.size <= 1 && !isLoading && !rateLimitHit) {
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

                // Usage footer (subtle)
                UsageFooter(
                    usedCount = usedCount,
                    limit = if (isPremium) 100 else 5,
                    isPremium = isPremium,
                )

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
}

@Composable
private fun ChatHeader(
    isPremium: Boolean,
    usedCount: Int,
    remainingCount: Int,
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // History button — upper left
        IconButton(onClick = onOpenHistory) {
            Icon(
                Icons.Default.History,
                contentDescription = "Chat history",
                tint = GoldPrimary,
            )
        }

        // Mascot + title
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_chatbot_mascot),
                contentDescription = "CookScope AI Chef",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                val tierLabel = if (isPremium) "Premium · CS Donna AI" else "Free · CS Myth AI"
                Text(
                    "$tierLabel · $remainingCount left",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // New chat button — upper right
        IconButton(onClick = onNewChat) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "New chat",
                tint = GoldPrimary,
            )
        }
    }
}

@Composable
private fun RateLimitBanner(
    isPremium: Boolean,
    remaining: Int,
    onUpgrade: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    if (isPremium) "Monthly limit reached" else "Free plan limit reached",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (isPremium) {
                        "You've used all 100 questions this month. Resets next month."
                    } else {
                        "You've used all 5 free questions this month. Upgrade to Premium for 100/month."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UsageFooter(
    usedCount: Int,
    limit: Int,
    isPremium: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$usedCount / $limit questions this month",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        if (!isPremium) {
            Spacer(Modifier.width(6.dp))
            Text(
                "·",
                color = GoldPrimary,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Free plan",
                style = MaterialTheme.typography.labelSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ChatHistoryDrawer(
    conversations: List<ChatHistoryManager.Conversation>,
    activeId: String?,
    isPremium: Boolean,
    onNewChat: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val limit = if (isPremium) 30 else 10
    val dateFmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Chat History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${conversations.size} / $limit saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // New chat button
        NavigationDrawerItem(
            label = {
                Text(
                    "New Chat",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            selected = false,
            onClick = onNewChat,
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary) },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = GoldPrimary.copy(alpha = 0.08f),
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(4.dp))

        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "No saved chats yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Your conversations will appear here",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(conversations, key = { it.id }) { convo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (convo.id == activeId) {
                                    GoldPrimary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onOpen(convo.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = if (convo.id == activeId) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                convo.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (convo.id == activeId) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                dateFmt.format(Date(convo.updatedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { onDelete(convo.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
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
            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            Box(
                modifier = Modifier
                    .widthIn(max = (screenWidthDp * maxWidthFraction).dp)
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
            ),
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
