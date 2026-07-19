package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

/**
 * Persists chat conversations locally and syncs them to Supabase
 * (`chat_conversations` table) so a user's full history survives
 * reinstall and re-login.
 *
 * History limits:
 *  - Free plan:  10 conversations
 *  - Premium:    30 conversations
 *
 * Free users who reach the limit are blocked from creating new chats
 * (see [canStartNewConversation] / [tryStartNewConversation]) so the
 * UI can surface an upgrade prompt instead of silently trimming.
 */
object ChatHistoryManager {

    private const val PREFS_NAME = "cookscope_chat_history"
    private const val KEY_CONVERSATIONS = "conversations_json"
    private const val KEY_ACTIVE_ID = "active_conversation_id"

    private lateinit var prefs: SharedPreferences
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    /** DTO matching the `chat_conversations` Supabase table. */
    @Serializable
    data class ChatConversationDto(
        val id: String? = null,
        @SerialName("user_id") val userId: String = "",
        @SerialName("ext_id") val extId: String = "",
        val title: String = "New Chat",
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
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
        // Best-effort cloud sync — fire-and-forget.
        pushRemoteSync()
    }

    /** Current plan-based conversation limit. */
    private fun currentLimit(): Int =
        if (SubscriptionManager.isPremium.value) 30 else 10

    /**
     * True if the user can start a new conversation without exceeding
     * their plan limit. Use this to decide whether to show the upgrade
     * prompt instead of silently blocking.
     */
    fun canStartNewConversation(): Boolean =
        _conversations.value.size < currentLimit()

    /**
     * Try to start a new conversation. Returns the new conversation id,
     * or `null` if the plan limit blocks creation (free users at 10).
     *
     * Does NOT trim existing conversations — the caller decides how to
     * surface the block (typically an upgrade dialog).
     */
    fun tryStartNewConversation(): String? {
        if (!canStartNewConversation()) return null
        return startNewConversation()
    }

    /** Returns the active conversation, or null if none exists yet. */
    fun activeConversation(): Conversation? =
        _activeId.value?.let { id -> _conversations.value.firstOrNull { it.id == id } }

    /**
     * Create a new empty conversation, set it active, and return its id.
     *
     * This is the force-create path: it does NOT enforce the plan limit.
     * Use it only when a conversation is required as a landing target
     * (e.g. [appendMessage] when there is no active conversation).
     * UI-initiated "New Chat" actions should go through
     * [tryStartNewConversation] instead.
     */
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
        // Enforce the plan-based history limit so free users can never
        // exceed 10 conversations even via the force-create path.
        trimToLimit(currentLimit())
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
        // Also remove from remote so it doesn't reappear on next login.
        scope.launch { deleteRemoteConversation(id) }
    }

    /**
     * Trim conversation list to the given limit, dropping oldest by updatedAt.
     * Called after premium status changes.
     */
    fun trimToLimit(limit: Int) {
        if (_conversations.value.size <= limit) return
        val sorted = _conversations.value.sortedByDescending { it.updatedAt }
        val kept = sorted.take(limit)
        val dropped = sorted.drop(limit).map { it.id }
        _conversations.value = kept
        if (_activeId.value != null && kept.none { it.id == _activeId.value }) {
            _activeId.value = kept.firstOrNull()?.id
        }
        persist()
        // Clean up remote rows that were trimmed locally.
        if (dropped.isNotEmpty()) {
            scope.launch {
                dropped.forEach { deleteRemoteConversation(it) }
            }
        }
    }

    /** Clear all conversations locally (e.g., on sign-out). Remote rows stay. */
    fun clearAll() {
        _conversations.value = emptyList()
        _activeId.value = null
        prefs.edit().clear().apply()
    }

    // ---- Supabase sync ----

    private fun currentUid(): String? =
        (SupabaseClient.client.auth.sessionStatus.value as?
            io.github.jan.supabase.auth.status.SessionStatus.Authenticated)
            ?.session?.user?.id

    /**
     * Push the current local conversation list to Supabase. Upserts each
     * conversation by (user_id, ext_id) and best-effort deletes remote
     * rows whose ext_id is no longer present locally.
     *
     * Fire-and-forget — failures are silently ignored to keep the chat
     * experience resilient when offline.
     */
    private fun pushRemoteSync() {
        val uid = currentUid() ?: return
        val local = _conversations.value
        scope.launch {
            try {
                local.forEach { convo ->
                    val dto = ChatConversationDto(
                        userId = uid,
                        extId = convo.id,
                        title = convo.title,
                        messages = convo.messages,
                    )
                    SupabaseClient.client.from("chat_conversations").upsert(dto)
                }
            } catch (_: Exception) {
                // Best-effort.
            }
        }
    }

    private suspend fun deleteRemoteConversation(extId: String) {
        val uid = currentUid() ?: return
        try {
            SupabaseClient.client.from("chat_conversations").delete {
                filter {
                    eq("user_id", uid)
                    eq("ext_id", extId)
                }
            }
        } catch (_: Exception) {
            // Best-effort.
        }
    }

    /**
     * Load all conversations for the current user from Supabase and merge
     * them into local state. Remote wins on conflict by ext_id; local-only
     * conversations are kept. Called by [UserManager] after auth.
     */
    suspend fun loadRemoteConversations(uid: String) {
        try {
            val dtos = SupabaseClient.client.from("chat_conversations").select {
                filter { eq("user_id", uid) }
                order("updated_at", Order.DESCENDING)
            }.decodeList<ChatConversationDto>()

            if (dtos.isEmpty()) return

            val remote = dtos.map { dto ->
                Conversation(
                    id = dto.extId.ifBlank { dto.id ?: "" },
                    title = dto.title.ifBlank { "New Chat" },
                    createdAt = dto.createdAt?.let { parseIsoToMillis(it) } ?: System.currentTimeMillis(),
                    updatedAt = dto.updatedAt?.let { parseIsoToMillis(it) } ?: System.currentTimeMillis(),
                    messages = dto.messages,
                )
            }.filter { it.id.isNotBlank() }

            if (remote.isEmpty()) return

            // Merge: remote wins on id conflict; keep local-only entries.
            val byId = linkedMapOf<String, Conversation>()
            // Insert local first (oldest first), then overwrite/add remote.
            _conversations.value.forEach { byId[it.id] = it }
            remote.sortedBy { it.updatedAt }.forEach { byId[it.id] = it }

            // Re-sort newest-first and enforce the plan limit.
            val merged = byId.values.sortedByDescending { it.updatedAt }
            val limit = currentLimit()
            _conversations.value = merged.take(limit)
            if (_activeId.value == null || _conversations.value.none { it.id == _activeId.value }) {
                _activeId.value = _conversations.value.firstOrNull()?.id
            }
            // Persist locally without re-triggering remote push.
            val raw = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(Conversation.serializer()),
                _conversations.value,
            )
            prefs.edit()
                .putString(KEY_CONVERSATIONS, raw)
                .putString(KEY_ACTIVE_ID, _activeId.value)
                .apply()
        } catch (_: Exception) {
            // Best-effort — keep local state on failure.
        }
    }

    private fun parseIsoToMillis(iso: String): Long? = try {
        java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            java.time.LocalDateTime.parse(iso)
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
