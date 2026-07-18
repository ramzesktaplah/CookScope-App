package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import cookscopeai.com.data.SupabaseClient.ProfileDto
import cookscopeai.com.data.SupabaseClient.UserPreferenceDto
import cookscopeai.com.data.model.ChefPost
import cookscopeai.com.data.model.UserProfile
import cookscopeai.com.data.model.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Manages user profile, role lock, onboarding state, and preferences.
 *
 * Integrates with Supabase Auth: when a session becomes authenticated,
 * the profile + preferences are loaded from the `profiles` and
 * `user_preferences` tables. On sign-out, local state is reset.
 *
 * Role lock: once [setRoleLocked] is called (after onboarding role
 * selection), the role cannot be changed via [updateProfile].
 *
 * Onboarding state is persisted both locally (SharedPreferences) and
 * remotely (profiles.onboarding_complete) so returning users skip
 * onboarding.
 */
object UserManager {

    private const val PREFS_NAME = "cookscope_user"
    private const val KEY_PROFILE = "user_profile_json"
    private const val KEY_SAVED_RECIPE_IDS = "saved_recipe_ids"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val KEY_PREFERENCES = "user_preferences_json"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profile = MutableStateFlow(
        UserProfile(
            username = "homechef",
            displayName = "Home Chef",
            bio = "Cooking with passion, one dish at a time",
            role = UserRole.LEARNER,
            starsReceived = 0,
            starsGiven = 0,
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    /** Whether the user's role can still be changed. Locked after onboarding. */
    private val _isRoleLocked = MutableStateFlow(false)
    val isRoleLocked: StateFlow<Boolean> = _isRoleLocked.asStateFlow()

    /** Whether onboarding (welcome → … → paywall) has been completed. */
    private val _isOnboardingComplete = MutableStateFlow(false)
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    /** Whether a remote profile row exists for the current user. */
    private val _hasRemoteProfile = MutableStateFlow(false)
    val hasRemoteProfile: StateFlow<Boolean> = _hasRemoteProfile.asStateFlow()

    private val _savedRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val savedRecipeIds: StateFlow<Set<String>> = _savedRecipeIds.asStateFlow()

    /** Cooking preferences used as defaults on the Create Recipe screen. */
    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    @kotlinx.serialization.Serializable
    data class UserPreferences(
        val dietaryTags: List<String> = emptyList(),
        val skillLevel: String = "Beginner",
        val favoriteCuisines: List<String> = emptyList(),
        val allergies: List<String> = emptyList(),
    )

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load profile
        val profileJson = prefs.getString(KEY_PROFILE, null)
        if (profileJson != null) {
            try {
                _profile.value = json.decodeFromString<UserProfile>(profileJson)
            } catch (_: Exception) { }
        }

        // Load saved recipes
        val saved = prefs.getStringSet(KEY_SAVED_RECIPE_IDS, emptySet()) ?: emptySet()
        _savedRecipeIds.value = saved

        // Load onboarding flag
        _isOnboardingComplete.value = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

        // Load preferences
        val prefJson = prefs.getString(KEY_PREFERENCES, null)
        if (prefJson != null) {
            try {
                _preferences.value = json.decodeFromString<UserPreferences>(prefJson)
            } catch (_: Exception) { }
        }

        // Wire auth session changes → load/reset profile.
        scope.launch {
            AuthManager.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val uid = status.session.user?.id
                        if (uid != null) {
                            _profile.value = _profile.value.copy(id = uid)
                            // Use the username from signup metadata if no remote profile exists yet.
                            val metadata = status.session.user?.userMetadata
                            val signupUsername = metadata?.get("username")?.jsonPrimitive?.content?.trim()
                            val signupDisplayName = metadata?.get("display_name")?.jsonPrimitive?.content?.trim()
                            if (!signupUsername.isNullOrBlank()) {
                                _profile.value = _profile.value.copy(
                                    username = signupUsername,
                                    displayName = signupDisplayName?.ifBlank { signupUsername } ?: signupUsername,
                                )
                                saveProfile()
                            }
                            loadRemoteProfile(uid)
                            loadRemotePreferences(uid)
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        resetToLocalDefaults()
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Load the user's profile row from Supabase. If none exists yet
     * (e.g., brand-new signup), keep local defaults and flag that no
     * remote profile exists so onboarding can create one.
     */
    private suspend fun loadRemoteProfile(uid: String) {
        try {
            val dto = SupabaseClient.client.postgrest["profiles"]
                .select(columns = Columns.ALL) {
                    filter { eq("id", uid) }
                    limit(1)
                }
                .decodeSingleOrNull<ProfileDto>()

            if (dto != null) {
                _hasRemoteProfile.value = true
                _isRoleLocked.value = dto.roleLocked
                _isOnboardingComplete.value = dto.onboardingComplete
                val role = parseRole(dto.role)
                _profile.value = _profile.value.copy(
                    id = uid,
                    username = dto.username.ifBlank { _profile.value.username },
                    displayName = dto.displayName.ifBlank { _profile.value.displayName },
                    bio = dto.bio.ifBlank { _profile.value.bio },
                    avatarUrl = dto.avatarUrl.ifBlank { _profile.value.avatarUrl },
                    role = role,
                    starsReceived = dto.starsCount,
                    starsGiven = dto.starsGiven,
                )
                // Persist locally so it survives offline relaunch.
                saveProfile()
                prefs.edit()
                    .putBoolean(KEY_ONBOARDING_COMPLETE, dto.onboardingComplete)
                    .apply()
            } else {
                _hasRemoteProfile.value = false
                // New user — onboarding will create the row.
            }
        } catch (e: Exception) {
            // Network/permission error — keep local defaults.
            _hasRemoteProfile.value = false
        }
    }

    private suspend fun loadRemotePreferences(uid: String) {
        try {
            val dto = SupabaseClient.client.postgrest["user_preferences"]
                .select(columns = Columns.ALL) {
                    filter { eq("user_id", uid) }
                    limit(1)
                }
                .decodeSingleOrNull<UserPreferenceDto>()

            if (dto != null) {
                _preferences.value = UserPreferences(
                    dietaryTags = dto.dietaryTags,
                    skillLevel = dto.skillLevel.ifBlank { "Beginner" },
                    favoriteCuisines = dto.favoriteCuisines,
                    allergies = dto.allergies,
                )
                prefs.edit()
                    .putString(KEY_PREFERENCES, json.encodeToString(UserPreferences.serializer(), _preferences.value))
                    .apply()
            }
        } catch (_: Exception) {
            // Keep local defaults on error.
        }
    }

    private fun resetToLocalDefaults() {
        _hasRemoteProfile.value = false
        _isRoleLocked.value = false
        _isOnboardingComplete.value = false
        _preferences.value = UserPreferences()
        _profile.value = UserProfile(
            username = "homechef",
            displayName = "Home Chef",
            bio = "Cooking with passion, one dish at a time",
            role = UserRole.LEARNER,
        )
        prefs.edit().clear().apply()
    }

    private fun saveProfile() {
        prefs.edit()
            .putString(KEY_PROFILE, json.encodeToString(UserProfile.serializer(), _profile.value))
            .apply()
    }

    /**
     * Update local profile. Role change is ignored if locked.
     * Does NOT write to Supabase — call [pushProfileToRemote] for that.
     */
    fun updateProfile(
        username: String? = null,
        displayName: String? = null,
        bio: String? = null,
        profilePhotoUri: String? = null,
        avatarUrl: String? = null,
        role: UserRole? = null,
    ) {
        val effectiveRole = if (role != null && !_isRoleLocked.value) role else _profile.value.role
        _profile.value = _profile.value.copy(
            username = username ?: _profile.value.username,
            displayName = displayName ?: _profile.value.displayName,
            bio = bio ?: _profile.value.bio,
            profilePhotoUri = profilePhotoUri ?: _profile.value.profilePhotoUri,
            avatarUrl = avatarUrl ?: _profile.value.avatarUrl,
            role = effectiveRole,
        )
        saveProfile()
    }

    /**
     * Persist the current local profile to the `profiles` table.
     * Creates a new row if none exists, otherwise upserts.
     */
    suspend fun pushProfileToRemote() {
        val uid = _profile.value.id
        if (uid == "local_user") return
        try {
            val dto = ProfileDto(
                id = uid,
                username = _profile.value.username,
                displayName = _profile.value.displayName,
                bio = _profile.value.bio,
                avatarUrl = _profile.value.avatarUrl ?: "",
                role = roleToString(_profile.value.role),
                roleLocked = _isRoleLocked.value,
                starsCount = _profile.value.starsReceived,
                starsGiven = _profile.value.starsGiven,
                onboardingComplete = _isOnboardingComplete.value,
            )
            SupabaseClient.client.postgrest["profiles"].upsert(dto)
            _hasRemoteProfile.value = true
        } catch (_: Exception) {
            // Best-effort; local state remains.
        }
    }

    /**
     * Persist the current preferences to the `user_preferences` table.
     */
    suspend fun pushPreferencesToRemote() {
        val uid = _profile.value.id
        if (uid == "local_user") return
        try {
            val dto = UserPreferenceDto(
                userId = uid,
                dietaryTags = _preferences.value.dietaryTags,
                skillLevel = _preferences.value.skillLevel,
                favoriteCuisines = _preferences.value.favoriteCuisines,
                allergies = _preferences.value.allergies,
            )
            SupabaseClient.client.postgrest["user_preferences"].upsert(dto)
        } catch (_: Exception) {
            // Best-effort; local state remains.
        }
    }

    /**
     * Lock the role after onboarding selection. Writes the locked flag
     * locally and to the remote profile row.
     */
    fun setRoleLocked() {
        _isRoleLocked.value = true
        scope.launch { pushProfileToRemote() }
    }

    /**
     * Mark onboarding complete (after paywall skip/subscribe).
     */
    fun setOnboardingComplete() {
        _isOnboardingComplete.value = true
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
        scope.launch { pushProfileToRemote() }
    }

    /**
     * Update preferences (local + remote). Used by the onboarding
     * preferences screen and the in-app preferences screen.
     */
    fun updatePreferences(prefs: UserPreferences) {
        _preferences.value = prefs
        this.prefs.edit()
            .putString(KEY_PREFERENCES, json.encodeToString(UserPreferences.serializer(), prefs))
            .apply()
        scope.launch { pushPreferencesToRemote() }
    }

    fun addPost(post: ChefPost) {
        _profile.value = _profile.value.copy(
            posts = listOf(post) + _profile.value.posts
        )
        saveProfile()
    }

    fun removePost(postId: String) {
        _profile.value = _profile.value.copy(
            posts = _profile.value.posts.filter { it.id != postId }
        )
        saveProfile()
    }

    fun giveStar() {
        _profile.value = _profile.value.copy(
            starsGiven = _profile.value.starsGiven + 1
        )
        saveProfile()
        scope.launch { pushProfileToRemote() }
    }

    fun toggleSavedRecipe(recipeId: String) {
        val current = _savedRecipeIds.value.toMutableSet()
        if (recipeId in current) current.remove(recipeId) else current.add(recipeId)
        _savedRecipeIds.value = current
        prefs.edit().putStringSet(KEY_SAVED_RECIPE_IDS, current).apply()
    }

    fun isRecipeSaved(recipeId: String): Boolean = recipeId in _savedRecipeIds.value

    // ---- Helpers ----

    private fun parseRole(s: String): UserRole = when (s.lowercase()) {
        "chef", "teacher", "chef_teacher", "chef/teacher" -> UserRole.CHEF_TEACHER
        else -> UserRole.LEARNER
    }

    private fun roleToString(r: UserRole): String = when (r) {
        UserRole.CHEF_TEACHER -> "chef_teacher"
        UserRole.LEARNER -> "learner"
    }
}
