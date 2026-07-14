package com.rork.cookscope.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.headers
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * AI chat service that talks to the Rork proxy (Vercel AI Gateway).
 * Uses OpenAI-compatible chat completions endpoint with gpt-4o-mini.
 */
class ChatService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val toolkitUrl = "https://toolkit.rork.com"
    private val secretKey = "rork_sk_p6wfm99bye40dybmfr6puviqlgt8uqcp"

    @Serializable
    data class ChatMessage(
        val role: String,
        val content: String,
    )

    /**
     * Sends conversation history to the AI and returns the assistant's reply.
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit
    ): Result<String> {
        return try {
            val messagesArray = buildJsonArray {
                // System prompt — culinary assistant persona
                add(buildJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }

            val requestBody = buildJsonObject {
                put("model", "openai/gpt-4o-mini")
                put("messages", messagesArray)
                put("stream", false)
                put("temperature", 0.7)
                put("max_tokens", 800)
            }

            val response: HttpResponse = client.post("$toolkitUrl/v2/vercel/v1/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $secretKey")
                    append(HttpHeaders.ContentType, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            if (response.status.value !in 200..299) {
                val errorBody = response.bodyAsText()
                return Result.failure(Exception("AI request failed: ${response.status.value}"))
            }

            val responseText = response.bodyAsText()
            val json = Json.parseToJsonElement(responseText).jsonObject
            val content = json["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.contentOrNull ?: ""

            onToken(content)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SYSTEM_PROMPT = """You are CookScope, a warm and expert culinary assistant. You help home cooks with:
- Recipe suggestions based on ingredients they have
- Cooking techniques and tips
- Ingredient substitutions
- Meal planning ideas
- Dietary restriction guidance
- Flavor pairings and seasoning advice

Keep responses concise, practical, and encouraging. Use brief paragraphs or short lists. When suggesting recipes, include key ingredients and a quick method overview. Always be food-safety conscious. If asked about non-food topics, gently redirect to cooking."""
    }
}
