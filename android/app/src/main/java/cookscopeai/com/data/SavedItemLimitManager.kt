package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the count of saved recipes and scanned dishes.
 *
 *  - Free plan:    50 recipes + 50 scanned dishes (tracked separately)
 *  - Premium plan: unlimited (Int.MAX_VALUE)
 *
 * Unlike the usage managers, this is a simple count cap — no time window.
 */
object SavedItemLimitManager {

    private const val PREFS_NAME = "cookscope_saved_limits"
    private const val KEY_DISH_COUNT = "saved_dish_count"

    const val FREE_RECIPE_LIMIT = 50
    const val FREE_DISH_LIMIT = 50

    private lateinit var prefs: SharedPreferences

    private val _dishCount = MutableStateFlow(0)
    val dishCount: StateFlow<Int> = _dishCount.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _dishCount.value = prefs.getInt(KEY_DISH_COUNT, 0)
    }

    fun recipeLimit(): Int =
        if (SubscriptionManager.isPremium.value) Int.MAX_VALUE else FREE_RECIPE_LIMIT

    fun dishLimit(): Int =
        if (SubscriptionManager.isPremium.value) Int.MAX_VALUE else FREE_DISH_LIMIT

    fun recipeCount(): Int = UserManager.savedRecipeIds.value.size

    fun canSaveRecipe(): Boolean = recipeCount() < recipeLimit()

    fun canSaveDish(): Boolean = _dishCount.value < dishLimit()

    fun incrementDishCount() {
        _dishCount.value += 1
        if (::prefs.isInitialized) {
            prefs.edit().putInt(KEY_DISH_COUNT, _dishCount.value).apply()
        }
    }

    fun reset() {
        _dishCount.value = 0
        if (::prefs.isInitialized) {
            prefs.edit().putInt(KEY_DISH_COUNT, 0).apply()
        }
    }
}
