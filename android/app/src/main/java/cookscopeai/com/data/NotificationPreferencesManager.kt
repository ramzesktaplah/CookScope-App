package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import cookscopeai.com.data.SupabaseClient.NotificationPrefsDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Per-category notification opt-in/opt-out flags.
 *
 * Stored locally (SharedPreferences) for instant reads and mirrored to the
 * `user_notification_preferences` Supabase table so preferences survive
 * reinstall and sync across devices. Defaults are all true (full opt-in)
 * matching the migration defaults.
 *
 * [NotificationService] checks the relevant flag before posting any local
 * notification, and the notify-user edge function checks the remote row
 * before sending an FCM push.
 */
object NotificationPreferencesManager {

    private const val PREFS_NAME = "cookscope_notif_prefs"
    private const val KEY_PREFS_JSON = "prefs_json"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable
    data class NotificationPrefs(
        val popularRecipe: Boolean = true,
        val mealtime: Boolean = true,
        val scanAi: Boolean = true,
        val savedRecipes: Boolean = true,
        val engagement: Boolean = true,
        val achievements: Boolean = true,
        val trending: Boolean = true,
        val premium: Boolean = true,
        val rateLimitReset: Boolean = true,
    )

    private val _preferences = MutableStateFlow(NotificationPrefs())
    val preferences: StateFlow<NotificationPrefs> = _preferences.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PREFS_JSON, null)
        if (stored != null) {
            try {
                _preferences.value = json.decodeFromString<NotificationPrefs>(stored)
            } catch (_: Exception) { }
        }
        // Best-effort remote sync after auth.
        scope.launch { loadRemote() }
    }

    /** Update a single category flag locally and push to Supabase. */
    fun setCategory(category: NotificationCategory, enabled: Boolean) {
        val current = _preferences.value
        _preferences.value = when (category) {
            NotificationCategory.POPULAR_RECIPE -> current.copy(popularRecipe = enabled)
            NotificationCategory.MEALTIME -> current.copy(mealtime = enabled)
            NotificationCategory.SCAN_AI -> current.copy(scanAi = enabled)
            NotificationCategory.SAVED_RECIPES -> current.copy(savedRecipes = enabled)
            NotificationCategory.ENGAGEMENT -> current.copy(engagement = enabled)
            NotificationCategory.ACHIEVEMENTS -> current.copy(achievements = enabled)
            NotificationCategory.TRENDING -> current.copy(trending = enabled)
            NotificationCategory.PREMIUM -> current.copy(premium = enabled)
            NotificationCategory.RATE_LIMIT_RESET -> current.copy(rateLimitReset = enabled)
        }
        persistLocal()
        scope.launch { pushRemote() }
    }

    /** True if the given category is currently enabled. */
    fun isEnabled(category: NotificationCategory): Boolean {
        return when (category) {
            NotificationCategory.POPULAR_RECIPE -> _preferences.value.popularRecipe
            NotificationCategory.MEALTIME -> _preferences.value.mealtime
            NotificationCategory.SCAN_AI -> _preferences.value.scanAi
            NotificationCategory.SAVED_RECIPES -> _preferences.value.savedRecipes
            NotificationCategory.ENGAGEMENT -> _preferences.value.engagement
            NotificationCategory.ACHIEVEMENTS -> _preferences.value.achievements
            NotificationCategory.TRENDING -> _preferences.value.trending
            NotificationCategory.PREMIUM -> _preferences.value.premium
            NotificationCategory.RATE_LIMIT_RESET -> _preferences.value.rateLimitReset
        }
    }

    private fun persistLocal() {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_PREFS_JSON, json.encodeToString(NotificationPrefs.serializer(), _preferences.value))
            .apply()
    }

    private suspend fun loadRemote() {
        val uid = UserManager.profile.value.id
        if (uid == "local_user" || uid.isBlank()) return
        try {
            val dto = SupabaseClient.client.postgrest["user_notification_preferences"]
                .select {
                    filter { eq("user_id", uid) }
                    limit(1)
                }
                .decodeSingleOrNull<NotificationPrefsDto>()
            if (dto != null) {
                _preferences.value = NotificationPrefs(
                    popularRecipe = dto.popularRecipe,
                    mealtime = dto.mealtime,
                    scanAi = dto.scanAi,
                    savedRecipes = dto.savedRecipes,
                    engagement = dto.engagement,
                    achievements = dto.achievements,
                    trending = dto.trending,
                    premium = dto.premium,
                    rateLimitReset = dto.rateLimitReset,
                )
                persistLocal()
            }
        } catch (_: Exception) {
            // Keep local defaults on failure.
        }
    }

    private suspend fun pushRemote() {
        val uid = UserManager.profile.value.id
        if (uid == "local_user" || uid.isBlank()) return
        try {
            val p = _preferences.value
            val dto = NotificationPrefsDto(
                userId = uid,
                popularRecipe = p.popularRecipe,
                mealtime = p.mealtime,
                scanAi = p.scanAi,
                savedRecipes = p.savedRecipes,
                engagement = p.engagement,
                achievements = p.achievements,
                trending = p.trending,
                premium = p.premium,
                rateLimitReset = p.rateLimitReset,
            )
            SupabaseClient.client.postgrest["user_notification_preferences"].upsert(dto)
        } catch (_: Exception) {
            // Best-effort; local state stays correct.
        }
    }
}

/** Notification categories matching the channel + preferences schema. */
enum class NotificationCategory {
    POPULAR_RECIPE,
    MEALTIME,
    SCAN_AI,
    SAVED_RECIPES,
    ENGAGEMENT,
    ACHIEVEMENTS,
    TRENDING,
    PREMIUM,
    RATE_LIMIT_RESET,
}
