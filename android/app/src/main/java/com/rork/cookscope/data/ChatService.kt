package com.rork.cookscope.data

import kotlinx.serialization.Serializable

/**
 * AI chat service powered by DeepSeek-v4-flash via NVIDIA integrate API.
 * Uses USDA FoodData Central for factual nutrition data to ground answers.
 *
 * Flow:
 * 1. User asks a question
 * 2. If the question involves nutrition facts (calories, protein, comparison),
 *    USDA FoodData Central is queried first for reliable data
 * 3. DeepSeek formats the final answer using the USDA data as context
 */
class ChatService {

    private val deepSeek = DeepSeekService()
    private val usda = UsdaFoodService()

    @Serializable
    data class ChatMessage(
        val role: String,
        val content: String,
    )

    /**
     * Sends conversation history to the AI and returns the assistant's reply.
     * Automatically detects nutrition-related queries and enriches them with
     * USDA FoodData Central data before sending to DeepSeek.
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
    ): Result<String> {
        return try {
            val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""

            // Check if this is a nutrition-related query
            val nutritionContext = fetchNutritionContext(lastUserMessage)

            // Build the system prompt with optional USDA context
            val systemPrompt = if (nutritionContext != null) {
                "$SYSTEM_PROMPT\n\nYou have access to factual nutrition data from the USDA FoodData Central database. Use the provided data to ground your answer in real nutritional information.\n\n$nutritionContext"
            } else {
                SYSTEM_PROMPT
            }

            val deepSeekMessages = buildList {
                add(DeepSeekService.Message(role = "system", content = systemPrompt))
                messages.forEach { msg ->
                    add(DeepSeekService.Message(role = msg.role, content = msg.content))
                }
            }

            val result = deepSeek.chat(
                messages = deepSeekMessages,
                temperature = 0.7,
                maxTokens = 4096,
                thinking = false,
                reasoningEffort = "medium",
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
     * Detects if the user's message is a nutrition-related query and fetches
     * relevant data from USDA FoodData Central.
     *
     * Returns formatted nutrition context string, or null if not a nutrition query.
     */
    private suspend fun fetchNutritionContext(userMessage: String): String? {
        val lower = userMessage.lowercase()

        // Keywords that indicate a nutrition question
        val nutritionKeywords = listOf(
            "calories", "protein", "carbs", "carbohydrate", "fat", "fiber",
            "sugar", "sodium", "nutrition", "nutritional", "healthier", "healthy",
            "vitamin", "iron", "calcium", "potassium", "cholesterol",
            "macros", "macro", "nutrient", "compare", "vs", "versus",
            "how many calories", "nutritional value", "nutrition facts",
        )

        val isNutritionQuery = nutritionKeywords.any { lower.contains(it) }
        if (!isNutritionQuery) return null

        // Try to extract food names from the query
        val foodNames = extractFoodNames(userMessage)
        if (foodNames.isEmpty()) return null

        // Fetch nutrition data for each food
        val nutritionMap = mutableMapOf<String, UsdaFoodService.FoodNutrition>()
        for (food in foodNames) {
            val result = usda.getFoodNutrition(food)
            result.onSuccess { nutrition -> nutritionMap[food] = nutrition }
        }

        if (nutritionMap.isEmpty()) return null

        // Format the nutrition data for the DeepSeek prompt
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

    /**
     * Extracts food names from a user query.
     * Handles patterns like "is X healthier than Y", "nutrition of X", "calories in X and Y".
     */
    private fun extractFoodNames(message: String): List<String> {
        val lower = message.lowercase()

        // Pattern: "is X healthier than Y" / "X vs Y" / "X versus Y" / "X or Y"
        val comparisonPatterns = listOf(" healthier than ", " vs ", " versus ", " or ", " compared to ", " compare ")
        for (pattern in comparisonPatterns) {
            if (lower.contains(pattern)) {
                val parts = lower.split(pattern).filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    // Clean up the food names — remove question words and punctuation
                    val food1 = cleanFoodName(parts[0])
                    val food2 = cleanFoodName(parts[1])
                    if (food1.isNotEmpty() && food2.isNotEmpty()) {
                        return listOf(food1, food2)
                    }
                }
            }
        }

        // Pattern: "calories in X" / "nutrition of X" / "nutrition facts for X"
        val prefixPatterns = listOf("calories in ", "nutrition of ", "nutrition facts for ", "nutrition facts of ", "nutritional value of ", "macros for ", "macros in ")
        for (prefix in prefixPatterns) {
            if (lower.contains(prefix)) {
                val after = lower.substringAfter(prefix).trim()
                val foodName = cleanFoodName(after)
                if (foodName.isNotEmpty()) return listOf(foodName)
            }
        }

        // Pattern: "how many calories in X"
        if (lower.contains("how many calories")) {
            val after = lower.substringAfter("calories").removePrefix(" in ").removePrefix(" are in ").trim()
            val foodName = cleanFoodName(after)
            if (foodName.isNotEmpty()) return listOf(foodName)
        }

        return emptyList()
    }

    /**
     * Cleans a raw food name from a user query — removes question marks,
     * trailing punctuation, and common non-food words.
     */
    private fun cleanFoodName(raw: String): String {
        return raw
            .replace(Regex("[?#.,!]"), "")
            .replace(Regex("\\b(a|an|the|is|are|which|what|has|have|more|less|better|worse)\\b"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { it.isNotBlank() && it.length > 1 } ?: ""
    }

    fun close() {
        deepSeek.close()
        usda.close()
    }

    companion object {
        const val SYSTEM_PROMPT = """You are CookScope, a warm and expert culinary assistant powered by DeepSeek. You help home cooks with:
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
