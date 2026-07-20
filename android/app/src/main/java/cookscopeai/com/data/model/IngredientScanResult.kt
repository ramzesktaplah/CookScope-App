package cookscopeai.com.data.model

import kotlinx.serialization.Serializable

/**
 * Result of an AI ingredient scan: a list of detected raw food items plus
 * optional notes about ripeness/quantity.
 */
@Serializable
data class IngredientScanResult(
    val ingredients: List<DetectedIngredient> = emptyList(),
    val notes: String = "",
)

/**
 * A single ingredient detected from a photo by AI vision.
 *
 * @param name Ingredient name (e.g. "tomato").
 * @param category Coarse category: PRODUCE, PROTEIN, DAIRY, GRAIN, PANTRY, SPICE.
 * @param confidence Model confidence in the detection, 0..1.
 */
@Serializable
data class DetectedIngredient(
    val name: String = "",
    val category: String = "",
    val confidence: Float = 0f,
)
