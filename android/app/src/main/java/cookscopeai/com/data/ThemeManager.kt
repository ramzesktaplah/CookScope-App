package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages dark/light theme preference with persistence.
 *
 * CookScope defaults to light mode so the warm cream/gold palette is visible.
 * Users can still toggle dark mode from Settings.
 */
object ThemeManager {

    private const val PREFS_NAME = "cookscope_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    private lateinit var prefs: SharedPreferences

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default to light mode (false) unless the user explicitly enabled dark mode.
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun toggle() {
        setDarkMode(!_isDarkMode.value)
    }
}
