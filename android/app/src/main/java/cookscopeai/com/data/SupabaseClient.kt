package cookscopeai.com.data

import cookscopeai.com.Config
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase client wrapper for CookScope.
 * Provides typed access to the managed Postgres database and native Supabase Auth.
 *
 * Tables:
 * - profiles: user profile data (username, bio, role, role_locked, stars, plan)
 * - saved_recipes: bookmarked recipes per user
 * - user_preferences: dietary tags, skill level, cuisines, allergies
 * - chat_messages: chat history per user
 *
 * Uses native Supabase Auth mode (auth.uid() RLS).
 *
 * Deep link scheme/host: cookscope://login-callback
 * This URL must be added to Supabase Dashboard → Authentication → URL Configuration
 * → Additional Redirect URLs.
 */
object SupabaseClient {

    const val DEEPLINK_SCHEME = "cookscope"
    const val DEEPLINK_HOST = "login-callback"

    val client = createSupabaseClient(
        supabaseUrl = Config.EXPO_PUBLIC_SUPABASE_URL,
        supabaseKey = Config.EXPO_PUBLIC_SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            this.flowType = FlowType.PKCE
            scheme = DEEPLINK_SCHEME
            host = DEEPLINK_HOST
        }
        install(Postgrest)
    }

    // ---- DTOs matching database schema ----

    @Serializable
    data class ProfileDto(
        val id: String? = null,
        val username: String = "",
        @SerialName("display_name") val displayName: String = "",
        val bio: String = "",
        @SerialName("avatar_url") val avatarUrl: String = "",
        val role: String = "learner",
        @SerialName("role_locked") val roleLocked: Boolean = false,
        @SerialName("stars_count") val starsCount: Int = 0,
        @SerialName("stars_given") val starsGiven: Int = 0,
        @SerialName("plan_type") val planType: String = "free",
        @SerialName("onboarding_complete") val onboardingComplete: Boolean = false,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
    )

    @Serializable
    data class SavedRecipeDto(
        val id: String? = null,
        @SerialName("user_id") val userId: String = "",
        @SerialName("recipe_id") val recipeId: String = "",
        @SerialName("recipe_title") val recipeTitle: String = "",
        @SerialName("recipe_image_url") val recipeImageUrl: String = "",
        @SerialName("recipe_cuisine") val recipeCuisine: String = "",
        @SerialName("recipe_difficulty") val recipeDifficulty: String = "",
        @SerialName("recipe_cook_time_min") val recipeCookTimeMin: Int = 0,
        val source: String = "api",
        @SerialName("created_at") val createdAt: String? = null,
    )

    @Serializable
    data class UserPreferenceDto(
        val id: String? = null,
        @SerialName("user_id") val userId: String = "",
        @SerialName("dietary_tags") val dietaryTags: List<String> = emptyList(),
        @SerialName("skill_level") val skillLevel: String = "beginner",
        @SerialName("favorite_cuisines") val favoriteCuisines: List<String> = emptyList(),
        val allergies: List<String> = emptyList(),
        @SerialName("updated_at") val updatedAt: String? = null,
    )

    @Serializable
    data class ChatMessageDto(
        val id: String? = null,
        @SerialName("user_id") val userId: String = "",
        val role: String = "",
        val content: String = "",
        @SerialName("created_at") val createdAt: String? = null,
    )
}
