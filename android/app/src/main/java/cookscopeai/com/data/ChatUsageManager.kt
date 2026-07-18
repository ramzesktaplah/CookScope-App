package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Tracks monthly AI chat usage for rate limiting.
 *
 *  - Free plan:   5 questions / month
 *  - Premium:   100 questions / month
 *
 * Month is keyed as YYYY-MM. When the month rolls over, the counter resets.
 */
object ChatUsageManager {

    private const val PREFS_NAME = "cookscope_chat_usage"
    private const val KEY_MONTH = "usage_month"
    private const val KEY_COUNT = "usage_count"

    const val FREE_LIMIT = 5
    const val PREMIUM_LIMIT = 100

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
            // New month — reset counter
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

    /** Returns true if the user can still send a message this month. */
    fun canSendMessage(): Boolean {
        val limit = if (SubscriptionManager.isPremium.value) PREMIUM_LIMIT else FREE_LIMIT
        return _used.value < limit
    }

    /** Increment the counter after a successful send. Returns remaining. */
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
