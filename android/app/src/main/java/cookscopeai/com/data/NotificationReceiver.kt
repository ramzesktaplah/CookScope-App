package cookscopeai.com.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for scheduled CookScope notification alarms.
 *
 * Handles repeating scheduled actions:
 *  - [NotificationService.ACTION_POPULAR_RECIPE]   → daily 8 AM popular recipe
 *  - [NotificationService.ACTION_MEALTIME_LUNCH]   → daily 12 PM lunch idea
 *  - [NotificationService.ACTION_MEALTIME_DINNER]  → daily 5:30 PM dinner idea
 *  - [NotificationService.ACTION_MEALTIME_WEEKEND] → Saturday 10 AM weekend idea
 *  - [NotificationService.ACTION_WEEKLY_TRENDING]  → weekly trending/seasonal
 *  - [NotificationService.ACTION_RATE_LIMIT_RESET] → one-shot assistant-ready reminder
 *  - [NotificationService.ACTION_INACTIVITY]       → daily inactivity re-engagement check
 *
 * Triggered (event-based) notifications are posted directly via
 * [NotificationService.showTriggeredNotification] from the calling screen,
 * so they do not flow through this receiver.
 *
 * On BOOT_COMPLETED, all repeating alarms are rescheduled.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        // Ensure the service has an app context to post with.
        NotificationService.initialize(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                // Reschedule all repeating alarms after a reboot.
                NotificationService.scheduleAllRepeating()
                NotificationService.scheduleInactivityCheck()
            }
            NotificationService.ACTION_TRIGGERED -> {
                NotificationService.postTriggeredFromIntent(intent)
            }
            else -> {
                NotificationService.postForAction(action)
            }
        }
    }
}
