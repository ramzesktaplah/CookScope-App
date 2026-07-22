package cookscopeai.com.data

import cookscopeai.com.Config
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * CookScope backend service — proxies all third-party API calls through
 * authenticated Supabase Edge Functions so no upstream secrets ever ship
 * in the Android APK.
 *
 * Every request attaches the caller's Supabase access token as
 * `Authorization: Bearer <jwt>`. The edge functions validate the session
 * and forward the request to the upstream provider (Rork AI Gateway or
 * recipeapi.io) using server-side secrets.
 *
 * Functions live at:
 *   {supabaseUrl}/functions/v1/{ai-chat|ai-scan|recipe-search|recipe-detail|recipe-random}
 */
class CookScopeBackendService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val functionsBase = "${Config.EXPO_PUBLIC_SUPABASE_URL}/functions/v1"

    /** Generic error returned by every backend function. */
    @Serializable
    private data class BackendError(val error: String? = null)

    /** Result of an AI chat call. */
    data class AiChatResult(
        val content: String,
        val reasoning: String?,
    )

    /**
     * Call the `ai-chat` function with a list of role/content messages.
     * Used for recipe generation and the chatbot.
     */
    suspend fun aiChat(
        messages: List<Message>,
        model: String,
        temperature: Double,
        maxTokens: Int,
        thinking: Boolean,
        reasoningEffort: String,
    ): Result<AiChatResult> {
        val body = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
            put("temperature", temperature)
            put("maxTokens", maxTokens)
            put("thinking", thinking)
            put("reasoningEffort", reasoningEffort)
        }
        return postJson("$functionsBase/ai-chat", body).mapResponse { raw ->
            val obj = Json.parseToJsonElement(raw).jsonObject
            val content = obj["content"]?.jsonPrimitive?.contentOrNull ?: ""
            val reasoning = obj["reasoning"]?.jsonPrimitive?.contentOrNull
            AiChatResult(content, reasoning)
        }
    }

    /**
     * Call the `ai-chat` function with a pre-built multimodal messages array.
     * Supports image attachments in user messages.
     */
    suspend fun aiChatMultimodal(
        messagesJson: JsonArray,
        model: String,
        temperature: Double,
        maxTokens: Int,
    ): Result<AiChatResult> {
        val body = buildJsonObject {
            put("model", model)
            put("messages", messagesJson)
            put("temperature", temperature)
            put("maxTokens", maxTokens)
            put("thinking", false)
            put("reasoningEffort", "low")
        }
        return postJson("$functionsBase/ai-chat", body).mapResponse { raw ->
            val obj = Json.parseToJsonElement(raw).jsonObject
            val content = obj["content"]?.jsonPrimitive?.contentOrNull ?: ""
            val reasoning = obj["reasoning"]?.jsonPrimitive?.contentOrNull
            AiChatResult(content, reasoning)
        }
    }

    /**
     * Call the `ai-scan` function with a pre-built multimodal messages array.
     * Returns the raw model content string (caller parses JSON out of it).
     */
    suspend fun aiScan(
        messagesJson: JsonArray,
        temperature: Double,
        maxTokens: Int,
    ): Result<String> {
        val body = buildJsonObject {
            put("messages", messagesJson)
            put("temperature", temperature)
            put("maxTokens", maxTokens)
        }
        return postJson("$functionsBase/ai-scan", body).mapResponse { raw ->
            val obj = Json.parseToJsonElement(raw).jsonObject
            obj["content"]?.jsonPrimitive?.contentOrNull ?: ""
        }
    }

    /**
     * Call the `recipe-search` function by text query.
     * Returns the raw recipeapi.io list payload as a string.
     */
    suspend fun recipeSearch(
        query: String,
        page: Int,
        perPage: Int,
    ): Result<String> {
        val body = buildJsonObject {
            put("query", query)
            put("page", page)
            put("perPage", perPage)
        }
        return postJson("$functionsBase/recipe-search", body).mapResponse { it }
    }

    /**
     * Call the `recipe-search` function by ingredient list.
     */
    suspend fun recipeSearchByIngredients(
        ingredients: List<String>,
        page: Int,
        perPage: Int,
    ): Result<String> {
        val body = buildJsonObject {
            put("ingredients", buildJsonArray {
                ingredients.forEach { add(it) }
            })
            put("page", page)
            put("perPage", perPage)
        }
        return postJson("$functionsBase/recipe-search", body).mapResponse { it }
    }

    /**
     * Call the `recipe-detail` function by recipe id.
     */
    suspend fun recipeDetail(id: String): Result<String> {
        val body = buildJsonObject {
            put("id", id)
        }
        return postJson("$functionsBase/recipe-detail", body).mapResponse { it }
    }

    /**
     * Call the `trending-fetch` function to get today's server-side trending
     * recipes. Returns the raw `{ data: [...] }` payload as a string.
     */
    suspend fun trendingFetch(): Result<String> {
        val body = buildJsonObject { put("count", 10) }
        return postJson("$functionsBase/trending-fetch", body).mapResponse { it }
    }

    /**
     * Call the `recipe-random` function with a count (1–10).
     * Returns the raw `{ data: [...] }` payload as a string.
     */
    suspend fun recipeRandom(count: Int): Result<String> {
        val body = buildJsonObject {
            put("count", count)
        }
        return postJson("$functionsBase/recipe-random", body).mapResponse { it }
    }

    // ---- Internals ----

    /** Simple message shape for `aiChat`. */
    data class Message(val role: String, val content: String)

    /**
     * Resolve the current Supabase access token from the live session status.
     * Returns null if the user is not authenticated.
     */
    private suspend fun currentAccessToken(): String? {
        return try {
            val status = SupabaseClient.client.auth.sessionStatus.value
            (status as? SessionStatus.Authenticated)?.session?.accessToken
        } catch (_: Exception) {
            null
        }
    }

    /**
     * POST a JSON object to a function endpoint with the bearer token.
     * On a non-2xx response, returns a safe user-facing error message.
     */
    private suspend fun postJson(url: String, payload: JsonObject): Result<String> {
        return try {
            val token = currentAccessToken()
                ?: return Result.failure(Exception("Authentication required. Please sign in again."))

            val response: HttpResponse = client.post(url) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val text = response.bodyAsText()
            if (response.status.value !in 200..299) {
                val safe = runCatching {
                    json.decodeFromString<BackendError>(text).error
                }.getOrNull()
                return Result.failure(Exception(safe ?: "Service unavailable. Please try again later."))
            }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(Exception("Network error. Check your connection and try again."))
        }
    }

    /** Map a raw response string into a typed result. */
    private inline fun <T> Result<String>.mapResponse(transform: (String) -> T): Result<T> =
        fold({ Result.success(transform(it)) }, { Result.failure(it) })

    fun close() {
        client.close()
    }
}
