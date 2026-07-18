package cookscopeai.com.data

import kotlinx.serialization.Serializable

/**
 * AI chat service powered by the Rork AI Gateway proxy.
 *
 * Model selection is tier-based:
 *  - Free plan:   DeepSeek V4 Flash (deepseek/deepseek-v4-flash) — 5 msgs/month
 *  - Premium:     GPT-4o (openai/gpt-4o) — 100 msgs/month
 *
 * The system prompt is personalized using the user's saved cooking preferences
 * (dietary tags, skill level, favorite cuisines, allergies) so the assistant
 * adapts its suggestions to each user.
 *
 * Nutrition-related queries are enriched with USDA FoodData Central data before
 * being sent to the model.
 */
class ChatService {

    private val gateway = DeepSeekService()
    private val usda = UsdaFoodService()

    @Serializable
    data class ChatMessage(
        val role: String,
        val content: String,
    )

    /**
     * Sends conversation history to the AI and returns the assistant's reply.
     * Automatically detects nutrition-related queries and enriches them with
     * USDA FoodData Central data before sending to the model.
     *
     * @param isPremium when true, uses GPT-4o; when false, uses DeepSeek V4 Flash.
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
        isPremium: Boolean = SubscriptionManager.isPremium.value,
    ): Result<String> {
        return try {
            val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""

            // Check if this is a nutrition-related query
            val nutritionContext = fetchNutritionContext(lastUserMessage)

            // Build a personalized system prompt from the user's preferences
            val basePrompt = buildPersonalizedSystemPrompt()
            val systemPrompt = if (nutritionContext != null) {
                "$basePrompt\n\nYou have access to factual nutrition data from the USDA FoodData Central database. Use the provided data to ground your answer in real nutritional information.\n\n$nutritionContext"
            } else {
                basePrompt
            }

            val modelMessages = buildList {
                add(DeepSeekService.Message(role = "system", content = systemPrompt))
                messages.forEach { msg ->
                    add(DeepSeekService.Message(role = msg.role, content = msg.content))
                }
            }

            val model = if (isPremium) {
                DeepSeekService.MODEL_CHAT_PREMIUM
            } else {
                DeepSeekService.MODEL_CHAT_FREE
            }

            val result = gateway.chat(
                messages = modelMessages,
                temperature = 0.7,
                maxTokens = 4096,
                thinking = false,
                reasoningEffort = "medium",
                model = model,
            )

            result.fold(
                onSuccess = { content ->
                    onToken(content)
                    Result.success(content)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
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

When nutrition data from USDA FoodData Central is provided, use it to give accurate, specific nutritional comparisons. Present the data in a clear, easy-to-understand way for home cooks."""
    }
}
