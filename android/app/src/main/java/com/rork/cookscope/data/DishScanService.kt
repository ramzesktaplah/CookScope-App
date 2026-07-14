package com.rork.cookscope.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
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
import com.rork.cookscope.data.model.DishAnalysis

/**
 * AI vision service that analyzes a dish photo and returns structured nutrition info.
 * Uses OpenAI gpt-4o-mini (vision-capable) via the Rork proxy.
 */
class DishScanService {

    private val client = HttpClient(Android)

    private val toolkitUrl = "https://toolkit.rork.com"
    private val secretKey = "rork_sk_p6wfm99bye40dybmfr6puviqlgt8uqcp"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Analyzes a dish photo (base64 JPEG, no data: prefix) and returns structured analysis.
     */
    suspend fun analyzeDish(base64Image: String): Result<DishAnalysis> {
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

            val requestBody = buildJsonObject {
                put("model", "openai/gpt-4o-mini")
                put("messages", messagesArray)
                put("stream", false)
                put("temperature", 0.3)
                put("max_tokens", 1200)
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
                return Result.failure(Exception("Scan failed: ${response.status.value}"))
            }

            val responseText = response.bodyAsText()
            val responseJson = Json.parseToJsonElement(responseText).jsonObject
            val content = responseJson["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")
                ?.jsonPrimitive?.contentOrNull ?: ""

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
        private const val SYSTEM_PROMPT = """You are CookScope's AI dish analyzer. You analyze food photos and return precise nutritional information as JSON. Always respond with ONLY valid JSON, no markdown, no explanation."""

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
