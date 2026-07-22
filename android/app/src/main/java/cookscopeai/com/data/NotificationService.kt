package cookscopeai.com.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cookscopeai.com.MainActivity
import cookscopeai.com.R
import cookscopeai.com.data.model.Recipe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/**
 * Local scheduled push notifications for CookScope.
 *
 * Three notification channels:
 *  - `trending`        — daily trending recipe highlight
 *  - `rate_limit_reset` — "Your Assistant is waiting for you" when the
 *                        chat/scan 5-hour (or 3.5-hour) window refills
 *  - `recipe_updates`  — weekly recipe update nudge
 *
 * Alarms are scheduled via [AlarmManager]; the actual notifications are
 * posted by [NotificationReceiver]. POST_NOTIFICATIONS must be granted at
 * runtime (Android 13+) before notifications will display.
 */
object NotificationService {

    const val CHANNEL_TRENDING = "trending"
    const val CHANNEL_RATE_LIMIT = "rate_limit_reset"
    const val CHANNEL_RECIPE_UPDATES = "recipe_updates"

    private const val PREFS_NAME = "cookscope_notifications"
    private const val KEY_SCHEDULED = "repeating_scheduled"
    private const val KEY_RATE_LIMIT_PENDING = "rate_limit_pending"

    private const val REQUEST_DAILY = 1001
    private const val REQUEST_WEEKLY = 1002
    private const val REQUEST_RATE_LIMIT = 1003

    private const val ACTION_DAILY = "cookscopeai.com.DAILY_TRENDING"
    private const val ACTION_WEEKLY = "cookscopeai.com.WEEKLY_UPDATE"
    private const val ACTION_RATE_LIMIT = "cookscopeai.com.RATE_LIMIT_RESET"

    private const val DAILY_HOUR = 11
    private const val WEEKLY_DAY = Calendar.SATURDAY
    private const val INTERVAL_DAY_MILLIS = 24L * 60L * 60_000L
    private const val INTERVAL_WEEK_MILLIS = 7L * INTERVAL_DAY_MILLIS

    private lateinit var appContext: Context

    /**
     * Create notification channels and schedule the repeating daily/weekly
     * alarms on first launch. Safe to call multiple times — the repeating
     * alarms are only scheduled once (tracked by a persisted flag).
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        createChannels()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SCHEDULED, false)) {
            scheduleDailyTrending()
            scheduleWeeklyUpdate()
            prefs.edit().putBoolean(KEY_SCHEDULED, true).apply()
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRENDING,
                "Trending Recipes",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily trending recipe inspiration from CookScope."
            }
        )
        nm?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RATE_LIMIT,
                "Assistant Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "A tap on the shoulder when your CookScope assistant is ready again."
            }
        )
        nm?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RECIPE_UPDATES,
                "Recipe Updates",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Weekly highlights of new recipes and collections."
            }
        )
    }

    /** Whether the user has granted POST_NOTIFICATIONS (always true pre-13). */
    fun notificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Schedule (or reschedule) the daily 11:00 trending recipe alarm.
     * Uses [AlarmManager.setInexactRepeating] to be battery-friendly.
     */
    fun scheduleDailyTrending() {
        val triggerAt = nextDailyTrigger()
        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            action = ACTION_DAILY
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager()?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            INTERVAL_DAY_MILLIS,
            pi,
        )
    }

    /**
     * Schedule (or reschedule) the weekly Saturday 11:00 recipe-update alarm.
     */
    fun scheduleWeeklyUpdate() {
        val triggerAt = nextWeeklyTrigger()
        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            action = ACTION_WEEKLY
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_WEEKLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager()?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            INTERVAL_WEEK_MILLIS,
            pi,
        )
    }

    /**
     * Schedule a one-shot "Your Assistant is waiting for you" reminder at
     * [triggerAtMillis] (the next rate-limit reset). Any previously scheduled
     * rate-limit reminder is cancelled first so only the latest window end is
     * surfaced.
     */
    fun scheduleRateLimitReminder(triggerAtMillis: Long) {
        if (!::appContext.isInitialized) return
        val now = System.currentTimeMillis()
        if (triggerAtMillis <= now) return

        cancelRateLimitReminder()

        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            action = ACTION_RATE_LIMIT
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_RATE_LIMIT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = alarmManager() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_RATE_LIMIT_PENDING, triggerAtMillis).apply()
    }

    /** Cancel any pending rate-limit-reset reminder. */
    fun cancelRateLimitReminder() {
        if (!::appContext.isInitialized) return
        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            action = ACTION_RATE_LIMIT
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_RATE_LIMIT,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) {
            alarmManager()?.cancel(pi)
        }
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RATE_LIMIT_PENDING).apply()
    }

    /** Open the system app-notification settings for CookScope. */
    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Build and post a notification for the given [action]. Called from
     * [NotificationReceiver.onReceive]. No-op if notifications are disabled.
     */
    internal fun postForAction(action: String) {
        if (!notificationsEnabled()) return
        when (action) {
            ACTION_DAILY -> postTrending()
            ACTION_WEEKLY -> postWeeklyUpdate()
            ACTION_RATE_LIMIT -> postRateLimitReset()
        }
    }

    private fun postTrending() {
        // Try to fetch today's server-side trending recipes first.
        // Falls back to the local seed list on any failure.
        val recipe: Recipe? = runBlocking {
            withTimeoutOrNull(5_000L) {
                try {
                    val service = RecipeDiscoveryService()
                    val result = service.loadTrendingRecipes()
                    service.close()
                    result.getOrNull()?.randomOrNull()
                } catch (_: Exception) {
                    null
                }
            }
        } ?: RecipeData.recipes.randomOrNull()

        if (recipe == null) return
        val contentText = "Today's trending recipe: ${recipe.title} — tap to cook."
        showNotification(
            channelId = CHANNEL_TRENDING,
            title = "Trending today",
            text = contentText,
            notificationId = REQUEST_DAILY,
        )
    }

    private fun postWeeklyUpdate() {
        showNotification(
            channelId = CHANNEL_RECIPE_UPDATES,
            title = "What's cooking this week",
            text = "New recipes added this week — explore what's cooking on CookScope!",
            notificationId = REQUEST_WEEKLY,
        )
    }

    private fun postRateLimitReset() {
        showNotification(
            channelId = CHANNEL_RATE_LIMIT,
            title = "Your Assistant is waiting for you",
            text = "Your CookScope questions and scans have refilled. Come back and keep cooking!",
            notificationId = REQUEST_RATE_LIMIT,
        )
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RATE_LIMIT_PENDING).apply()
    }

    private fun showNotification(
        channelId: String,
        title: String,
        text: String,
        notificationId: Int,
    ) {
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet — silently skip.
        }
    }

    private fun alarmManager(): AlarmManager? =
        appContext.getSystemService(AlarmManager::class.java)

    private fun nextDailyTrigger(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DAILY_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun nextWeeklyTrigger(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DAILY_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Next Saturday (or today if still before 11:00)
            while (get(Calendar.DAY_OF_WEEK) != WEEKLY_DAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
