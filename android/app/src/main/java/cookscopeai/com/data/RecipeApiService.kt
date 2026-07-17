package cookscopeai.com.data

import cookscopeai.com.data.model.CookStep
import cookscopeai.com.data.model.Difficulty
import cookscopeai.com.data.model.Ingredient
import cookscopeai.com.data.model.IngredientCategory
import cookscopeai.com.data.model.Recipe
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Service for fetching recipes from recipeapi.io — a REST API with 50,000+ recipes.
 * Uses Bearer token authentication with the development API key.
 */
class RecipeApiService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "https://recipeapi.io/api/v1"
    private val apiKey = "sk_live_Txu9GNOnm5fqSb3CTITHbT55FbKLBVQeb08SNOWe7589f535"
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
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/recipes?search=$encodedQuery&lang=en&page=$page&per_page=$perPage"
            val response = makeGetRequest(url)
            if (!response.isSuccessful()) {
                return Result.failure(Exception("Search failed: ${response.statusCode()}"))
            }
            val body = response.bodyText()
            val listResponse = jsonParser.decodeFromString<RecipeListResponse>(body)
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
            val ingredientsParam = ingredients.joinToString(",") {
                java.net.URLEncoder.encode(it.trim(), "UTF-8")
            }
            val url = "$baseUrl/recipes?ingredients=$ingredientsParam&lang=en&page=$page&per_page=$perPage"
            val response = makeGetRequest(url)
            if (!response.isSuccessful()) {
                return Result.failure(Exception("Recipe search failed: ${response.statusCode()}"))
            }
            val body = response.bodyText()
            val listResponse = jsonParser.decodeFromString<RecipeListResponse>(body)
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
            val url = "$baseUrl/recipes/${java.net.URLEncoder.encode(id, "UTF-8")}?lang=en"
            val response = makeGetRequest(url)
            if (!response.isSuccessful()) {
                return Result.failure(Exception("Recipe fetch failed: ${response.statusCode()}"))
            }
            val body = response.bodyText()
            val detailResponse = jsonParser.decodeFromString<RecipeDetailResponse>(body)
            val detail = detailResponse.data
                ?: return Result.failure(Exception("Recipe not found"))
            Result.success(mapApiRecipeToRecipe(detail))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get random recipes for discovery on the Home screen.
     * The API returns one random recipe per call.
     */
    suspend fun getRandomRecipes(count: Int = 10): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()
            val fetchCount = minOf(count, 10)
            for (i in 0 until fetchCount) {
                val url = "$baseUrl/recipes/random?lang=en"
                val response = makeGetRequest(url)
                if (response.isSuccessful()) {
                    val body = response.bodyText()
                    val detailResponse = jsonParser.decodeFromString<RecipeDetailResponse>(body)
                    detailResponse.data?.let { detail ->
                        recipes.add(mapApiRecipeToRecipe(detail))
                    }
                }
            }
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
     * Fetch available ingredients from the API (for autocomplete suggestions).
     */
    suspend fun getIngredients(page: Int = 1, perPage: Int = 50): Result<List<ApiIngredientInfo>> {
        return try {
            val url = "$baseUrl/ingredients?lang=en&page=$page&per_page=$perPage"
            val response = makeGetRequest(url)
            if (!response.isSuccessful()) {
                return Result.failure(Exception("Ingredients fetch failed: ${response.statusCode()}"))
            }
            val body = response.bodyText()
            val listResponse = jsonParser.decodeFromString<IngredientsListResponse>(body)
            Result.success(listResponse.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun makeGetRequest(url: String): HttpResponseWrapper {
        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.Accept, "application/json")
        }
        return HttpResponseWrapper(
            statusCode = response.status.value,
            body = response.bodyAsText(),
        )
    }

    /**
     * Map an ApiRecipe from recipeapi.io to the app's Recipe model.
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
        client.close()
    }
}

private data class HttpResponseWrapper(
    private val statusCode: Int,
    private val body: String,
) {
    fun statusCode(): Int = statusCode
    fun isSuccessful(): Boolean = statusCode in 200..299
    fun bodyText(): String = body
}
