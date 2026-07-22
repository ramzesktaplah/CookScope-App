package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Tracks AI recipe generation usage with a sliding window.
 *
 *  - Free plan:    2 generations / 30-minute window
 *  - Premium plan: 25 generations / 24-hour window
 *
 * The window only resets when the user has used all generations AND the
 * window has passed (same refill-at-zero pattern as [ScanUsageManager]).
 */
object RecipeGenerationUsageManager {

    private const val PREFS_NAME = "cookscope_recipe_gen_usage"
    private const val KEY_USED = "used_in_window"
    private const val KEY_WINDOW_START = "window_start_millis"

    const val FREE_LIMIT = 2
    const val PREMIUM_LIMIT = 25

    /** Free window = 30 min; Premium window = 24 hrs. */
    private val FREE_WINDOW_MILLIS = 30L * 60_000L
    private val PREMIUM_WINDOW_MILLIS = 24L * 60L * 60_000L

    private lateinit var prefs: SharedPreferences

    private val _remaining = MutableStateFlow(FREE_LIMIT)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    private val _used = MutableStateFlow(0)
    val used: StateFlow<Int> = _used.asStateFlow()

    private val _isRateLimited = MutableStateFlow(false)
    val isRateLimited: StateFlow<Boolean> = _isRateLimited.asStateFlow()

    private val _nextResetMillis = MutableStateFlow<Long?>(null)
    val nextResetMillis: StateFlow<Long?> = _nextResetMillis.asStateFlow()

    private val _windowStart = MutableStateFlow(0L)

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _used.value = prefs.getInt(KEY_USED, 0)
        _windowStart.value = prefs.getLong(KEY_WINDOW_START, 0L)
        refreshState()
    }

    fun currentLimit(): Int =
        if (SubscriptionManager.isPremium.value) PREMIUM_LIMIT else FREE_LIMIT

    private fun currentWindowMillis(): Long =
        if (SubscriptionManager.isPremium.value) PREMIUM_WINDOW_MILLIS else FREE_WINDOW_MILLIS

    fun canGenerate(): Boolean {
        refreshState()
        return _used.value < currentLimit()
    }

    fun increment(): Int {
        val now = System.currentTimeMillis()
        val limit = currentLimit()
        if (_used.value >= limit && now >= _windowStart.value + currentWindowMillis()) {
            resetWindow(now)
        }
        _used.value += 1
        if (_windowStart.value == 0L) {
            _windowStart.value = now
        }
        persist()
        refreshState()
        return _remaining.value
    }

    fun refreshState() {
        val now = System.currentTimeMillis()
        val windowEnd = _windowStart.value + currentWindowMillis()
        val limit = currentLimit()

        val atLimit = _used.value >= limit
        _isRateLimited.value = atLimit && now < windowEnd
        _nextResetMillis.value = if (atLimit && now < windowEnd) windowEnd else null
        _remaining.value = if (_isRateLimited.value) 0 else max(limit - _used.value, 0)
    }

    fun reset() {
        resetWindow(System.currentTimeMillis())
    }

    private fun resetWindow(now: Long) {
        _used.value = 0
        _windowStart.value = now
        persist()
        refreshState()
    }

    private fun persist() {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putInt(KEY_USED, _used.value)
            .putLong(KEY_WINDOW_START, _windowStart.value)
            .apply()
    }

    fun formatTimeRemaining(): String {
        val reset = _nextResetMillis.value ?: return "0m"
        val millis = max(reset - System.currentTimeMillis(), 0L)
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
