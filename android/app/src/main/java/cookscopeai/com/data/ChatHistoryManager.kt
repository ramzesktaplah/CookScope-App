package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persists chat conversations locally so users can revisit past chats.
 *
 * History limits (enforced by [trimToLimit]):
 *  - Free plan:  10 conversations
 *  - Premium:    30 conversations
 *
 * When the limit is exceeded, the oldest conversations are dropped.
 */
object ChatHistoryManager {

    private const val PREFS_NAME = "cookscope_chat_history"
    private const val KEY_CONVERSATIONS = "conversations_json"
    private const val KEY_ACTIVE_ID = "active_conversation_id"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class ChatMessage(
        val role: String,
        val content: String,
        val timestamp: Long,
    )

    @Serializable
    data class Conversation(
        val id: String,
        val title: String,
        val createdAt: Long,
        val updatedAt: Long,
        val messages: List<ChatMessage> = emptyList(),
    )

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        val raw = prefs.getString(KEY_CONVERSATIONS, null) ?: return
        try {
            val list = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(Conversation.serializer()),
                raw,
            )
            _conversations.value = list
            val savedActive = prefs.getString(KEY_ACTIVE_ID, null)
            _activeId.value = savedActive?.takeIf { id -> list.any { it.id == id } }
        } catch (_: Exception) {
            _conversations.value = emptyList()
        }
    }

    private fun persist() {
        val raw = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Conversation.serializer()),
            _conversations.value,
        )
        prefs.edit()
            .putString(KEY_CONVERSATIONS, raw)
            .putString(KEY_ACTIVE_ID, _activeId.value)
            .apply()
    }

    /** Returns the active conversation, or null if none exists yet. */
    fun activeConversation(): Conversation? =
        _activeId.value?.let { id -> _conversations.value.firstOrNull { it.id == id } }

    /** Create a new empty conversation, set it active, and return its id. */
    fun startNewConversation(): String {
        val id = "chat_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val now = System.currentTimeMillis()
        val convo = Conversation(
            id = id,
            title = "New Chat",
            createdAt = now,
            updatedAt = now,
            messages = emptyList(),
        )
        _conversations.value = listOf(convo) + _conversations.value
        _activeId.value = id
        // Enforce the plan-based history limit immediately so free users
        // can never exceed 10 conversations, even if they rapidly start
        // new chats. Premium is capped at 30.
        val limit = if (SubscriptionManager.isPremium.value) 30 else 10
        trimToLimit(limit)
        return id
    }

    /** Switch to an existing conversation by id. */
    fun setActive(id: String) {
        if (_conversations.value.any { it.id == id }) {
            _activeId.value = id
            persist()
        }
    }

    /**
     * Append a message to the active conversation (creating one if needed).
     * Updates the conversation title from the first user message and refreshes updatedAt.
     */
    fun appendMessage(role: String, content: String) {
        val activeId = _activeId.value ?: startNewConversation()
        val now = System.currentTimeMillis()
        _conversations.value = _conversations.value.map { c ->
            if (c.id == activeId) {
                val newTitle = if (c.title == "New Chat" && role == "user") {
                    content.take(38).ifBlank { "New Chat" }
                } else {
                    c.title
                }
                c.copy(
                    title = newTitle,
                    updatedAt = now,
                    messages = c.messages + ChatMessage(role, content, now),
                )
            } else c
        }
        persist()
    }

    /** Delete a conversation by id. Clears activeId if it was active. */
    fun deleteConversation(id: String) {
        _conversations.value = _conversations.value.filter { it.id != id }
        if (_activeId.value == id) {
            _activeId.value = _conversations.value.firstOrNull()?.id
        }
        persist()
    }

    /**
     * Trim conversation list to the given limit, dropping oldest by updatedAt.
     * Called after premium status changes.
     */
    fun trimToLimit(limit: Int) {
        if (_conversations.value.size <= limit) return
        val sorted = _conversations.value.sortedByDescending { it.updatedAt }
        val kept = sorted.take(limit)
        _conversations.value = kept
        if (_activeId.value != null && kept.none { it.id == _activeId.value }) {
            _activeId.value = kept.firstOrNull()?.id
        }
        persist()
    }

    /** Clear all conversations (e.g., on sign-out). */
    fun clearAll() {
        _conversations.value = emptyList()
        _activeId.value = null
        prefs.edit().clear().apply()
    }
}
