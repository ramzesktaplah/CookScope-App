package cookscopeai.com.data

import cookscopeai.com.data.model.DishAnalysis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AI vision service that analyzes a dish photo and returns structured nutrition info.
 * Uses DeepSeek-v4-flash via NVIDIA integrate API.
 *
 * Note: DeepSeek-v4-flash is a text model. For vision tasks, we use a two-step approach:
 * 1. If the NVIDIA platform supports vision with this model, we send the image directly
 * 2. Otherwise, we use a text-based analysis prompt as fallback
 *
 * Currently sends image_url in the OpenAI-compatible format — NVIDIA's integrate API
 * supports this for vision-capable models. If the model doesn't support vision,
 * the caller should handle the error gracefully.
 */
class DishScanService {

    private val deepSeek = DeepSeekService()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Analyzes a dish photo (base64 JPEG, no data: prefix) and returns structured analysis.
     */
    suspend fun analyzeDish(base64Image: String): Result<DishAnalysis> {
        return try {
            // Build multimodal content (text + image_url in OpenAI-compatible format)
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

            // For multimodal, we need to send the content as a JSON array.
            // We build the request manually since DeepSeekService.Message uses String content.
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

            // Use the raw DeepSeek API for multimodal support
            val result = deepSeek.completeWithRawMessages(
                messagesJson = messagesArray,
                temperature = 0.3,
                maxTokens = 4096,
                thinking = false,
                reasoningEffort = "medium",
            )

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Scan failed"))
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
        private const val SYSTEM_PROMPT = """You are CookScope's AI dish analyzer powered by DeepSeek. You analyze food photos and return precise nutritional information as JSON. Always respond with ONLY valid JSON, no markdown, no explanation."""

        private const val ANALYSIS_PROMPT = """Analyze this dish photo and return a JSON object with these exact fields:
{
  "dishName": "name of the dish",
  "description": "1-2 sentence description of what the dish is",
  "ingredients": [{"name": "ingredient name", "amount": "estimated amount"}],
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
  "healthNotes": "brief health assessment and suggestions",
  "portionSize": "estimated portion size",
  "mealType": "breakfast/lunch/dinner/snack/dessert"
}

Be thorough and realistic with estimates. Return ONLY the JSON object."""
    }
}
