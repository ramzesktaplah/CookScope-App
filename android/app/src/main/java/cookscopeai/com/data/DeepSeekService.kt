package cookscopeai.com.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/**
 * AI completion service for CookScope.
 *
 * All requests are proxied through the CookScope backend (Supabase Edge
 * Functions) so the Rork toolkit secret never ships in the APK. The backend
 * validates the user's Supabase session and forwards the request to the
 * Rork AI Gateway.
 *
 * This client is model-agnostic: callers pass a model ID (e.g.
 * `openai/gpt-4o-mini` for chat, or `deepseek/deepseek-v4-flash` for recipe
 * generation and dish scan).
 */
class DeepSeekService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val backend = CookScopeBackendService()

    @Serializable
    data class Message(
        val role: String,
        val content: String,
    )

    data class CompletionResult(
        val content: String,
        val reasoning: String?,
    )

    /**
     * Sends a chat completion request and returns both the content and reasoning.
     *
     * @param messages Conversation messages (system + user/assistant turns)
     * @param temperature Sampling temperature (default 0.7)
     * @param maxTokens Maximum tokens to generate
     * @param thinking Enable thinking/reasoning mode for complex tasks
     * @param reasoningEffort Reasoning depth: "low", "medium", or "high"
     * @param model Model ID to use on the backend
     */
    suspend fun complete(
        messages: List<Message>,
        temperature: Double = 0.7,
        maxTokens: Int = 4096,
        thinking: Boolean = false,
        reasoningEffort: String = "medium",
        model: String = MODEL_DEFAULT,
    ): Result<CompletionResult> {
        return backend.aiChat(
            messages = messages.map { CookScopeBackendService.Message(it.role, it.content) },
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            thinking = thinking,
            reasoningEffort = reasoningEffort,
        ).map { CompletionResult(content = it.content, reasoning = it.reasoning) }
    }

    /**
     * Sends a chat completion request with raw JSON messages array.
     * Used for multimodal content (e.g. image_url in DishScanService).
     *
     * @param messagesJson Pre-built JsonArray of message objects
     * @return Result containing the content string
     */
    suspend fun completeWithRawMessages(
        messagesJson: JsonArray,
        temperature: Double = 0.7,
        maxTokens: Int = 4096,
        thinking: Boolean = false,
        reasoningEffort: String = "medium",
        model: String = MODEL_DEFAULT,
    ): Result<String> {
        // thinking/reasoningEffort are ignored by ai-scan (vision) — keep the
        // signature for source compatibility.
        return backend.aiScan(
            messagesJson = messagesJson,
            temperature = temperature,
            maxTokens = maxTokens,
        )
    }

    /**
     * Convenience method that returns only the content string.
     */
    suspend fun chat(
        messages: List<Message>,
        temperature: Double = 0.7,
        maxTokens: Int = 4096,
        thinking: Boolean = false,
        reasoningEffort: String = "medium",
        model: String = MODEL_DEFAULT,
    ): Result<String> {
        return complete(messages, temperature, maxTokens, thinking, reasoningEffort, model)
            .map { it.content }
    }

    fun close() {
        client.close()
        backend.close()
    }

    companion object {
        /** Default model for recipe generation and dish scan (DeepSeek). */
        const val MODEL_DEFAULT = "deepseek/deepseek-v4-flash"

        /** Free-tier chatbot model. */
        const val MODEL_CHAT_FREE = "openai/gpt-4o-mini"

        /** Premium-tier chatbot model. */
        const val MODEL_CHAT_PREMIUM = "openai/gpt-4o-mini"
    }
}
