package cookscopeai.com.data

import cookscopeai.com.data.model.Difficulty
import cookscopeai.com.data.model.Ingredient
import cookscopeai.com.data.model.Recipe

/**
 * Recipe discovery service that searches recipeapi.io for recipes matching
 * the user's ingredients and preferences. Uses recipeapi.io only — no USDA
 * fallback — so every card is a real recipe from the live database.
 */
class RecipeDiscoveryService {

    private val apiService = RecipeApiService()

    /**
     * Find recipes matching the given ingredients.
     * Returns a list of recipes from the recipeapi.io database.
     */
    suspend fun findRecipes(
        ingredients: List<String>,
        cuisine: String = "",
        skillLevel: String = "",
        isPremium: Boolean = false,
    ): Result<List<Recipe>> {
        return try {
            val result = apiService.getRecipesByIngredients(ingredients, perPage = 20)
            result.fold(
                onSuccess = { recipes ->
                    var filtered = recipes

                    // Filter by cuisine if specified
                    if (cuisine.isNotBlank() && cuisine != "Let AI decide") {
                        filtered = filtered.filter {
                            it.cuisine.contains(cuisine, ignoreCase = true)
                        }
                    }

                    // If no matches after filtering, return unfiltered results
                    if (filtered.isEmpty() && recipes.isNotEmpty()) {
                        filtered = recipes
                    }

                    // Mark premium recipes
                    filtered = filtered.map { it.copy(isPremium = isPremium) }

                    // Cache results
                    RecipeData.setApiRecipes(filtered)

                    Result.success(filtered)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search recipes by text query (name, cuisine, tag, etc).
     * recipeapi.io only — no fallback.
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return try {
            val result = apiService.searchRecipes(query, perPage = 20)
            result.fold(
                onSuccess = { recipes ->
                    RecipeData.setApiRecipes(recipes)
                    Result.success(recipes)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get full recipe details by ID (fetches from API if needed).
     */
    suspend fun getRecipeDetails(id: String): Result<Recipe> {
        // Check cache first
        RecipeData.byId(id)?.let { cached ->
            if (cached.ingredients.isNotEmpty() && cached.steps.isNotEmpty()) {
                return Result.success(cached)
            }
        }

        // Fetch full details from API
        return try {
            // Strip the "api_" prefix to get the raw API ID
            val rawId = id.removePrefix("api_")
            val result = apiService.getRecipe(rawId)
            result.fold(
                onSuccess = { recipe ->
                    val fullRecipe = recipe.copy(id = id)
                    RecipeData.addApiRecipe(fullRecipe)
                    Result.success(fullRecipe)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load today's trending recipes from the server-side cache.
     * Falls back gracefully if the trending table is empty or the
     * network call fails.
     */
    suspend fun loadTrendingRecipes(): Result<List<Recipe>> {
        return try {
            val result = apiService.getTrendingRecipes()
            result.fold(
                onSuccess = { recipes ->
                    RecipeData.setApiRecipes(recipes)
                    Result.success(recipes)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load random recipes for Home screen discovery.
     * recipeapi.io only — no fallback.
     */
    suspend fun loadRandomRecipes(count: Int = 10): Result<List<Recipe>> {
        return try {
            val result = apiService.getRandomRecipes(count)
            result.fold(
                onSuccess = { recipes ->
                    RecipeData.setApiRecipes(recipes)
                    Result.success(recipes)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load the daily Discover feed. Prefers the server-side trending cache,
     * falls back to live random recipeapi.io recipes, and finally uses the
     * local seed recipe library so the Discover screen is never empty.
     */
    suspend fun loadDailyRecipes(count: Int = 20): Result<List<Recipe>> {
        // 1. Try today's server-side trending cache first (fastest, refreshed daily).
        loadTrendingRecipes().getOrNull()?.let { trending ->
            if (trending.isNotEmpty()) {
                RecipeData.setDailyRecipes(trending)
                return Result.success(trending.take(count))
            }
        }

        // 2. Fetch live random recipes from recipeapi.io.
        loadRandomRecipes(count).getOrNull()?.let { recipes ->
            if (recipes.isNotEmpty()) {
                RecipeData.setDailyRecipes(recipes)
                return Result.success(recipes.take(count))
            }
        }

        // 3. Last resort: local seed recipes so the screen is never empty.
        val seedRecipes = RecipeData.seedRecipes.take(count)
        RecipeData.setDailyRecipes(seedRecipes)
        return Result.success(seedRecipes)
    }

    fun close() {
        apiService.close()
    }
}
