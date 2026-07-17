package cookscopeai.com.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val cuisine: String,
    val difficulty: Difficulty,
    val cookTimeMin: Int,
    val servings: Int,
    val caloriesPerServing: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val imageUrl: String,
    val tags: List<String>,
    val ingredients: List<Ingredient>,
    val steps: List<CookStep>,
    val tips: List<String>,
    val rating: Float,
    val reviewCount: Int,
    val authorName: String,
    val isPremium: Boolean = false,
)

@Serializable
enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Advanced"),
}

@Serializable
data class Ingredient(
    val name: String,
    val amount: String,
    val category: IngredientCategory = IngredientCategory.PANTRY,
)

@Serializable
enum class IngredientCategory(val label: String) {
    PRODUCE("Produce"),
    PROTEIN("Protein"),
    DAIRY("Dairy"),
    GRAIN("Grains"),
    PANTRY("Pantry"),
    SPICE("Spices"),
}

@Serializable
data class CookStep(
    val index: Int,
    val title: String,
    val instruction: String,
    val durationMin: Int = 0,
    val technique: String? = null,
    val tip: String? = null,
)

/**
 * A curated food/cooking video for the Explore feed.
 */
@Serializable
data class FoodVideo(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val chefName: String,
    val chefAvatar: String,
    val cuisine: String,
    val durationSec: Int,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val tags: List<String>,
    val recipeId: String? = null,
)

@Serializable
data class Cuisine(
    val name: String,
    val emoji: String,
    val tagline: String,
)

@Serializable
data class UserPreferences(
    val dietaryTags: List<String> = emptyList(),
    val skillLevel: String = "Intermediate",
    val favoriteCuisines: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
)
