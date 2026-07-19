package cookscopeai.com.data

import cookscopeai.com.data.model.Recipe
import cookscopeai.com.data.model.CookStep

/**
 * Context-aware AI cooking assistant.
 *
 * Builds a system prompt that embeds the full recipe (title, cuisine,
 * ingredients, all steps) plus the user's current step and preferences,
 * so the assistant can answer mid-cooking questions like:
 *  - "Is my chicken cooked?"
 *  - "Can I reduce the heat?"
 *  - "What if I don't have garlic?"
 *
 * Uses the same Rork AI Gateway proxy as [ChatService] (so no API keys
 * ship in the APK) and the same tier-based model selection (DeepSeek for
 * free, GPT-4o for premium). Cooking assistance does NOT count against
 * the chatbot's monthly question limit — it's part of the guided cooking
 * experience, not the general chat tab.
 */
class CookingAssistantService {

    private val gateway = DeepSeekService()

    @kotlinx.serialization.Serializable
    data class ChatMessage(
        val role: String,
        val content: String,
    )

    /**
     * Send a cooking question to the assistant.
     *
     * @param recipe       The recipe being cooked.
     * @param currentStep  The step the user is currently on (0-indexed).
     * @param conversation Prior Q&A turns in this cooking session (role/content).
     *                     Empty on first question. Pass the running list so the
     *                     assistant has context of follow-up questions.
     * @param onReply      Called with the assistant's reply text on success.
     */
    suspend fun ask(
        recipe: Recipe,
        currentStep: Int,
        conversation: List<ChatMessage>,
        onReply: (String) -> Unit,
    ): Result<String> {
        val systemPrompt = buildCookingSystemPrompt(recipe, currentStep)
        val modelMessages = buildList {
            add(DeepSeekService.Message(role = "system", content = systemPrompt))
            conversation.forEach { msg ->
                add(DeepSeekService.Message(role = msg.role, content = msg.content))
            }
        }

        val model = if (SubscriptionManager.isPremium.value) {
            DeepSeekService.MODEL_CHAT_PREMIUM
        } else {
            DeepSeekService.MODEL_CHAT_FREE
        }

        return try {
            val result = gateway.chat(
                messages = modelMessages,
                temperature = 0.5,
                maxTokens = 1024,
                thinking = false,
                reasoningEffort = "low",
                model = model,
            )
            result.fold(
                onSuccess = { content ->
                    onReply(content)
                    Result.success(content)
                },
                onFailure = { Result.failure(it) },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        gateway.close()
    }

    /**
     * Build a cooking-context system prompt. Embeds:
     *  - Recipe title, cuisine, difficulty, cook time
     *  - Full ingredient list with amounts
     *  - All steps (so the assistant knows what came before and what's next)
     *  - The current step highlighted
     *  - User's allergies (safety-critical) and dietary tags
     *  - Guardrails: short, practical, food-safety-first answers
     */
    private fun buildCookingSystemPrompt(recipe: Recipe, currentStep: Int): String {
        val prefs = UserManager.preferences.value
        val ingredientsList = recipe.ingredients.joinToString("\n") { ing ->
            "  - ${ing.amount} ${ing.name}"
        }
        val stepsList = recipe.steps.joinToString("\n") { step ->
            val marker = if (step.index - 1 == currentStep) "▶ CURRENT STEP" else ""
            "  ${step.index}. ${step.title}${marker}\n     ${step.instruction}" +
                (step.tip?.let { "\n     Tip: $it" } ?: "")
        }

        val allergyLine = if (prefs.allergies.isNotEmpty()) {
            "\nALLERGIES (critical — never suggest substitutions containing these, and warn about cross-contact): ${prefs.allergies.joinToString(", ")}"
        } else ""

        val dietLine = if (prefs.dietaryTags.isNotEmpty()) {
            "\nDietary restrictions: ${prefs.dietaryTags.joinToString(", ")}"
        } else ""

        val skillLine = "\nUser skill level: ${prefs.skillLevel}"

        return """You are CookScope Cooking Assistant, guiding a home cook through a recipe in real time. They are actively cooking and may ask quick, practical questions mid-step.

RECIPE: ${recipe.title}
Cuisine: ${recipe.cuisine} · Difficulty: ${recipe.difficulty.label} · Total time: ${recipe.cookTimeMin} min · Servings: ${recipe.servings}

INGREDIENTS (what the cook has):
$ingredientsList

STEPS:
$stepsList
$allergyLine$dietLine$skillLine

HOW TO ANSWER:
- Be concise and practical. The cook is at the stove — answer in 1-3 short sentences, max ~80 words, unless they ask for detail.
- Focus on the CURRENT STEP unless they reference another step.
- For "is it cooked / done?" questions, give clear visual/temperature cues (e.g. internal temp, color, texture).
- For "can I reduce/increase heat?" questions, give a direct yes/no with the reasoning, then the adjusted time if relevant.
- For "what if I don't have X?" questions, suggest 1-2 common substitutes with amounts, checking against their allergies first.
- If a question involves their allergies, lead with a clear warning.
- If they go off-recipe, help them adapt without lecturing.
- Never invent temperatures or times that contradict food safety. When unsure about doneness, recommend using a thermometer and give the safe internal temperature.
- Don't recite the recipe back — they can see it. Answer their specific question."""
    }
}
