package cookscopeai.com.data

import cookscopeai.com.data.model.CookStep
import cookscopeai.com.data.model.Difficulty
import cookscopeai.com.data.model.Ingredient
import cookscopeai.com.data.model.IngredientCategory
import cookscopeai.com.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * AI recipe generation service powered by DeepSeek-v4-flash via NVIDIA.
 * Sends the user's ingredients and preferences to DeepSeek and returns a structured Recipe.
 * Uses thinking/reasoning mode for higher-quality recipe creation.
 */
class RecipeGenerationService {

    private val deepSeek = DeepSeekService()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Generates a recipe based on the given parameters.
     * Returns a Recipe object on success, or an error on failure.
     */
    suspend fun generateRecipe(
        ingredients: List<String>,
        cuisine: String,
        dishType: String,
        skillLevel: String,
        dietaryMode: String,
        isPremium: Boolean,
    ): Result<Recipe> {
        return try {
            val ingredientStr = ingredients.joinToString(", ")
            val qualityHint = if (isPremium) {
                "Create a premium, restaurant-quality recipe with refined techniques, professional plating, and elevated flavors."
            } else {
                "Create a delicious, approachable home-style recipe that's practical and easy to follow."
            }

            val userMessage = """Generate a recipe with these specifications:
Ingredients available: $ingredientStr
Cuisine: $cuisine
Dish type: $dishType
Cook skill level: $skillLevel
Dietary mode: $dietaryMode
$qualityHint

Return ONLY a valid JSON object with this exact structure:
{
  "title": "recipe name",
  "description": "1-2 sentence description",
  "cuisine": "$cuisine",
  "difficulty": "EASY" or "MEDIUM" or "HARD",
  "cookTimeMin": estimated minutes as integer,
  "servings": integer,
  "caloriesPerServing": integer,
  "proteinG": integer,
  "carbsG": integer,
  "fatG": integer,
  "tags": ["tag1", "tag2", "tag3"],
  "ingredients": [{"name": "ingredient", "amount": "amount", "category": "PRODUCE"}],
  "steps": [{"index": 1, "title": "step title", "instruction": "detailed instruction", "durationMin": 5, "technique": "technique name", "tip": "optional tip"}],
  "tips": ["tip1", "tip2", "tip3"],
  "authorName": "CookScope AI"
}

Use the provided ingredients and supplement with common pantry items as needed.
Ingredient categories must be one of: PRODUCE, PROTEIN, DAIRY, GRAIN, PANTRY, SPICE.
Return ONLY the JSON, no markdown, no explanation."""

            val messages = listOf(
                DeepSeekService.Message(
                    role = "system",
                    content = "You are CookScope's AI recipe generator powered by DeepSeek. You create detailed, practical recipes based on available ingredients and user preferences. Always respond with ONLY valid JSON, no markdown.",
                ),
                DeepSeekService.Message(
                    role = "user",
                    content = userMessage,
                ),
            )

            val result = deepSeek.chat(
                messages = messages,
                temperature = 0.8,
                maxTokens = 8192,
                thinking = true,
                reasoningEffort = "high",
            )

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Generation failed"))
            }

            val content = result.getOrThrow()
            val cleaned = content
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val recipeJson = json.parseToJsonElement(cleaned).jsonObject
            val recipe = parseRecipe(recipeJson, isPremium)
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRecipe(obj: JsonObject, isPremium: Boolean): Recipe {
        val id = "ai_${System.currentTimeMillis()}"
        val difficultyStr = obj["difficulty"]?.jsonPrimitive?.contentOrNull ?: "MEDIUM"
        val difficulty = try { Difficulty.valueOf(difficultyStr) } catch (_: Exception) { Difficulty.MEDIUM }

        val ingredients = (obj["ingredients"]?.jsonArray ?: emptyList<JsonElement>()).map { item ->
            val ing = item.jsonObject
            val catStr = ing["category"]?.jsonPrimitive?.contentOrNull ?: "PANTRY"
            val category = try { IngredientCategory.valueOf(catStr) } catch (_: Exception) { IngredientCategory.PANTRY }
            Ingredient(
                name = ing["name"]?.jsonPrimitive?.contentOrNull ?: "",
                amount = ing["amount"]?.jsonPrimitive?.contentOrNull ?: "",
                category = category,
            )
        }

        val steps = (obj["steps"]?.jsonArray ?: emptyList<JsonElement>()).mapIndexed { idx, item ->
            val step = item.jsonObject
            CookStep(
                index = step["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: (idx + 1),
                title = step["title"]?.jsonPrimitive?.contentOrNull ?: "",
                instruction = step["instruction"]?.jsonPrimitive?.contentOrNull ?: "",
                durationMin = step["durationMin"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                technique = step["technique"]?.jsonPrimitive?.contentOrNull,
                tip = step["tip"]?.jsonPrimitive?.contentOrNull,
            )
        }

        val tags = (obj["tags"]?.jsonArray ?: emptyList<JsonElement>()).mapNotNull {
            it.jsonPrimitive.contentOrNull
        }

        val tips = (obj["tips"]?.jsonArray ?: emptyList<JsonElement>()).mapNotNull {
            it.jsonPrimitive.contentOrNull
        }

        val imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80"

        return Recipe(
            id = id,
            title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "AI Recipe",
            description = obj["description"]?.jsonPrimitive?.contentOrNull ?: "",
            cuisine = obj["cuisine"]?.jsonPrimitive?.contentOrNull ?: "AI",
            difficulty = difficulty,
            cookTimeMin = obj["cookTimeMin"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 30,
            servings = obj["servings"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 4,
            caloriesPerServing = obj["caloriesPerServing"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
            proteinG = obj["proteinG"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
            carbsG = obj["carbsG"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
            fatG = obj["fatG"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
            imageUrl = imageUrl,
            tags = tags,
            ingredients = ingredients,
            steps = steps,
            tips = tips,
            rating = if (isPremium) 4.8f else 4.5f,
            reviewCount = 0,
            authorName = obj["authorName"]?.jsonPrimitive?.contentOrNull ?: "CookScope AI",
            isPremium = isPremium,
        )
    }

    fun close() {
        deepSeek.close()
    }
}
