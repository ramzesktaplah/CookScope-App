package cookscopeai.com.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * USDA FoodData Central nutrition data client.
 *
 * All USDA API calls are now proxied through the `usda-search` Supabase Edge
 * Function so the USDA API key never ships in the APK. The edge function
 * handles authentication, rate limiting, and Play Integrity verification.
 *
 * Used alongside DeepSeek to ground chatbot answers in reliable nutritional
 * data. API docs: https://fdc.nal.usda.gov/api-guide.html
 */
class UsdaFoodService(
    private val backend: CookScopeBackendService = CookScopeBackendService(),
) {

    private val json = Json { ignoreUnknownKeys = true }

    // In-memory cache for common food lookups
    private val foodCache = mutableMapOf<String, FoodSearchResult>()
    private val nutritionCache = mutableMapOf<Int, FoodNutrition>()

    @Serializable
    data class FoodNutrient(
        @SerialName("nutrientId") val nutrientId: Int = 0,
        @SerialName("nutrientName") val nutrientName: String = "",
        @SerialName("nutrientNumber") val nutrientNumber: String = "",
        @SerialName("unitName") val unitName: String = "",
        @SerialName("value") val value: Double = 0.0,
    )

    @Serializable
    data class FoodItem(
        @SerialName("fdcId") val fdcId: Int = 0,
        @SerialName("description") val description: String = "",
        @SerialName("dataType") val dataType: String = "",
        @SerialName("foodCategory") val foodCategory: String? = null,
        @SerialName("foodNutrients") val foodNutrients: List<FoodNutrient> = emptyList(),
    )

    @Serializable
    data class FoodSearchResponse(
        @SerialName("foods") val foods: List<FoodItem> = emptyList(),
        @SerialName("totalHits") val totalHits: Int = 0,
        @SerialName("currentPage") val currentPage: Int = 0,
    )

    data class FoodSearchResult(
        val fdcId: Int,
        val description: String,
        val dataType: String,
        val category: String?,
    )

    data class FoodNutrition(
        val fdcId: Int,
        val description: String,
        val calories: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val fiberG: Double,
        val sugarG: Double,
        val sodiumMg: Double,
        val ironMg: Double,
        val calciumMg: Double,
        val vitaminC: Double,
        val cholesterolMg: Double,
        val potassiumMg: Double,
    )

    /**
     * Search for foods by query string via the backend edge function.
     * Results are cached for subsequent lookups.
     */
    suspend fun searchFoods(query: String, pageSize: Int = 5): Result<List<FoodSearchResult>> {
        val cacheKey = query.lowercase().trim()
        foodCache[cacheKey]?.let { return Result.success(listOf(it)) }

        return try {
            val result = backend.usdaSearch(query, pageSize)
            result.fold(
                onSuccess = { raw ->
                    val searchResponse = json.decodeFromString<FoodSearchResponse>(raw)
                    val results = searchResponse.foods.map { food ->
                        FoodSearchResult(
                            fdcId = food.fdcId,
                            description = food.description,
                            dataType = food.dataType,
                            category = food.foodCategory,
                        )
                    }
                    if (results.isNotEmpty()) {
                        foodCache[cacheKey] = results.first()
                    }
                    Result.success(results)
                },
                onFailure = { Result.failure(it) },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get detailed nutrition data for a specific food by FDC ID via the
     * backend edge function. Results are cached.
     */
    suspend fun getNutrition(fdcId: Int): Result<FoodNutrition> {
        nutritionCache[fdcId]?.let { return Result.success(it) }

        return try {
            val result = backend.usdaNutrition(fdcId)
            result.fold(
                onSuccess = { raw ->
                    val foodItem = json.decodeFromString<FoodItem>(raw)
                    val nutrition = extractNutrition(foodItem)
                    nutritionCache[fdcId] = nutrition
                    Result.success(nutrition)
                },
                onFailure = { Result.failure(it) },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search for a food and return its nutrition data in one call.
     * Convenience method for chatbot queries like "nutrition of salmon".
     */
    suspend fun getFoodNutrition(query: String): Result<FoodNutrition> {
        return try {
            val searchResult = searchFoods(query, pageSize = 1)
            searchResult.fold(
                onSuccess = { results ->
                    if (results.isEmpty()) {
                        return Result.failure(Exception("No food found for: $query"))
                    }
                    getNutrition(results.first().fdcId)
                },
                onFailure = { Result.failure(it) },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compare nutrition data for multiple foods.
     * Returns a map of food name to nutrition data.
     */
    suspend fun compareFoods(queries: List<String>): Result<Map<String, FoodNutrition>> {
        return try {
            val results = mutableMapOf<String, FoodNutrition>()
            for (query in queries) {
                val nutrition = getFoodNutrition(query)
                nutrition.fold(
                    onSuccess = { data -> results[query] = data },
                    onFailure = { /* skip failed lookups */ },
                )
            }
            if (results.isEmpty()) {
                Result.failure(Exception("Could not find nutrition data for any of: ${queries.joinToString()}"))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats nutrition data as a readable string for inclusion in DeepSeek prompts.
     */
    fun formatNutritionForPrompt(foodName: String, nutrition: FoodNutrition): String {
        return buildString {
            appendLine("$foodName (per 100g):")
            appendLine("  Calories: ${nutrition.calories} kcal")
            appendLine("  Protein: ${nutrition.proteinG} g")
            appendLine("  Carbs: ${nutrition.carbsG} g")
            appendLine("  Fat: ${nutrition.fatG} g")
            appendLine("  Fiber: ${nutrition.fiberG} g")
            appendLine("  Sugar: ${nutrition.sugarG} g")
            appendLine("  Sodium: ${nutrition.sodiumMg} mg")
            appendLine("  Iron: ${nutrition.ironMg} mg")
            appendLine("  Calcium: ${nutrition.calciumMg} mg")
            appendLine("  Vitamin C: ${nutrition.vitaminC} mg")
            appendLine("  Cholesterol: ${nutrition.cholesterolMg} mg")
            appendLine("  Potassium: ${nutrition.potassiumMg} mg")
        }
    }

    private fun extractNutrition(food: FoodItem): FoodNutrition {
        val nutrients = food.foodNutrients.associateBy { it.nutrientId }

        fun getValue(id: Int): Double = nutrients[id]?.value ?: 0.0

        // Standard USDA nutrient IDs
        return FoodNutrition(
            fdcId = food.fdcId,
            description = food.description,
            calories = getValue(1008),       // Energy (kcal)
            proteinG = getValue(1003),       // Protein
            carbsG = getValue(1005),         // Carbohydrate, by difference
            fatG = getValue(1004),           // Total lipid (fat)
            fiberG = getValue(1079),         // Fiber, total dietary
            sugarG = getValue(2000),         // Total sugars
            sodiumMg = getValue(1093),       // Sodium, Na
            ironMg = getValue(1089),         // Iron, Fe
            calciumMg = getValue(1087),      // Calcium, Ca
            vitaminC = getValue(1162),       // Vitamin C, total ascorbic acid
            cholesterolMg = getValue(1253),  // Cholesterol
            potassiumMg = getValue(1092),    // Potassium, K
        )
    }

    fun close() {
        backend.close()
    }

    companion object {
        // Common food names for proactive caching
        val COMMON_FOODS = listOf("chicken", "rice", "eggs", "milk", "salmon", "beef", "pasta", "broccoli")
    }
}
