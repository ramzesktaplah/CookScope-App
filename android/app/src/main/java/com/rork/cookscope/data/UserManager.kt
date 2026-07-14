package com.rork.cookscope.data

import android.content.Context
import android.content.SharedPreferences
import com.rork.cookscope.data.model.ChefPost
import com.rork.cookscope.data.model.UserProfile
import com.rork.cookscope.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Manages local user profile state with persistence.
 * Handles profile editing, role switching, posts, and star giving.
 */
object UserManager {

    private const val PREFS_NAME = "cookscope_user"
    private const val KEY_PROFILE = "user_profile_json"
    private const val KEY_SAVED_RECIPE_IDS = "saved_recipe_ids"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

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

    private val _savedRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val savedRecipeIds: StateFlow<Set<String>> = _savedRecipeIds.asStateFlow()

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
    }

    private fun saveProfile() {
        prefs.edit()
            .putString(KEY_PROFILE, json.encodeToString(UserProfile.serializer(), _profile.value))
            .apply()
    }

    fun updateProfile(
        username: String? = null,
        displayName: String? = null,
        bio: String? = null,
        profilePhotoUri: String? = null,
        role: UserRole? = null,
    ) {
        _profile.value = _profile.value.copy(
            username = username ?: _profile.value.username,
            displayName = displayName ?: _profile.value.displayName,
            bio = bio ?: _profile.value.bio,
            profilePhotoUri = profilePhotoUri ?: _profile.value.profilePhotoUri,
            role = role ?: _profile.value.role,
        )
        saveProfile()
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

    /**
     * Give a star to a chef. Increments starsGiven for the current user.
     */
    fun giveStar() {
        _profile.value = _profile.value.copy(
            starsGiven = _profile.value.starsGiven + 1
        )
        saveProfile()
    }

    /**
     * Toggle saved recipe.
     */
    fun toggleSavedRecipe(recipeId: String) {
        val current = _savedRecipeIds.value.toMutableSet()
        if (recipeId in current) {
            current.remove(recipeId)
        } else {
            current.add(recipeId)
        }
        _savedRecipeIds.value = current
        prefs.edit().putStringSet(KEY_SAVED_RECIPE_IDS, current).apply()
    }

    fun isRecipeSaved(recipeId: String): Boolean = recipeId in _savedRecipeIds.value
}
