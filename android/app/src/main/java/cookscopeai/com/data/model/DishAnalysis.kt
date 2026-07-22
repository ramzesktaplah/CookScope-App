package cookscopeai.com.data.model

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
    // ---- New expanded fields ----
    val primaryIngredients: List<AnalyzedIngredient> = emptyList(),
    val secondaryIngredients: List<AnalyzedIngredient> = emptyList(),
    val seasoningSpices: List<AnalyzedIngredient> = emptyList(),
    val cookingMethods: List<String> = emptyList(),
    val difficulty: String = "",
    val cookingTimeMin: Int = 0,
    val servingSize: String = "",
    val improvements: List<String> = emptyList(),
    val perfectPairings: List<String> = emptyList(),
    val healthyAlternatives: List<String> = emptyList(),
    val nutritionalAnalysis: String = "",
) {
    /** Letter grade derived from healthScore: 90+=A, 80+=B, 70+=C, 60+=D, else F. */
    val healthGrade: String
        get() = when {
            healthScore >= 90 -> "A"
            healthScore >= 80 -> "B"
            healthScore >= 70 -> "C"
            healthScore >= 60 -> "D"
            else -> "F"
        }
}

@Serializable
data class AnalyzedIngredient(
    val name: String = "",
    val amount: String = "",
)
