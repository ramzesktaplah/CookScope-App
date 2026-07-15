package com.rork.cookscope.data

import com.rork.cookscope.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Shared NVIDIA DeepSeek API client.
 * Uses deepseek-ai/deepseek-v4-flash via integrate.api.nvidia.com.
 * Supports thinking/reasoning mode for higher-quality outputs.
 */
class DeepSeekService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "https://integrate.api.nvidia.com/v1"
    private val apiKey = BuildConfig.NVIDIA_API_KEY

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
     * Sends a chat completion request to DeepSeek and returns both the content and reasoning.
     *
     * @param messages Conversation messages (system + user/assistant turns)
     * @param temperature Sampling temperature (default 0.7)
     * @param maxTokens Maximum tokens to generate
     * @param thinking Enable thinking/reasoning mode for complex tasks
     * @param reasoningEffort Reasoning depth: "low", "medium", or "high"
     */
    suspend fun complete(
        messages: List<Message>,
        temperature: Double = 0.7,
        maxTokens: Int = 4096,
        thinking: Boolean = false,
        reasoningEffort: String = "medium",
    ): Result<CompletionResult> {
        return try {
            val messagesArray = buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }

            val requestBody = buildJsonObject {
                put("model", MODEL)
                put("messages", messagesArray)
                put("temperature", temperature)
                put("top_p", 0.95)
                put("max_tokens", maxTokens)
                put("stream", false)
                // NVIDIA-specific: enables thinking mode for DeepSeek
                put("chat_template_kwargs", buildJsonObject {
                    put("thinking", thinking)
                    put("reasoning_effort", reasoningEffort)
                })
            }

            val response: HttpResponse = client.post("$baseUrl/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            if (response.status.value !in 200..299) {
                val errorBody = response.bodyAsText()
                return Result.failure(Exception("DeepSeek request failed: ${response.status.value}"))
            }

            val responseText = response.bodyAsText()
            val json = Json.parseToJsonElement(responseText).jsonObject
            val message = json["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject

            val content = message?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
            val reasoning = message?.get("reasoning")?.jsonPrimitive?.contentOrNull
                ?: message?.get("reasoning_content")?.jsonPrimitive?.contentOrNull

            Result.success(CompletionResult(content = content, reasoning = reasoning))
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    ): Result<String> {
        return try {
            val requestBody = buildJsonObject {
                put("model", MODEL)
                put("messages", messagesJson)
                put("temperature", temperature)
                put("top_p", 0.95)
                put("max_tokens", maxTokens)
                put("stream", false)
                put("chat_template_kwargs", buildJsonObject {
                    put("thinking", thinking)
                    put("reasoning_effort", reasoningEffort)
                })
            }

            val response: HttpResponse = client.post("$baseUrl/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            if (response.status.value !in 200..299) {
                val errorBody = response.bodyAsText()
                return Result.failure(Exception("DeepSeek request failed: ${response.status.value}"))
            }

            val responseText = response.bodyAsText()
            val json = Json.parseToJsonElement(responseText).jsonObject
            val content = json["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.contentOrNull ?: ""

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    ): Result<String> {
        return complete(messages, temperature, maxTokens, thinking, reasoningEffort)
            .map { it.content }
    }

    fun close() {
        client.close()
    }

    companion object {
        private const val MODEL = "deepseek-ai/deepseek-v4-flash"
    }
}
