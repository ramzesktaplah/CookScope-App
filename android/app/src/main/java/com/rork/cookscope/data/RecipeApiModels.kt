package com.rork.cookscope.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response envelope for list endpoints from recipeapi.io.
 * Contains paginated data with links and metadata.
 */
@Serializable
data class RecipeListResponse(
    val data: List<ApiRecipe> = emptyList(),
    val links: ApiPaginationLinks? = null,
    val meta: ApiPaginationMeta? = null,
)

/**
 * Response envelope for single recipe endpoints (detail, random).
 */
@Serializable
data class RecipeDetailResponse(
    val data: ApiRecipe? = null,
    val meta: ApiPaginationMeta? = null,
)

/**
 * Response envelope for the ingredients listing endpoint.
 */
@Serializable
data class IngredientsListResponse(
    val data: List<ApiIngredientInfo> = emptyList(),
    val links: ApiPaginationLinks? = null,
    val meta: ApiPaginationMeta? = null,
)

/**
 * Recipe object returned by recipeapi.io.
 * Both list and detail endpoints return this same full structure.
 */
@Serializable
data class ApiRecipe(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val difficulty: String = "",
    @SerialName("meal_type")
    val mealType: String = "",
    val cuisine: String = "",
    @SerialName("dietary_tags")
    val dietaryTags: List<String> = emptyList(),
    val servings: Int = 4,
    @SerialName("prep_time")
    val prepTime: Int = 0,
    @SerialName("cook_time")
    val cookTime: Int = 0,
    @SerialName("calories_per_serving")
    val caloriesPerServing: Int = 0,
    val protein: Int = 0,
    val instructions: List<String> = emptyList(),
    val ingredients: List<ApiIngredient> = emptyList(),
)

/**
 * Ingredient object within a recipe.
 */
@Serializable
data class ApiIngredient(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val quantity: Float? = null,
    val unit: String = "",
    val optional: Boolean = false,
)

/**
 * Ingredient info from the ingredients listing endpoint.
 */
@Serializable
data class ApiIngredientInfo(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
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
