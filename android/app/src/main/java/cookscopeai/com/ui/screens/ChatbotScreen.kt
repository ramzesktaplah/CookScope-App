package cookscopeai.com.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cookscopeai.com.R
import cookscopeai.com.data.ChatHistoryManager
import cookscopeai.com.data.ChatService
import cookscopeai.com.data.ChatUsageManager
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.ui.components.CookScopeTopLogo
import cookscopeai.com.ui.components.ShimmerDots
import cookscopeai.com.ui.components.SmoothInput
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.utilities.toBase64Jpeg
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChatBubble(
    val id: Long,
    val role: String,
    val content: String,
    val isTyping: Boolean = false,
    val hasImage: Boolean = false,
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
fun ChatbotScreen(
    onUpgrade: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val context = LocalContext.current

    val conversations by ChatHistoryManager.conversations.collectAsState()
    val activeId by ChatHistoryManager.activeId.collectAsState()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val usedCount by ChatUsageManager.used.collectAsState()
    val remainingCount by ChatUsageManager.remaining.collectAsState()
    val isRateLimited by ChatUsageManager.isRateLimited.collectAsState()
    val nextResetMillis by ChatUsageManager.nextResetMillis.collectAsState()
    val canStartNewChat = ChatHistoryManager.canStartNewConversation()

    val messages = remember { mutableStateListOf<ChatBubble>() }
    var isLoading by remember { mutableStateOf(false) }
    var showChatLimitDialog by remember { mutableStateOf(false) }
    var showRateLimitedDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoDeclined by remember { mutableStateOf(false) }
    val chatService = remember { ChatService() }
    val listState = rememberLazyListState()

    var currentSuggestions by remember { mutableStateOf(allSuggestionPool.shuffled().take(4)) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pendingImageUri = uri
            photoDeclined = false
        }
    }

    fun refreshRateLimit() {
        ChatUsageManager.refreshRemaining()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentSuggestions = allSuggestionPool.shuffled().take(4)
        refreshRateLimit()
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
        refreshRateLimit()
    }

    fun sendMessage(text: String, imageUri: Uri? = null) {
        if (text.isBlank() && imageUri == null) return
        if (isLoading) return

        // Rate limit check — surface a dialog instead of silently dropping.
        refreshRateLimit()
        if (!ChatUsageManager.canSendMessage()) {
            showRateLimitedDialog = true
            return
        }
        photoDeclined = false

        val imageBase64 = imageUri?.toBase64Jpeg(context)
        val displayContent = if (imageUri != null) {
            if (text.isBlank()) "[Photo attached]" else "$text\n[Photo attached]"
        } else {
            text
        }

        val userMsg = ChatBubble(
            id = System.currentTimeMillis(),
            role = "user",
            content = displayContent,
            hasImage = imageUri != null,
        )
        messages.add(userMsg)
        pendingImageUri = null
        isLoading = true

        // Persist the user message (text-only, image refs are descriptive)
        ChatHistoryManager.appendMessage("user", displayContent)

        val typingId = System.currentTimeMillis() + 1
        messages.add(ChatBubble(id = typingId, role = "assistant", content = "", isTyping = true))

        scope.launch {
            val history = messages
                .filter { !it.isTyping && it.content.isNotBlank() && it.id != 0L }
                .map { ChatService.ChatMessage(it.role, it.content) }
                .toMutableList()

            // The last user message may include the image.
            if (history.isNotEmpty() && history.last().role == "user") {
                history[history.lastIndex] = ChatService.ChatMessage(
                    role = "user",
                    content = text,
                    imageBase64 = imageBase64,
                )
            }

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

            when (result) {
                is ChatService.SendResult.Success -> {
                    ChatUsageManager.increment()
                }
                is ChatService.SendResult.DeclinedNonFood -> {
                    photoDeclined = true
                    val index = messages.indexOfFirst { it.id == typingId }
                    if (index >= 0) {
                        messages.removeAt(index)
                    }
                    // Remove the user message too since it was declined.
                    messages.remove(userMsg)
                    ChatHistoryManager.deleteLastUserMessage()
                }
                is ChatService.SendResult.Error -> {
                    val index = messages.indexOfFirst { it.id == typingId }
                    if (index >= 0) {
                        messages[index] = ChatBubble(
                            id = typingId,
                            role = "assistant",
                            content = result.message,
                        )
                    }
                }
            }

            refreshRateLimit()
            isLoading = false
        }
    }

    fun startNewChat() {
        if (!ChatHistoryManager.canStartNewConversation()) {
            showChatLimitDialog = true
            scope.launch { drawerState.close() }
            return
        }
        messages.clear()
        ChatHistoryManager.startNewConversation()
        pendingImageUri = null
        photoDeclined = false
        scope.launch { drawerState.close() }
    }

    fun openConversation(id: String) {
        ChatHistoryManager.setActive(id)
        pendingImageUri = null
        photoDeclined = false
        scope.launch { drawerState.close() }
    }

    fun deleteConversation(id: String) {
        if (id == activeId) {
            messages.clear()
        }
        ChatHistoryManager.deleteConversation(id)
    }

    val tierLabel = if (isPremium) "Premium · CS Donna AI" else "Free · CS Myth AI"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                conversations = conversations,
                activeId = activeId,
                isPremium = isPremium,
                canStartNewChat = canStartNewChat,
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
                // Upper-center brand logo
                CookScopeTopLogo()

                // Two-line top-left bar: history + new chat
                ChatTopBar(
                    onOpenHistory = { scope.launch { drawerState.open() } },
                    onNewChat = ::startNewChat,
                )

                // Rate-limit banner above the input area
                if (isRateLimited) {
                    RateLimitBanner(
                        timeRemaining = ChatUsageManager.formatTimeRemaining(),
                        onUpgrade = onUpgrade,
                    )
                }

                if (showChatLimitDialog) {
                    ChatLimitDialog(
                        isPremium = isPremium,
                        onUpgrade = {
                            showChatLimitDialog = false
                            onUpgrade()
                        },
                        onDismiss = { showChatLimitDialog = false },
                    )
                }

                if (showRateLimitedDialog) {
                    RateLimitReachedDialog(
                        timeRemaining = ChatUsageManager.formatTimeRemaining(),
                        onUpgrade = {
                            showRateLimitedDialog = false
                            onUpgrade()
                        },
                        onDismiss = { showRateLimitedDialog = false },
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
                }

                // Suggestions + photo preview + input footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (messages.size <= 1 && !isLoading) {
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

                    if (photoDeclined) {
                        Text(
                            "Please share a photo of a dish, ingredient, recipe, or anything cooking-related.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }

                    if (pendingImageUri != null) {
                        PhotoPreviewChip(
                            uri = pendingImageUri,
                            onRemove = { pendingImageUri = null },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    SmoothInput(
                        onSend = { sendMessage(it, pendingImageUri) },
                        placeholder = "Ask about recipes, techniques...",
                        isLoading = isLoading,
                        onAttach = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        tierLabel = tierLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onOpenHistory,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Chat history",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onNewChat,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "New chat",
                tint = GoldPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PhotoPreviewChip(
    uri: Uri?,
    onRemove: () -> Unit,
) {
    if (uri == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Restaurant,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Photo ready to send",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RateLimitBanner(
    timeRemaining: String,
    onUpgrade: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Question limit reached",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "You can get assistant in $timeRemaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onUpgrade) {
            Text(
                "Upgrade",
                color = GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun RateLimitReachedDialog(
    timeRemaining: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Question limit reached",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                "You can get assistant in $timeRemaining. Upgrade to Premium for more questions anytime.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onUpgrade) {
                Text("Upgrade", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
}

@Composable
private fun ChatLimitDialog(
    isPremium: Boolean,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Chat history full",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                if (isPremium) {
                    "You've reached the 30-chat limit on Premium. Delete an old chat to start a new one."
                } else {
                    "You've reached the 10-chat limit on the Free plan. Upgrade to Premium for 30 chats."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            if (!isPremium) {
                TextButton(onClick = onUpgrade) {
                    Text("Upgrade", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ChatHistoryDrawer(
    conversations: List<ChatHistoryManager.Conversation>,
    activeId: String?,
    isPremium: Boolean,
    canStartNewChat: Boolean,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.ChatBubble,
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

        NavigationDrawerItem(
            label = {
                Text(
                    "New Chat",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canStartNewChat) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            },
            selected = false,
            onClick = onNewChat,
            icon = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (canStartNewChat) GoldPrimary else GoldPrimary.copy(alpha = 0.4f),
                )
            },
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
            Image(
                painter = painterResource(R.drawable.ic_cookscope_mascot),
                contentDescription = "CookScope assistant",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(8.dp))
        }

        if (bubble.isTyping) {
            val lineCount = remember(bubble.id) { (2..3).random() }
            Box(
                modifier = Modifier
                    .widthIn(max = (LocalConfiguration.current.screenWidthDp * maxWidthFraction).dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp,
                        )
                    )
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column {
                    repeat(lineCount) { i ->
                        val fraction = if (i == lineCount - 1) 0.6f else 0.95f
                        ShimmerLine(
                            fraction = fraction,
                            modifier = Modifier.padding(top = if (i == 0) 0.dp else 6.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ShimmerDots()
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "CookScope is thinking…",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                        )
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
                Column {
                    Text(
                        text = bubble.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface,
                    )
                    if (bubble.hasImage) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Photo attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) Color.Black.copy(alpha = 0.7f) else GoldPrimary,
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun ShimmerLine(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmerLine")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerLineTranslate",
    )
    val base = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(translate - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translate, 0f),
    )
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(11.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(brush),
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
