package cookscopeai.com.data

import cookscopeai.com.data.model.CookStep
import cookscopeai.com.data.model.Difficulty
import cookscopeai.com.data.model.Ingredient
import cookscopeai.com.data.model.IngredientCategory
import cookscopeai.com.data.model.Recipe
import kotlinx.serialization.json.Json

/**
 * Service for fetching recipes via the CookScope backend.
 *
 * All requests are proxied through Supabase Edge Functions
 * (`recipe-search`, `recipe-detail`, `recipe-random`) so the recipeapi.io
 * API key never ships in the APK. The backend validates the user's Supabase
 * session and forwards the call to recipeapi.io with a server-side secret.
 */
class RecipeApiService {

    private val backend = CookScopeBackendService()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Search recipes by text query (recipe name, cuisine, tag, etc).
     */
    suspend fun searchRecipes(
        query: String,
        page: Int = 1,
        perPage: Int = 20,
    ): Result<List<Recipe>> {
        return try {
            val result = backend.recipeSearch(query, page, perPage)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Search failed"))
            }
            val listResponse = jsonParser.decodeFromString<RecipeListResponse>(result.getOrThrow())
            val recipes = listResponse.data.map { mapApiRecipeToRecipe(it) }
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find recipes that match the given ingredients.
     */
    suspend fun getRecipesByIngredients(
        ingredients: List<String>,
        page: Int = 1,
        perPage: Int = 20,
    ): Result<List<Recipe>> {
        return try {
            val result = backend.recipeSearchByIngredients(ingredients, page, perPage)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Recipe search failed"))
            }
            val listResponse = jsonParser.decodeFromString<RecipeListResponse>(result.getOrThrow())
            val recipes = listResponse.data.map { mapApiRecipeToRecipe(it) }
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single recipe by its ID, with full details (ingredients, instructions, nutrition).
     */
    suspend fun getRecipe(id: String): Result<Recipe> {
        return try {
            val result = backend.recipeDetail(id)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Recipe fetch failed"))
            }
            val detailResponse = jsonParser.decodeFromString<RecipeDetailResponse>(result.getOrThrow())
            val detail = detailResponse.data
                ?: return Result.failure(Exception("Recipe not found"))
            Result.success(mapApiRecipeToRecipe(detail))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch today's trending recipes from the server-side cache.
     * The `trending-fetch` edge function queries the trending_recipes
     * table (populated daily by the pg_cron job) and returns rows with
     * the full recipeapi.io payload in `recipe_data`.
     */
    suspend fun getTrendingRecipes(): Result<List<Recipe>> {
        return try {
            val result = backend.trendingFetch()
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Trending fetch failed"))
            }
            val response = jsonParser.decodeFromString<TrendingFetchResponse>(result.getOrThrow())
            val recipes = response.data.map { row ->
                mapApiRecipeToRecipe(row.recipeData)
            }
            if (recipes.isEmpty()) {
                Result.failure(Exception("No trending recipes available"))
            } else {
                Result.success(recipes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get random recipes for discovery on the Home screen.
     * The backend fans out to recipeapi.io's /recipes/random endpoint.
     */
    suspend fun getRandomRecipes(count: Int = 10): Result<List<Recipe>> {
        return try {
            val fetchCount = minOf(count, 10)
            val result = backend.recipeRandom(fetchCount)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("No recipes returned"))
            }
            val body = result.getOrThrow()
            // The backend returns `{ data: [ApiRecipe, ...] }`.
            val parsed = runCatching {
                jsonParser.decodeFromString<RecipeListResponse>(body)
            }.getOrNull()
                ?: runCatching {
                    jsonParser.decodeFromString<RecipeDetailResponse>(body)
                }.getOrNull()?.let { RecipeListResponse(data = listOfNotNull(it.data)) }
            val recipes = (parsed?.data ?: emptyList()).map { mapApiRecipeToRecipe(it) }
            if (recipes.isEmpty()) {
                Result.failure(Exception("No recipes returned"))
            } else {
                Result.success(recipes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch available ingredients (for autocomplete suggestions).
     *
     * The backend does not currently expose an ingredients listing endpoint —
     * returning an empty list keeps callers working without embedding the
     * recipeapi.io key client-side.
     */
    suspend fun getIngredients(page: Int = 1, perPage: Int = 50): Result<List<ApiIngredientInfo>> {
        return Result.success(emptyList())
    }

    /**
     * Map an ApiRecipe from the backend payload to the app's Recipe model.
     * Both list and detail endpoints return the same full structure.
     */
    private fun mapApiRecipeToRecipe(api: ApiRecipe): Recipe {
        val id = api.id.toString()
        val cookTimeMin = api.cookTime + api.prepTime
        val imageUrl = getPlaceholderImage(id)
        val difficulty = parseDifficulty(api.difficulty)

        val ingredients = api.ingredients.map { ing ->
            val amountStr = buildAmountString(ing.quantity, ing.unit)
            Ingredient(
                name = ing.name,
                amount = amountStr,
                category = mapIngredientCategory(ing.category),
            )
        }

        val steps = api.instructions.mapIndexed { idx, instruction ->
            CookStep(
                index = idx + 1,
                title = "Step ${idx + 1}",
                instruction = instruction,
            )
        }

        return Recipe(
            id = "api_$id",
            title = api.name,
            description = api.description,
            cuisine = api.cuisine.replaceFirstChar { it.uppercase() },
            difficulty = difficulty,
            cookTimeMin = cookTimeMin,
            servings = api.servings,
            caloriesPerServing = api.caloriesPerServing,
            proteinG = api.protein,
            carbsG = 0,
            fatG = 0,
            imageUrl = imageUrl,
            tags = api.dietaryTags,
            ingredients = ingredients,
            steps = steps,
            tips = emptyList(),
            rating = 4.5f,
            reviewCount = 0,
            authorName = "Recipe API",
        )
    }

    private fun parseDifficulty(value: String?): Difficulty {
        if (value == null) return Difficulty.MEDIUM
        val lower = value.lowercase()
        return when {
            lower.contains("easy") || lower.contains("beginner") || lower.contains("simple") -> Difficulty.EASY
            lower.contains("hard") || lower.contains("advanced") || lower.contains("expert") || lower.contains("professional") -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }
    }

    private fun buildAmountString(quantity: Float?, unit: String?): String {
        val parts = mutableListOf<String>()
        if (quantity != null) {
            parts.add(if (quantity == quantity.toLong().toFloat()) {
                quantity.toLong().toString()
            } else {
                quantity.toString()
            })
        }
        if (!unit.isNullOrBlank()) parts.add(unit)
        return parts.joinToString(" ")
    }

    private fun mapIngredientCategory(apiCategory: String): IngredientCategory {
        val lower = apiCategory.lowercase()
        return when {
            listOf("poultry", "meat", "seafood", "egg", "fish").any { lower.contains(it) } -> IngredientCategory.PROTEIN
            listOf("dairy", "cheese").any { lower.contains(it) } -> IngredientCategory.DAIRY
            listOf("grain", "bread", "pasta", "noodle", "flour", "rice").any { lower.contains(it) } -> IngredientCategory.GRAIN
            listOf("spice", "herb", "condiment", "oil").any { lower.contains(it) } -> IngredientCategory.SPICE
            listOf("vegetable", "fruit", "mushroom").any { lower.contains(it) } -> IngredientCategory.PRODUCE
            else -> IngredientCategory.PANTRY
        }
    }

    private fun getPlaceholderImage(id: String): String {
        val images = listOf(
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80",
            "https://images.unsplash.com/photo-1476124369496-e5bda7f4d1a1?w=800&q=80",
            "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&q=80",
            "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800&q=80",
            "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=800&q=80",
            "https://images.unsplash.com/photo-1551782450-a2132b4ba21d?w=800&q=80",
            "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=800&q=80",
            "https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=800&q=80",
        )
        val index = if (id.isNotEmpty()) id.first().code % images.size else 0
        return images[index]
    }

    fun close() {
        backend.close()
    }
}
