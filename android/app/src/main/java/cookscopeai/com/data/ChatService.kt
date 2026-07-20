package cookscopeai.com.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AI chat service powered by the Rork AI Gateway proxy.
 *
 * Model selection:
 *  - Chat (free and premium): gpt-4o-mini via the CookScope backend
 *
 * The system prompt is personalized using the user's saved cooking preferences
 * (dietary tags, skill level, favorite cuisines, allergies) so the assistant
 * adapts its suggestions to each user.
 *
 * Nutrition-related queries are enriched with USDA FoodData Central data before
 * being sent to the model. Photo attachments are accepted only when they depict
 * food, dishes, ingredients, recipes, or anything cooking-related.
 */
class ChatService {

    private val gateway = DeepSeekService()
    private val backend = CookScopeBackendService()
    private val usda = UsdaFoodService()

    data class ChatMessage(
        val role: String,
        val content: String,
        val imageBase64: String? = null,
    )

    sealed class SendResult {
        data class Success(val reply: String) : SendResult()
        data class Error(val message: String) : SendResult()
        data object DeclinedNonFood : SendResult()
    }

    /**
     * Sends conversation history to the AI and returns the assistant's reply.
     * Automatically detects nutrition-related queries and enriches them with
     * USDA FoodData Central data before sending to the model.
     *
     * @param messages Conversation history (last entry is the new user message).
     * @param onToken Called with the assistant reply when it arrives.
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
    ): SendResult {
        return try {
            val lastUserMessage = messages.lastOrNull { it.role == "user" }
                ?: return SendResult.Error("No user message found.")

            // Validate image attachment if present.
            val imageBase64 = lastUserMessage.imageBase64
            if (imageBase64 != null) {
                val isFood = validateFoodImage(imageBase64)
                if (!isFood) {
                    return SendResult.DeclinedNonFood
                }
            }

            // Check if this is a nutrition-related query
            val nutritionContext = fetchNutritionContext(lastUserMessage.content)

            // Build a personalized system prompt from the user's preferences
            val basePrompt = buildPersonalizedSystemPrompt()
            val systemPrompt = if (nutritionContext != null) {
                "$basePrompt\n\nYou have access to factual nutrition data from the USDA FoodData Central database. Use the provided data to ground your answer in real nutritional information.\n\n$nutritionContext"
            } else {
                basePrompt
            }

            val model = DeepSeekService.MODEL_CHAT_FREE
            val modelMessages = buildMultimodalMessages(systemPrompt, messages)

            val result = backend.aiChatMultimodal(
                messagesJson = modelMessages,
                model = model,
                temperature = 0.7,
                maxTokens = 4096,
            )

            result.fold(
                onSuccess = { content ->
                    onToken(content.content)
                    SendResult.Success(content.content)
                },
                onFailure = { SendResult.Error(it.message ?: "AI service failed. Please try again.") }
            )
        } catch (e: Exception) {
            SendResult.Error("Network error. Check your connection and try again.")
        }
    }

    /**
     * Asks a small vision check whether the supplied image is food-related.
     * Returns true if the image shows food, a dish, ingredients, recipe, or
     * anything cooking-related. False otherwise.
     */
    private suspend fun validateFoodImage(base64: String): Boolean {
        val prompt = "Is this image related to food, a dish, ingredients, a recipe, or cooking? Reply only YES or NO."
        val messages = buildJsonArray {
            add(buildJsonObject {
                put("role", "user")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", prompt)
                    })
                    add(buildJsonObject {
                        put("type", "image_url")
                        put("image_url", buildJsonObject {
                            put("url", "data:image/jpeg;base64,$base64")
                            put("detail", "low")
                        })
                    })
                })
            })
        }

        return try {
            val result = backend.aiChatMultimodal(
                messagesJson = messages,
                model = DeepSeekService.MODEL_CHAT_FREE,
                temperature = 0.0,
                maxTokens = 16,
            )
            result.getOrNull()?.content?.trim()?.uppercase()?.startsWith("YES") == true
        } catch (_: Exception) {
            // If validation fails, be permissive so the user isn't blocked.
            true
        }
    }

    /**
     * Builds a multimodal message array compatible with the backend's ai-chat
     * function. Text-only messages use simple string content; messages with an
     * image use the OpenAI image_url content array format.
     */
    private fun buildMultimodalMessages(
        systemPrompt: String,
        messages: List<ChatMessage>,
    ): JsonArray {
        return buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", systemPrompt)
            })

            messages.forEach { msg ->
                if (msg.imageBase64 != null) {
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", msg.content.ifBlank { "What can you tell me about this?" })
                            })
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject {
                                    put("url", "data:image/jpeg;base64,${msg.imageBase64}")
                                    put("detail", "low")
                                })
                            })
                        })
                    })
                } else {
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
        }
    }

    /**
     * Builds a system prompt that incorporates the user's cooking preferences
     * (dietary restrictions, skill level, favorite cuisines, allergies) so the
     * assistant can tailor recipe suggestions and advice to each user.
     */
    private fun buildPersonalizedSystemPrompt(): String {
        val prefs = UserManager.preferences.value
        val profile = UserManager.profile.value

        val personalization = buildString {
            appendLine("USER PROFILE:")
            appendLine("- Skill level: ${prefs.skillLevel}")
            if (prefs.dietaryTags.isNotEmpty()) {
                appendLine("- Dietary restrictions: ${prefs.dietaryTags.joinToString(", ")}")
            }
            if (prefs.favoriteCuisines.isNotEmpty()) {
                appendLine("- Favorite cuisines: ${prefs.favoriteCuisines.joinToString(", ")}")
            }
            if (prefs.allergies.isNotEmpty()) {
                appendLine("- Allergies (IMPORTANT — always avoid these): ${prefs.allergies.joinToString(", ")}")
            }
            if (profile.displayName.isNotBlank() && profile.displayName != "Home Chef") {
                appendLine("- User name: ${profile.displayName}")
            }
        }

        return """$SYSTEM_PROMPT

ADAPT TO USER PREFERENCES:
$personalization
Always tailor your suggestions to the user's skill level and dietary restrictions. If the user has allergies, never recommend recipes containing those allergens and proactively warn about cross-contamination. Favor their preferred cuisines when suggesting recipes, but feel free to introduce new ones too. Match the depth of your explanations to their skill level — simpler for beginners, more technical for advanced cooks."""
    }

    /**
     * Detects if the user's message is a nutrition-related query and fetches
     * relevant data from USDA FoodData Central.
     */
    private suspend fun fetchNutritionContext(userMessage: String): String? {
        val lower = userMessage.lowercase()

        val nutritionKeywords = listOf(
            "calories", "protein", "carbs", "carbohydrate", "fat", "fiber",
            "sugar", "sodium", "nutrition", "nutritional", "healthier", "healthy",
            "vitamin", "iron", "calcium", "potassium", "cholesterol",
            "macros", "macro", "nutrient", "compare", "vs", "versus",
            "how many calories", "nutritional value", "nutrition facts",
        )

        val isNutritionQuery = nutritionKeywords.any { lower.contains(it) }
        if (!isNutritionQuery) return null

        val foodNames = extractFoodNames(userMessage)
        if (foodNames.isEmpty()) return null

        val nutritionMap = mutableMapOf<String, UsdaFoodService.FoodNutrition>()
        for (food in foodNames) {
            val result = usda.getFoodNutrition(food)
            result.onSuccess { nutrition -> nutritionMap[food] = nutrition }
        }

        if (nutritionMap.isEmpty()) return null

        return buildString {
            appendLine("=== USDA FoodData Central Nutrition Data ===")
            appendLine()
            for ((name, nutrition) in nutritionMap) {
                appendLine(usda.formatNutritionForPrompt(name, nutrition))
                appendLine()
            }
            appendLine("Use this factual data to answer the user's question. Explain the differences in simple language.")
        }
    }

    private fun extractFoodNames(message: String): List<String> {
        val lower = message.lowercase()

        val comparisonPatterns = listOf(" healthier than ", " vs ", " versus ", " or ", " compared to ", " compare ")
        for (pattern in comparisonPatterns) {
            if (lower.contains(pattern)) {
                val parts = lower.split(pattern).filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    val food1 = cleanFoodName(parts[0])
                    val food2 = cleanFoodName(parts[1])
                    if (food1.isNotEmpty() && food2.isNotEmpty()) {
                        return listOf(food1, food2)
                    }
                }
            }
        }

        val prefixPatterns = listOf("calories in ", "nutrition of ", "nutrition facts for ", "nutrition facts of ", "nutritional value of ", "macros for ", "macros in ")
        for (prefix in prefixPatterns) {
            if (lower.contains(prefix)) {
                val after = lower.substringAfter(prefix).trim()
                val foodName = cleanFoodName(after)
                if (foodName.isNotEmpty()) return listOf(foodName)
            }
        }

        if (lower.contains("how many calories")) {
            val after = lower.substringAfter("calories").removePrefix(" in ").removePrefix(" are in ").trim()
            val foodName = cleanFoodName(after)
            if (foodName.isNotEmpty()) return listOf(foodName)
        }

        return emptyList()
    }

    private fun cleanFoodName(raw: String): String {
        return raw
            .replace(Regex("[?#.,!]"), "")
            .replace(Regex("\\b(a|an|the|is|are|which|what|has|have|more|less|better|worse)\\b"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { it.isNotBlank() && it.length > 1 } ?: ""
    }

    fun close() {
        gateway.close()
        usda.close()
        backend.close()
    }

    companion object {
        const val SYSTEM_PROMPT = """You are CookScope, a warm and expert culinary assistant. You help home cooks with:
- Recipe suggestions based on ingredients they have
- Cooking techniques and tips
- Ingredient substitutions
- Meal planning ideas
- Dietary restriction guidance
- Flavor pairings and seasoning advice
- Nutrition information and food comparisons

Keep responses concise, practical, and encouraging. Use brief paragraphs or short lists. When suggesting recipes, include key ingredients and a quick method overview. Always be food-safety conscious. If asked about non-food topics, gently redirect to cooking.

When a photo is attached, only respond if it shows food, a dish, ingredients, a recipe, or something cooking-related. If it does not, politely decline and ask the user to share a cooking-related photo.

When nutrition data from USDA FoodData Central is provided, use it to give accurate, specific nutritional comparisons. Present the data in a clear, easy-to-understand way for home cooks."""
    }
}
