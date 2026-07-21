package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Tracks dish- and ingredient-scan usage on a sliding 5-hour window.
 *
 *  - Free plan:   10 scans / 5-hour window
 *  - Premium:    50 scans / 5-hour window
 *
 * The window only resets when the user has used all scans AND the 5-hour
 * window has passed. Users with 1..(limit-1) scans left keep their count
 * across the window boundary; they do not get refilled until they hit zero
 * and wait the full 5 hours.
 */
object ScanUsageManager {

    private const val PREFS_NAME = "cookscope_scan_usage"
    private const val KEY_USED = "used_in_window"
    private const val KEY_WINDOW_START = "window_start_millis"

    const val FREE_LIMIT = 10
    const val PREMIUM_LIMIT = 50

    private const val WINDOW_HOURS = 5L
    private val WINDOW_DURATION_MILLIS = WINDOW_HOURS * 60L * 60_000L

    private lateinit var prefs: SharedPreferences

    private val _remaining = MutableStateFlow(0)
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

    /** True if the user can perform a scan right now. */
    fun canScan(): Boolean {
        refreshState()
        return _used.value < currentLimit()
    }

    /**
     * Increment the counter after a scan is kicked off. If the user is at the
     * limit and the window has expired, reset first, then count the new scan
     * as the first one of the new window.
     */
    fun increment(): Int {
        val now = System.currentTimeMillis()
        if (_used.value >= currentLimit() && now >= _windowStart.value + WINDOW_DURATION_MILLIS) {
            resetWindow(now)
        }
        _used.value += 1
        if (_windowStart.value == 0L) {
            _windowStart.value = now
        }
        persist()
        refreshState()

        // Schedule a "your scans have refilled" notification when the user
        // crosses the limit. No-op if NotificationService isn't initialized.
        if (_isRateLimited.value) {
            _nextResetMillis.value?.let { resetAt ->
                NotificationService.scheduleRateLimitReminder(resetAt)
            }
        }
        return _remaining.value
    }

    /** Recalculate remaining, rate-limit, and next reset from stored state. */
    fun refreshState() {
        val now = System.currentTimeMillis()
        val windowEnd = _windowStart.value + WINDOW_DURATION_MILLIS
        val limit = currentLimit()
        val atLimit = _used.value >= limit
        _isRateLimited.value = atLimit && now < windowEnd
        _nextResetMillis.value = if (atLimit && now < windowEnd) windowEnd else null
        _remaining.value = if (_isRateLimited.value) 0 else max(limit - _used.value, 0)
    }

    /** Alias kept for screens that already call refreshRemaining(). */
    fun refreshRemaining() = refreshState()

    /** Reset the counter entirely, e.g. after a successful subscription upgrade. */
    fun reset() {
        resetWindow(System.currentTimeMillis())
        NotificationService.cancelRateLimitReminder()
    }

    private fun resetWindow(now: Long) {
        _used.value = 0
        _windowStart.value = now
        persist()
        refreshState()
    }

    private fun persist() {
        prefs.edit()
            .putInt(KEY_USED, _used.value)
            .putLong(KEY_WINDOW_START, _windowStart.value)
            .apply()
    }

    /** Current window limit based on premium status. */
    fun currentLimit(): Int =
        if (SubscriptionManager.isPremium.value) PREMIUM_LIMIT else FREE_LIMIT

    /** Formats the remaining time until the next window as "Xh Ym" or "Xm". */
    fun formatTimeRemaining(): String {
        val reset = _nextResetMillis.value ?: return "0m"
        val millis = max(reset - System.currentTimeMillis(), 0L)
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
