package cookscopeai.com.data

import cookscopeai.com.data.model.Difficulty
import cookscopeai.com.data.model.Ingredient
import cookscopeai.com.data.model.Recipe

/**
 * Recipe discovery service that searches recipeapi.io for recipes matching
 * the user's ingredients and preferences. Replaces the previous AI generation approach.
 * Falls back to USDA FoodData Central when recipeapi.io is unavailable.
 */
class RecipeDiscoveryService {

    private val apiService = RecipeApiService()
    private val usdaService = UsdaFoodService()

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
     * Falls back to USDA food items if recipeapi.io is unavailable.
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return try {
            val result = apiService.searchRecipes(query, perPage = 20)
            result.fold(
                onSuccess = { recipes ->
                    RecipeData.setApiRecipes(recipes)
                    Result.success(recipes)
                },
                onFailure = { fallbackToUsdaSearch(query) }
            )
        } catch (e: Exception) {
            fallbackToUsdaSearch(query)
        }
    }

    private suspend fun fallbackToUsdaSearch(query: String): Result<List<Recipe>> {
        return usdaService.searchFoods(query, pageSize = 20).map { foods ->
            foods.map { mapUsdaFoodToRecipeWithNutrition(it) }
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
     * First tries recipeapi.io; if that fails, falls back to USDA common foods.
     */
    suspend fun loadRandomRecipes(count: Int = 10): Result<List<Recipe>> {
        return try {
            val result = apiService.getRandomRecipes(count)
            result.fold(
                onSuccess = { recipes ->
                    RecipeData.setApiRecipes(recipes)
                    Result.success(recipes)
                },
                onFailure = { fallbackToUsdaDiscovery(count) }
            )
        } catch (e: Exception) {
            fallbackToUsdaDiscovery(count)
        }
    }

    private suspend fun fallbackToUsdaDiscovery(count: Int): Result<List<Recipe>> {
        val foods = UsdaFoodService.COMMON_FOODS.shuffled().take(count)
        val results = foods.mapNotNull { food ->
            usdaService.searchFoods(food, pageSize = 1).getOrNull()?.firstOrNull()
        }.map { mapUsdaFoodToRecipeWithNutrition(it) }
        return if (results.isNotEmpty()) {
            Result.success(results)
        } else {
            Result.failure(Exception("No recipes or foods available"))
        }
    }

    private fun mapUsdaFoodToRecipe(food: UsdaFoodService.FoodSearchResult): Recipe {
        return Recipe(
            id = "usda_${food.fdcId}",
            title = food.description,
            description = "${food.category ?: "USDA food item"}. Nutrition per 100g.",
            cuisine = "USDA",
            difficulty = Difficulty.EASY,
            cookTimeMin = 0,
            servings = 1,
            caloriesPerServing = 0,
            proteinG = 0,
            carbsG = 0,
            fatG = 0,
            imageUrl = "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=800&q=80",
            tags = listOf("USDA", "Nutrition"),
            ingredients = listOf(Ingredient(food.description, "100g")),
            steps = emptyList(),
            tips = emptyList(),
            rating = 4.0f,
            reviewCount = 0,
            authorName = "CookScopeAI",
        )
    }

    private suspend fun mapUsdaFoodToRecipeWithNutrition(food: UsdaFoodService.FoodSearchResult): Recipe {
        val nutrition = usdaService.getNutrition(food.fdcId).getOrNull()
        val base = mapUsdaFoodToRecipe(food)
        return if (nutrition != null) {
            base.copy(
                caloriesPerServing = nutrition.calories.toInt(),
                proteinG = nutrition.proteinG.toInt(),
                carbsG = nutrition.carbsG.toInt(),
                fatG = nutrition.fatG.toInt(),
            )
        } else {
            base
        }
    }

    fun close() {
        apiService.close()
        usdaService.close()
    }
}
