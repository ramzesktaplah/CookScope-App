package com.rork.cookscope.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DishAnalysis(
    val dishName: String = "",
    val description: String = "",
    val ingredients: List<AnalyzedIngredient> = emptyList(),
    val calories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
    val dietaryTags: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val healthScore: Int = 0,
    val healthNotes: String = "",
    val portionSize: String = "",
    val mealType: String = "",
)

@Serializable
data class AnalyzedIngredient(
    val name: String = "",
    val amount: String = "",
)
