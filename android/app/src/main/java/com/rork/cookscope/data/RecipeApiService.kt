package com.rork.cookscope.data

import com.rork.cookscope.data.model.CookStep
import com.rork.cookscope.data.model.Difficulty
import com.rork.cookscope.data.model.Ingredient
import com.rork.cookscope.data.model.IngredientCategory
import com.rork.cookscope.data.model.Recipe
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
            val recipes = listResponse.data.map { mapSummaryToRecipe(it) }
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
            val recipes = listResponse.data.map { mapSummaryToRecipe(it) }
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
            Result.success(mapDetailToRecipe(detail))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get random recipes for discovery on the Home screen.
     */
    suspend fun getRandomRecipes(count: Int = 10): Result<List<Recipe>> {
        return try {
            val recipes = mutableListOf<Recipe>()
            // Fetch random recipes one at a time (API returns single random recipe per call)
            val fetchCount = minOf(count, 10)
            for (i in 0 until fetchCount) {
                val url = "$baseUrl/recipes/random?lang=en"
                val response = makeGetRequest(url)
                if (response.isSuccessful()) {
                    val body = response.bodyText()
                    val detailResponse = jsonParser.decodeFromString<RecipeDetailResponse>(body)
                    detailResponse.data?.let { detail ->
                        recipes.add(mapDetailToRecipe(detail))
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

    private fun mapSummaryToRecipe(summary: ApiRecipeSummary): Recipe {
        val id = extractId(summary.id)
        val nutrition = summary.nutrition
        val meta = summary.meta
        val cookTimeMin = parseDurationToMinutes(meta?.totalTime ?: meta?.activeTime)
        val servings = meta?.yieldCount ?: 4
        val imageUrl = summary.imageUrl ?: summary.image ?: getPlaceholderImage(id)
        val difficulty = parseDifficulty(summary.difficulty)
        val tags = summary.tags.ifEmpty { summary.dietary?.flags ?: emptyList() }

        return Recipe(
            id = "api_$id",
            title = summary.name ?: "Unknown Recipe",
            description = summary.description ?: "",
            cuisine = summary.cuisine ?: summary.category ?: "Various",
            difficulty = difficulty,
            cookTimeMin = cookTimeMin,
            servings = servings,
            caloriesPerServing = nutrition?.calories ?: 0,
            proteinG = nutrition?.protein ?: 0,
            carbsG = nutrition?.carbohydrates ?: 0,
            fatG = nutrition?.fat ?: 0,
            imageUrl = imageUrl,
            tags = tags,
            ingredients = emptyList(),
            steps = emptyList(),
            tips = emptyList(),
            rating = 4.5f,
            reviewCount = 0,
            authorName = "Recipe API",
        )
    }

    private fun mapDetailToRecipe(detail: ApiRecipeDetail): Recipe {
        val id = extractId(detail.id)
        val nutrition = detail.nutrition
        val meta = detail.meta
        val cookTimeMin = parseDurationToMinutes(meta?.totalTime ?: meta?.activeTime)
        val servings = meta?.yieldCount ?: 4
        val imageUrl = detail.imageUrl ?: detail.image ?: getPlaceholderImage(id)
        val difficulty = parseDifficulty(detail.difficulty)
        val tags = detail.tags.ifEmpty { detail.dietary?.flags ?: emptyList() }

        val ingredients = detail.ingredients.flatMap { group ->
            group.items.map { item ->
                val amountStr = buildAmountString(item.quantity, item.unit, item.preparation)
                Ingredient(
                    name = item.name,
                    amount = amountStr,
                    category = guessIngredientCategory(item.name),
                )
            }
        }

        val steps = detail.instructions.mapIndexed { idx, instr ->
            CookStep(
                index = instr.step.takeIf { it > 0 } ?: (idx + 1),
                title = instr.title ?: instr.phase ?: "Step ${idx + 1}",
                instruction = instr.instruction,
                durationMin = parseDurationToMinutes(instr.duration),
                technique = instr.technique,
                tip = instr.tip ?: instr.donenessCue,
            )
        }

        val tips = detail.chefNotes.ifEmpty {
            detail.troubleshooting.map { "${it.problem}: ${it.solution}" }
        }

        return Recipe(
            id = "api_$id",
            title = detail.name ?: "Unknown Recipe",
            description = detail.description ?: "",
            cuisine = detail.cuisine ?: detail.category ?: "Various",
            difficulty = difficulty,
            cookTimeMin = cookTimeMin,
            servings = servings,
            caloriesPerServing = nutrition?.calories ?: 0,
            proteinG = nutrition?.protein ?: 0,
            carbsG = nutrition?.carbohydrates ?: 0,
            fatG = nutrition?.fat ?: 0,
            imageUrl = imageUrl,
            tags = tags,
            ingredients = ingredients,
            steps = steps,
            tips = tips,
            rating = 4.5f,
            reviewCount = 0,
            authorName = "Recipe API",
        )
    }

    private fun extractId(element: JsonElement?): String {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull ?: ""
            null -> ""
            else -> element.toString().removeSurrounding("\"")
        }
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

    private fun parseDurationToMinutes(duration: String?): Int {
        if (duration.isNullOrBlank()) return 0
        // Parse ISO 8601 duration like "PT45M" or "PT2H30M"
        val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
        val match = regex.find(duration)
        if (match != null) {
            val hours = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
            val minutes = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            return hours * 60 + minutes
        }
        // Try plain number (assume minutes)
        return duration.toIntOrNull() ?: 0
    }

    private fun buildAmountString(quantity: Float?, unit: String?, preparation: String?): String {
        val parts = mutableListOf<String>()
        if (quantity != null) {
            parts.add(if (quantity == quantity.toLong().toFloat()) {
                quantity.toLong().toString()
            } else {
                quantity.toString()
            })
        }
        if (!unit.isNullOrBlank()) parts.add(unit)
        if (!preparation.isNullOrBlank()) parts.add("($preparation)")
        return parts.joinToString(" ")
    }

    private fun guessIngredientCategory(name: String): IngredientCategory {
        val lower = name.lowercase()
        return when {
            listOf("chicken", "beef", "pork", "lamb", "fish", "salmon", "shrimp", "tofu", "egg", "turkey", "bacon", "sausage", "duck", "cod", "tuna").any { lower.contains(it) } -> IngredientCategory.PROTEIN
            listOf("milk", "cream", "cheese", "butter", "yogurt", "parmesan", "mozzarella", "feta").any { lower.contains(it) } -> IngredientCategory.DAIRY
            listOf("rice", "pasta", "flour", "bread", "noodle", "quinoa", "oats", "couscous", "tortilla", "bun").any { lower.contains(it) } -> IngredientCategory.GRAIN
            listOf("salt", "pepper", "cumin", "paprika", "oregano", "basil", "thyme", "rosemary", "cinnamon", "turmeric", "chili", "powder", "spice").any { lower.contains(it) } -> IngredientCategory.SPICE
            else -> IngredientCategory.PRODUCE
        }
    }

    private fun getPlaceholderImage(id: String): String {
        val images = listOf(
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80",
            "https://images.unsplash.com/photo-1476124369496-e5bda7f4d1a1?w=800&q=80",
            "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&q=80",
            "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800&q=80",
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
