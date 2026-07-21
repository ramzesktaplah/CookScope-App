package cookscopeai.com.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for scheduled CookScope notification alarms.
 *
 * Handles three actions:
 *  - `cookscopeai.com.DAILY_TRENDING`   → daily trending recipe notification
 *  - `cookscopeai.com.WEEKLY_UPDATE`    → weekly recipe-update notification
 *  - `cookscopeai.com.RATE_LIMIT_RESET` → one-shot "assistant is waiting" reminder
 *
 * Tapping the posted notification launches [cookscopeai.com.MainActivity].
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        // Make sure the service has an app context to post with.
        NotificationService.initialize(context)
        NotificationService.postForAction(action)
    }
}
