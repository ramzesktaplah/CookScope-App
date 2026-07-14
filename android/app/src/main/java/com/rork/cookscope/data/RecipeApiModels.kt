package com.rork.cookscope.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Response envelope for list endpoints from recipeapi.io.
 * Contains paginated data with links and metadata.
 */
@Serializable
data class RecipeListResponse(
    val data: List<ApiRecipeSummary> = emptyList(),
    val links: ApiPaginationLinks? = null,
    val meta: ApiPaginationMeta? = null,
)

/**
 * Response envelope for single recipe detail endpoint.
 */
@Serializable
data class RecipeDetailResponse(
    val data: ApiRecipeDetail? = null,
    val meta: ApiPaginationMeta? = null,
)

/**
 * Summary recipe object returned in list endpoints.
 * Fields are optional since the API may not always include all of them.
 */
@Serializable
data class ApiRecipeSummary(
    val id: JsonElement? = null,
    val name: String? = null,
    val description: String? = null,
    val cuisine: String? = null,
    val category: String? = null,
    val difficulty: String? = null,
    val tags: List<String> = emptyList(),
    val image: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val nutrition: ApiNutrition? = null,
    val meta: ApiRecipeMeta? = null,
    val dietary: ApiDietary? = null,
)

/**
 * Full recipe detail with ingredients, instructions, and nutrition.
 */
@Serializable
data class ApiRecipeDetail(
    val id: JsonElement? = null,
    val name: String? = null,
    val description: String? = null,
    val cuisine: String? = null,
    val category: String? = null,
    val difficulty: String? = null,
    val tags: List<String> = emptyList(),
    val image: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val nutrition: ApiNutrition? = null,
    val meta: ApiRecipeMeta? = null,
    val dietary: ApiDietary? = null,
    val ingredients: List<ApiIngredientGroup> = emptyList(),
    val instructions: List<ApiInstruction> = emptyList(),
    @SerialName("chef_notes")
    val chefNotes: List<String> = emptyList(),
    val equipment: List<ApiEquipment> = emptyList(),
    val storage: ApiStorage? = null,
    @SerialName("cultural_context")
    val culturalContext: String? = null,
    val troubleshooting: List<ApiTroubleshooting> = emptyList(),
)

@Serializable
data class ApiNutrition(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbohydrates: Int = 0,
    val fat: Int = 0,
    val fiber: Int = 0,
    val sugar: Int = 0,
    val sodium: Int = 0,
)

@Serializable
data class ApiRecipeMeta(
    @SerialName("active_time")
    val activeTime: String? = null,
    @SerialName("passive_time")
    val passiveTime: String? = null,
    @SerialName("total_time")
    val totalTime: String? = null,
    @SerialName("overnight_required")
    val overnightRequired: Boolean = false,
    val yields: String? = null,
    @SerialName("yield_count")
    val yieldCount: Int = 4,
    @SerialName("serving_size_g")
    val servingSizeG: Int? = null,
)

@Serializable
data class ApiDietary(
    val flags: List<String> = emptyList(),
    @SerialName("not_suitable_for")
    val notSuitableFor: List<String> = emptyList(),
)

@Serializable
data class ApiIngredientGroup(
    @SerialName("group_name")
    val groupName: String? = null,
    val items: List<ApiIngredient> = emptyList(),
)

@Serializable
data class ApiIngredient(
    val name: String = "",
    val quantity: Float? = null,
    val unit: String? = null,
    val preparation: String? = null,
    val notes: String? = null,
    val substitutions: List<String> = emptyList(),
)

@Serializable
data class ApiInstruction(
    val phase: String? = null,
    val step: Int = 0,
    val title: String? = null,
    val instruction: String = "",
    val duration: String? = null,
    val technique: String? = null,
    val tip: String? = null,
    @SerialName("doneness_cue")
    val donenessCue: String? = null,
)

@Serializable
data class ApiEquipment(
    val name: String = "",
    val required: Boolean = true,
    val alternative: String? = null,
)

@Serializable
data class ApiStorage(
    val refrigerator: String? = null,
    val freezer: String? = null,
    val reheating: String? = null,
)

@Serializable
data class ApiTroubleshooting(
    val problem: String = "",
    val solution: String = "",
)

@Serializable
data class ApiPaginationLinks(
    val first: String? = null,
    val last: String? = null,
    val prev: String? = null,
    val next: String? = null,
)

@Serializable
data class ApiPaginationMeta(
    @SerialName("current_page")
    val currentPage: Int = 1,
    @SerialName("last_page")
    val lastPage: Int = 1,
    @SerialName("per_page")
    val perPage: Int = 10,
    val total: Int = 0,
    val language: String = "en",
)
