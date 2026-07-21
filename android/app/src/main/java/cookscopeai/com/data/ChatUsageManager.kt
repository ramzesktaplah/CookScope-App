package cookscopeai.com.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Tracks AI chat usage with a sliding 3.5-hour window.
 *
 *  - Every user gets 5 questions per 3.5-hour window.
 *  - The window only resets when the user has used all 5 questions and the
 *    3.5-hour window has passed. Users with 1-4 questions left keep their
 *    count across the window boundary; they do not get refilled to 5 until
 *    they hit zero and wait.
 *
 *  This window is used for both the chatbot and the premium cooking assistant.
 */
object ChatUsageManager {

    private const val PREFS_NAME = "cookscope_chat_usage"
    private const val KEY_USED = "used_in_window"
    private const val KEY_WINDOW_START = "window_start_millis"

    private const val WINDOW_LIMIT = 5
    private const val WINDOW_HOURS = 3L
    private const val WINDOW_MINUTES = 30L
    private val WINDOW_DURATION_MILLIS = ((WINDOW_HOURS * 60L) + WINDOW_MINUTES) * 60_000L

    private lateinit var prefs: SharedPreferences

    private val _remaining = MutableStateFlow(WINDOW_LIMIT)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    private val _used = MutableStateFlow(0)
    val used: StateFlow<Int> = _used.asStateFlow()

    private val _rateLimited = MutableStateFlow(false)
    val isRateLimited: StateFlow<Boolean> = _rateLimited.asStateFlow()

    private val _nextResetMillis = MutableStateFlow<Long?>(null)
    val nextResetMillis: StateFlow<Long?> = _nextResetMillis.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _used.value = prefs.getInt(KEY_USED, 0)
        _windowStart.value = prefs.getLong(KEY_WINDOW_START, 0L)
        refreshState()
    }

    private val _windowStart = MutableStateFlow(0L)

    /** True if the user can send a question right now. */
    fun canSendMessage(): Boolean {
        refreshState()
        return _used.value < WINDOW_LIMIT || _nextResetMillis.value?.let { System.currentTimeMillis() >= it } == true
    }

    /**
     * Increment the counter after a successful send. If the user is currently at
     * the limit and the window has expired, reset first and then count the new
     * message as the first one of the new window.
     */
    fun increment(): Int {
        val now = System.currentTimeMillis()
        if (_used.value >= WINDOW_LIMIT && now >= _windowStart.value + WINDOW_DURATION_MILLIS) {
            resetWindow(now)
        }
        _used.value += 1
        if (_windowStart.value == 0L) {
            _windowStart.value = now
        }
        persist()
        refreshState()

        // Schedule a "your assistant is waiting" reminder when the user
        // crosses the limit. No-op if NotificationService isn't initialized.
        if (_rateLimited.value) {
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

        val atLimit = _used.value >= WINDOW_LIMIT
        _rateLimited.value = atLimit && now < windowEnd
        _nextResetMillis.value = if (atLimit && now < windowEnd) windowEnd else null
        _remaining.value = if (_rateLimited.value) 0 else max(WINDOW_LIMIT - _used.value, 0)
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

    fun currentLimit(): Int = WINDOW_LIMIT
}
