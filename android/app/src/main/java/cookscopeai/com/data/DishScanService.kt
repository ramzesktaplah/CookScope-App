package cookscopeai.com.data

import cookscopeai.com.data.model.DishAnalysis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AI vision service that analyzes a dish photo and returns structured nutrition info.
 * Uses a vision-capable model (openai/gpt-4o-mini) via the Rork AI Gateway proxy.
 */
class DishScanService {

    private val deepSeek = DeepSeekService()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Analyzes a dish photo (base64 JPEG, no data: prefix) and returns structured analysis.
     */
    suspend fun analyzeDish(base64Image: String): Result<DishAnalysis> {
        return try {
            val userContent = buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    put("text", ANALYSIS_PROMPT)
                })
                add(buildJsonObject {
                    put("type", "image_url")
                    put("image_url", buildJsonObject {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }

            val messagesArray = buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userContent)
                })
            }

            val result = deepSeek.completeWithRawMessages(
                messagesJson = messagesArray,
                temperature = 0.3,
                maxTokens = 4096,
                thinking = false,
                reasoningEffort = "medium",
                model = "openai/gpt-4o-mini",
            )

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Dish scan failed"))
            }

            val content = result.getOrThrow()
            val cleaned = content
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val analysis = json.decodeFromString<DishAnalysis>(cleaned)
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """You are CookScope's AI dish analyzer. You analyze food photos and return precise nutritional information as JSON. Always respond with ONLY valid JSON, no markdown, no explanation."""

        private const val ANALYSIS_PROMPT = """Analyze this dish photo and return a JSON object with these exact fields:
{
  "dishName": "name of the dish",
  "description": "1-2 sentence description of what the dish is",
  "ingredients": [{"name": "ingredient name", "amount": "estimated amount"}],
  "primaryIngredients": [{"name": "main ingredient", "amount": "estimated amount"}],
  "secondaryIngredients": [{"name": "supporting ingredient", "amount": "estimated amount"}],
  "seasoningSpices": [{"name": "spice or seasoning", "amount": "estimated amount"}],
  "calories": estimated total calories as integer,
  "proteinG": protein in grams as integer,
  "carbsG": carbs in grams as integer,
  "fatG": fat in grams as integer,
  "fiberG": fiber in grams as integer,
  "sugarG": sugar in grams as integer,
  "sodiumMg": sodium in mg as integer,
  "dietaryTags": ["vegetarian", "vegan", "gluten-free", "keto", "high-protein", etc],
  "allergens": ["dairy", "nuts", "gluten", "eggs", "soy", "shellfish", etc],
  "healthScore": 1-100 integer score based on nutritional balance,
  "healthNotes": "brief health assessment and a small note about the dish",
  "portionSize": "estimated portion size",
  "mealType": "breakfast/lunch/dinner/snack/dessert",
  "cookingMethods": ["grilled", "fried", "baked", "steamed", "roasted", etc],
  "difficulty": "Easy" or "Medium" or "Hard",
  "cookingTimeMin": estimated cooking time in minutes as integer,
  "servingSize": "e.g. 1 plate (350g)",
  "improvements": ["suggestion 1 to make it healthier", "suggestion 2", etc],
  "perfectPairings": ["side dish or drink pairing 1", "pairing 2", etc],
  "healthyAlternatives": ["lighter substitution 1", "substitution 2", etc],
  "nutritionalAnalysis": "2-3 sentence complete nutritional analysis summary"
}

Rules:
- Be thorough and realistic with estimates.
- Categorize ingredients: primaryIngredients = main components (protein, grain base), secondaryIngredients = supporting items (vegetables, sauce components), seasoningSpices = herbs, spices, condiments, salt, pepper.
- If you cannot determine a field, use an empty string, 0, or empty array rather than null.
- Return ONLY the JSON object, no markdown fences."""
    }
}
