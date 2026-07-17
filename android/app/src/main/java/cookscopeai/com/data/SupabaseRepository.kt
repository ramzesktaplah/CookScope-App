package cookscopeai.com.data

import cookscopeai.com.data.model.Recipe
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for syncing CookScope data with Supabase.
 *
 * Currently operates in a graceful-degradation mode: if Supabase auth
 * is not set up (no authenticated session), operations silently fall back
 * to local-only storage. Once auth is integrated, these methods will
 * sync profile, saved recipes, preferences, and chat history to the cloud.
 *
 * All operations use the anon key with RLS policies enforced server-side.
 * Authenticated users get full CRUD on their own rows.
 */
class SupabaseRepository {

    private val client = SupabaseClient.client

    // ---- Saved Recipes ----

    suspend fun saveRecipe(recipe: Recipe): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("saved_recipes").insert(
                SupabaseClient.SavedRecipeDto(
                    userId = "",
                    recipeId = recipe.id,
                    recipeTitle = recipe.title,
                    recipeImageUrl = recipe.imageUrl,
                    recipeCuisine = recipe.cuisine,
                    recipeDifficulty = recipe.difficulty.name,
                    recipeCookTimeMin = recipe.cookTimeMin,
                    source = if (recipe.id.startsWith("api_")) "api" else "seed",
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsaveRecipe(recipeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("saved_recipes").delete {
                filter { eq("recipe_id", recipeId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedRecipes(): Result<List<SupabaseClient.SavedRecipeDto>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from("saved_recipes").select {
                order("created_at", Order.DESCENDING)
            }
            val recipes = result.decodeList<SupabaseClient.SavedRecipeDto>()
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Chat Messages ----

    suspend fun saveChatMessage(role: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("chat_messages").insert(
                SupabaseClient.ChatMessageDto(
                    userId = "",
                    role = role,
                    content = content,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatHistory(limit: Int = 50): Result<List<SupabaseClient.ChatMessageDto>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from("chat_messages").select {
                order("created_at", Order.ASCENDING)
                range(0L, (limit - 1).toLong())
            }
            val messages = result.decodeList<SupabaseClient.ChatMessageDto>()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("chat_messages").delete {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- User Preferences ----

    suspend fun savePreferences(
        dietaryTags: List<String>,
        skillLevel: String,
        favoriteCuisines: List<String>,
        allergies: List<String>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("user_preferences").upsert(
                SupabaseClient.UserPreferenceDto(
                    userId = "",
                    dietaryTags = dietaryTags,
                    skillLevel = skillLevel,
                    favoriteCuisines = favoriteCuisines,
                    allergies = allergies,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPreferences(): Result<SupabaseClient.UserPreferenceDto?> = withContext(Dispatchers.IO) {
        try {
            val result = client.from("user_preferences").select { limit(1) }
            val prefs = result.decodeList<SupabaseClient.UserPreferenceDto>().firstOrNull()
            Result.success(prefs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Profile ----

    suspend fun upsertProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        role: String,
        starsCount: Int,
        starsGiven: Int,
        planType: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("profiles").upsert(
                SupabaseClient.ProfileDto(
                    id = null,
                    username = username,
                    displayName = displayName,
                    bio = bio,
                    avatarUrl = avatarUrl,
                    role = role,
                    starsCount = starsCount,
                    starsGiven = starsGiven,
                    planType = planType,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<SupabaseClient.ProfileDto?> = withContext(Dispatchers.IO) {
        try {
            val result = client.from("profiles").select {
                filter { eq("id", userId) }
                limit(1)
            }
            val profile = result.decodeList<SupabaseClient.ProfileDto>().firstOrNull()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
