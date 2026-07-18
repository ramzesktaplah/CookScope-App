package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Tracks monthly dish-scan usage for rate limiting the "Scan Your Dish" feature.
 *
 *  - Free plan:   2 scans / month
 *  - Premium:    35 scans / month
 *
 * Month is keyed as YYYY-MM. When the month rolls over, the counter resets.
 */
object ScanUsageManager {

    private const val PREFS_NAME = "cookscope_scan_usage"
    private const val KEY_MONTH = "usage_month"
    private const val KEY_COUNT = "usage_count"

    const val FREE_LIMIT = 2
    const val PREMIUM_LIMIT = 35

    private lateinit var prefs: SharedPreferences

    private val _remaining = MutableStateFlow(0)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    private val _used = MutableStateFlow(0)
    val used: StateFlow<Int> = _used.asStateFlow()

    private var currentMonth: String = ""

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentMonth = currentMonthKey()
        val savedMonth = prefs.getString(KEY_MONTH, null)
        val savedCount = prefs.getInt(KEY_COUNT, 0)
        if (savedMonth != currentMonth) {
            prefs.edit()
                .putString(KEY_MONTH, currentMonth)
                .putInt(KEY_COUNT, 0)
                .apply()
            _used.value = 0
        } else {
            _used.value = savedCount
        }
        refreshRemaining()
    }

    /** Update remaining based on the user's current premium status. */
    fun refreshRemaining() {
        val limit = if (SubscriptionManager.isPremium.value) PREMIUM_LIMIT else FREE_LIMIT
        _remaining.value = (limit - _used.value).coerceAtLeast(0)
    }

    /** Returns true if the user can still perform a scan this month. */
    fun canScan(): Boolean {
        val limit = if (SubscriptionManager.isPremium.value) PREMIUM_LIMIT else FREE_LIMIT
        return _used.value < limit
    }

    /** Increment the counter after a scan is kicked off. Returns remaining. */
    fun increment(): Int {
        _used.value += 1
        prefs.edit().putInt(KEY_COUNT, _used.value).apply()
        refreshRemaining()
        return _remaining.value
    }

    /** Current monthly limit based on premium status. */
    fun currentLimit(): Int =
        if (SubscriptionManager.isPremium.value) PREMIUM_LIMIT else FREE_LIMIT

    private fun currentMonthKey(): String {
        val cal = Calendar.getInstance()
        @Suppress("MagicNumber")
        val y = cal.get(Calendar.YEAR)
        @Suppress("MagicNumber")
        val m = cal.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(y, m)
    }
}
