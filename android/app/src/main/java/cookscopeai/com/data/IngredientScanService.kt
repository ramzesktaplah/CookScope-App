package cookscopeai.com.data

import cookscopeai.com.data.model.IngredientScanResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AI vision service that detects raw food ingredients in a photo and returns
 * a structured list. Uses the same Rork AI Gateway proxy as [DishScanService]
 * (no API keys ship in the APK).
 */
class IngredientScanService {

    private val deepSeek = DeepSeekService()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Detects raw ingredients in a photo (base64 JPEG, no data: prefix).
     */
    suspend fun detectIngredients(base64Image: String): Result<IngredientScanResult> {
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
                temperature = 0.2,
                maxTokens = 2048,
                model = "openai/gpt-4o-mini",
            )

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Ingredient scan failed"))
            }

            val content = result.getOrThrow()
            val cleaned = content
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = json.decodeFromString<IngredientScanResult>(cleaned)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        deepSeek.close()
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "You are CookScope's Smart Ingredient Scanner. You identify raw food items in a photo and return them as JSON. Always respond with ONLY valid JSON, no markdown, no explanation."

        private const val ANALYSIS_PROMPT = """Look at this photo and identify the raw food ingredients you can see. Return a JSON object with this exact structure:
{
  "ingredients": [
    {"name": "tomato", "category": "PRODUCE", "confidence": 0.95},
    {"name": "garlic", "category": "PRODUCE", "confidence": 0.9}
  ],
  "notes": "optional brief note about ripeness, quantity, or condition"
}

Rules:
- Only include raw food items visible in the photo (produce, meat, fish, dairy, grains, pantry staples, spices).
- Categories must be one of: PRODUCE, PROTEIN, DAIRY, GRAIN, PANTRY, SPICE.
- Confidence is 0.0 to 1.0 based on how certain you are.
- Keep ingredient names simple and common (e.g. "tomato", "chicken breast", "basil").
- If you can see packaged goods, use the likely food inside (e.g. "pasta" not "pasta box").
- notes is optional; include it only if there's something useful to mention.
Return ONLY the JSON object."""
    }
}
