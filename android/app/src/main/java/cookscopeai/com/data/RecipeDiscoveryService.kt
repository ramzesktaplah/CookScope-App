package cookscopeai.com.data

import cookscopeai.com.data.model.Recipe

/**
 * Recipe discovery service that searches recipeapi.io for recipes matching
 * the user's ingredients and preferences. Replaces the previous AI generation approach.
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

    fun close() {
        apiService.close()
    }
}
