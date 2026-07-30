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
 * CookScope notification center.
 *
 * Nine notification channels (one per category) so users can manage each
 * type independently in Android system settings:
 *
 *  - `popular_recipe`  — daily Popular Recipe highlight (replaces old `trending`)
 *  - `mealtime`        — Good Morning (8 AM), Lunch (12 PM), Dinner (5:30 PM)
 *  - `scan_ai`         — post-scan suggestions & AI assistant tips
 *  - `saved_recipes`   — reminders after saving + new collection nudges
 *  - `engagement`      — 1/3/7-day inactivity re-engagement
 *  - `achievements`    — first recipe, scan & favorite milestones
 *  - `trending`        — seasonal / trending catalog highlights
 *  - `premium`         — upgrade nudges & trial-ending reminders
 *  - `rate_limit_reset`— "Your Assistant is waiting" when a window refills
 *
 * Scheduled alarms:
 *  - Daily 8:00 AM  → Popular Recipe (Good Morning)
 *  - Daily 12:00 PM → Mealtime (Lunch)
 *  - Daily 5:30 PM  → Mealtime (Dinner)
 *  - Saturday 10:00 AM → Mealtime (Weekend)
 *  - Weekly Sunday 9:00 AM → Trending/seasonal update
 *
 * Triggered (event-based) notifications are posted immediately via the
 * `showTriggered*` helpers and respect [NotificationPreferencesManager].
 *
 * Alarms are scheduled via [AlarmManager]; the actual notifications are
 * posted by [NotificationReceiver]. POST_NOTIFICATIONS must be granted at
 * runtime (Android 13+) before notifications will display.
 */
object NotificationService {

    // ---- Channel IDs ----
    const val CHANNEL_POPULAR_RECIPE = "popular_recipe"
    const val CHANNEL_MEALTIME = "mealtime"
    const val CHANNEL_SCAN_AI = "scan_ai"
    const val CHANNEL_SAVED_RECIPES = "saved_recipes"
    const val CHANNEL_ENGAGEMENT = "engagement"
    const val CHANNEL_ACHIEVEMENTS = "achievements"
    const val CHANNEL_TRENDING = "trending"
    const val CHANNEL_PREMIUM = "premium"
    const val CHANNEL_RATE_LIMIT = "rate_limit_reset"

    private const val PREFS_NAME = "cookscope_notifications"
    private const val KEY_SCHEDULED = "repeating_scheduled"
    private const val KEY_RATE_LIMIT_PENDING = "rate_limit_pending"
    private const val KEY_LAST_ACTIVE = "last_active_millis"
    private const val KEY_SCAN_COUNT = "scan_count"
    private const val KEY_FAVORITE_COUNT = "favorite_count"
    private const val KEY_FIRST_RECIPE_FIRED = "first_recipe_fired"
    private const val KEY_SCAN_MILESTONE_FIRED = "scan_milestone_fired"
    private const val KEY_FAV_MILESTONE_FIRED = "fav_milestone_fired"

    // ---- Alarm request codes ----
    private const val REQUEST_POPULAR = 1001
    private const val REQUEST_LUNCH = 1002
    private const val REQUEST_DINNER = 1003
    private const val REQUEST_WEEKEND = 1004
    private const val REQUEST_WEEKLY_TRENDING = 1005
    private const val REQUEST_RATE_LIMIT = 1006
    private const val REQUEST_INACTIVITY = 1007

    // ---- Broadcast actions ----
    const val ACTION_POPULAR_RECIPE = "cookscopeai.com.POPULAR_RECIPE"
    const val ACTION_MEALTIME_LUNCH = "cookscopeai.com.MEALTIME_LUNCH"
    const val ACTION_MEALTIME_DINNER = "cookscopeai.com.MEALTIME_DINNER"
    const val ACTION_MEALTIME_WEEKEND = "cookscopeai.com.MEALTIME_WEEKEND"
    const val ACTION_WEEKLY_TRENDING = "cookscopeai.com.WEEKLY_TRENDING"
    const val ACTION_RATE_LIMIT_RESET = "cookscopeai.com.RATE_LIMIT_RESET"
    const val ACTION_INACTIVITY = "cookscopeai.com.INACTIVITY"
    // Triggered actions (carried via intent extras, posted immediately).
    const val ACTION_TRIGGERED = "cookscopeai.com.TRIGGERED"
    const val EXTRA_CHANNEL = "extra_channel"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TEXT = "extra_text"
    const val EXTRA_NOTIF_ID = "extra_notif_id"
    const val EXTRA_DEEP_LINK = "extra_deep_link"

    private const val INTERVAL_DAY_MILLIS = 24L * 60L * 60_000L
    private const val INTERVAL_WEEK_MILLIS = 7L * INTERVAL_DAY_MILLIS

    private lateinit var appContext: Context

    /**
     * Create notification channels and schedule the repeating alarms on
     * first launch. Safe to call multiple times — repeating alarms are only
     * scheduled once (tracked by a persisted flag).
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        createChannels()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SCHEDULED, false)) {
            scheduleAllRepeating()
            prefs.edit().putBoolean(KEY_SCHEDULED, true).apply()
        }
        // Always (re)schedule the inactivity check on launch.
        scheduleInactivityCheck()
        markActive()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(NotificationManager::class.java) ?: return

        fun make(id: String, name: String, desc: String, importance: Int) {
            nm.createNotificationChannel(
                NotificationChannel(id, name, importance).apply { description = desc }
            )
        }
        make(CHANNEL_POPULAR_RECIPE, "Popular Recipe", "Daily popular recipe inspiration from CookScope.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_MEALTIME, "Mealtime Inspiration", "Good morning, lunch, and dinner cooking ideas.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_SCAN_AI, "Scan & AI Assistant", "Post-scan suggestions and AI cooking tips.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_SAVED_RECIPES, "Saved Recipes", "Reminders about your bookmarked recipes and collections.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_ENGAGEMENT, "Re-engagement", "We miss you! Come back and cook something delicious.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_ACHIEVEMENTS, "Achievements", "Celebrate your cooking milestones.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_TRENDING, "Trending & Seasonal", "Seasonal highlights and trending recipes.", NotificationManager.IMPORTANCE_LOW)
        make(CHANNEL_PREMIUM, "Premium", "Upgrade nudges and trial-ending reminders.", NotificationManager.IMPORTANCE_DEFAULT)
        make(CHANNEL_RATE_LIMIT, "Assistant Reminders", "A tap on the shoulder when your CookScope assistant is ready again.", NotificationManager.IMPORTANCE_HIGH)
    }

    /** Whether the user has granted POST_NOTIFICATIONS (always true pre-13). */
    fun notificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ------------------------------------------------------------------
    // Scheduling
    // ------------------------------------------------------------------

    /** Schedule all repeating daily/weekly alarms. */
    fun scheduleAllRepeating() {
        if (!::appContext.isInitialized) return
        scheduleRepeatingDaily(ACTION_POPULAR_RECIPE, REQUEST_POPULAR, 8, 0)
        scheduleRepeatingDaily(ACTION_MEALTIME_LUNCH, REQUEST_LUNCH, 12, 0)
        scheduleRepeatingDaily(ACTION_MEALTIME_DINNER, REQUEST_DINNER, 17, 30)
        scheduleRepeatingWeekly(ACTION_MEALTIME_WEEKEND, REQUEST_WEEKEND, Calendar.SATURDAY, 10, 0)
        scheduleRepeatingWeekly(ACTION_WEEKLY_TRENDING, REQUEST_WEEKLY_TRENDING, Calendar.SUNDAY, 9, 0)
    }

    private fun scheduleRepeatingDaily(action: String, requestCode: Int, hour: Int, minute: Int) {
        val triggerAt = nextDailyTrigger(hour, minute)
        val pi = broadcastPI(action, requestCode)
        alarmManager()?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            INTERVAL_DAY_MILLIS,
            pi,
        )
    }

    private fun scheduleRepeatingWeekly(action: String, requestCode: Int, dayOfWeek: Int, hour: Int, minute: Int) {
        val triggerAt = nextWeeklyTrigger(dayOfWeek, hour, minute)
        val pi = broadcastPI(action, requestCode)
        alarmManager()?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            INTERVAL_WEEK_MILLIS,
            pi,
        )
    }

    /**
     * Schedule a one-shot rate-limit reminder at [triggerAtMillis].
     * Any previously scheduled reminder is cancelled first.
     */
    fun scheduleRateLimitReminder(triggerAtMillis: Long) {
        if (!::appContext.isInitialized) return
        val now = System.currentTimeMillis()
        if (triggerAtMillis <= now) return
        cancelRateLimitReminder()
        val pi = broadcastPI(ACTION_RATE_LIMIT_RESET, REQUEST_RATE_LIMIT)
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

    /** Cancel any pending rate-limit reminder. */
    fun cancelRateLimitReminder() {
        if (!::appContext.isInitialized) return
        val pi = broadcastPINullable(ACTION_RATE_LIMIT_RESET, REQUEST_RATE_LIMIT)
        if (pi != null) alarmManager()?.cancel(pi)
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RATE_LIMIT_PENDING).apply()
    }

    /** Schedule a one-shot inactivity re-engagement check 24h from now. */
    fun scheduleInactivityCheck() {
        if (!::appContext.isInitialized) return
        cancelInactivityCheck()
        val triggerAt = System.currentTimeMillis() + INTERVAL_DAY_MILLIS
        val pi = broadcastPI(ACTION_INACTIVITY, REQUEST_INACTIVITY)
        alarmManager()?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun cancelInactivityCheck() {
        if (!::appContext.isInitialized) return
        val pi = broadcastPINullable(ACTION_INACTIVITY, REQUEST_INACTIVITY)
        if (pi != null) alarmManager()?.cancel(pi)
    }

    // ------------------------------------------------------------------
    // Activity tracking & achievement counters
    // ------------------------------------------------------------------

    /** Record that the user is active right now (resets inactivity timer). */
    fun markActive() {
        if (!::appContext.isInitialized) return
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    /** Increment the lifetime scan counter and fire achievement notifications. */
    fun recordScanComplete() {
        if (!::appContext.isInitialized) return
        val p = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = p.getInt(KEY_SCAN_COUNT, 0) + 1
        p.edit().putInt(KEY_SCAN_COUNT, count).apply()
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.ACHIEVEMENTS)) return
        val fired = p.getInt(KEY_SCAN_MILESTONE_FIRED, 0)
        val milestone = nextScanMilestone(count, fired)
        if (milestone > 0) {
            p.edit().putInt(KEY_SCAN_MILESTONE_FIRED, milestone).apply()
            showTriggered(
                channel = CHANNEL_ACHIEVEMENTS,
                title = "Scan Milestone! \uD83C\uDF89",
                text = "Amazing — you've scanned $count dishes with CookScope. Keep exploring!",
                notificationId = 5200 + milestone,
            )
        }
    }

    /** Increment the saved-recipe counter and fire achievement notifications. */
    fun recordRecipeSaved() {
        if (!::appContext.isInitialized) return
        val p = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = p.getInt(KEY_FAVORITE_COUNT, 0) + 1
        p.edit().putInt(KEY_FAVORITE_COUNT, count).apply()
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.ACHIEVEMENTS)) return
        val fired = p.getInt(KEY_FAV_MILESTONE_FIRED, 0)
        val milestone = nextFavoriteMilestone(count, fired)
        if (milestone > 0) {
            p.edit().putInt(KEY_FAV_MILESTONE_FIRED, milestone).apply()
            showTriggered(
                channel = CHANNEL_ACHIEVEMENTS,
                title = "Favorite Milestone! \u2764\uFE0F",
                text = "You've saved $count recipes to your CookScope library. Your cookbook is growing!",
                notificationId = 5300 + milestone,
            )
        }
    }

    /** Fire the "first recipe generated" achievement (once only). */
    fun maybeFireFirstRecipeAchievement() {
        if (!::appContext.isInitialized) return
        val p = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY_FIRST_RECIPE_FIRED, false)) return
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.ACHIEVEMENTS)) return
        p.edit().putBoolean(KEY_FIRST_RECIPE_FIRED, true).apply()
        showTriggered(
            channel = CHANNEL_ACHIEVEMENTS,
            title = "First Recipe Created! \uD83C\uDF89",
            text = "You just generated your first AI recipe with CookScope. Welcome to the future of cooking!",
            notificationId = 5100,
        )
    }

    private fun nextScanMilestone(count: Int, fired: Int): Int {
        val milestones = listOf(1, 5, 10, 25, 50, 100, 250, 500)
        return milestones.firstOrNull { it <= count && it > fired } ?: 0
    }

    private fun nextFavoriteMilestone(count: Int, fired: Int): Int {
        val milestones = listOf(1, 5, 10, 25, 50, 100)
        return milestones.firstOrNull { it <= count && it > fired } ?: 0
    }

    // ------------------------------------------------------------------
    // Triggered (event-based) notifications
    // ------------------------------------------------------------------

    /** Post-scan suggestion (Scan & AI category). */
    fun showPostScanSuggestion(dishName: String?) {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.SCAN_AI)) return
        val title = "Scan Complete \uD83D\uDCF7"
        val text = if (dishName.isNullOrBlank()) {
            "Great scan! Ask CookScope AI for a recipe twist or nutrition tips."
        } else {
            "Nice — $dishName analyzed! Tap to get recipe ideas and nutrition insights."
        }
        showTriggered(channel = CHANNEL_SCAN_AI, title = title, text = text, notificationId = 6100)
    }

    /** Post-save reminder (Saved Recipes category). */
    fun showRecipeSavedReminder(recipeTitle: String?) {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.SAVED_RECIPES)) return
        showTriggered(
            channel = CHANNEL_SAVED_RECIPES,
            title = "Recipe Saved \uD83D\uDCD6",
            text = "\"${recipeTitle ?: "Recipe"}\" added to your library. We'll suggest similar recipes soon!",
            notificationId = 6200,
        )
    }

    /** Daily AI tip (Scan & AI category). */
    fun showDailyAiTip() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.SCAN_AI)) return
        val tips = listOf(
            "Ask AI: 'What can I make with chicken and rice?' for instant inspiration.",
            "Pro tip: Scan your fridge to get a recipe from what you already have.",
            "Did you know? You can ask CookScope AI for wine pairings with any dish.",
            "Try asking AI for a healthy swap for heavy cream in your favorite recipe.",
        )
        showTriggered(
            channel = CHANNEL_SCAN_AI,
            title = "Daily AI Tip \uD83E\uDD16",
            text = tips.random(),
            notificationId = 6300,
        )
    }

    /** Premium upgrade nudge (Premium category). */
    fun showPremiumUpgradeNudge() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.PREMIUM)) return
        showTriggered(
            channel = CHANNEL_PREMIUM,
            title = "Unlock CookScope Plus \u2B50",
            text = "Get unlimited scans, AI chats, and recipe generation. Upgrade today and cook without limits.",
            notificationId = 6400,
        )
    }

    // ------------------------------------------------------------------
    // Receiver dispatch
    // ------------------------------------------------------------------

    /**
     * Build and post a notification for the given [action]. Called from
     * [NotificationReceiver.onReceive]. No-op if notifications are disabled
     * or the relevant category preference is off.
     */
    internal fun postForAction(action: String) {
        if (!notificationsEnabled()) return
        when (action) {
            ACTION_POPULAR_RECIPE -> postPopularRecipe()
            ACTION_MEALTIME_LUNCH -> postMealtimeLunch()
            ACTION_MEALTIME_DINNER -> postMealtimeDinner()
            ACTION_MEALTIME_WEEKEND -> postMealtimeWeekend()
            ACTION_WEEKLY_TRENDING -> postWeeklyTrending()
            ACTION_RATE_LIMIT_RESET -> postRateLimitReset()
            ACTION_INACTIVITY -> checkInactivity()
        }
    }

    /**
     * Post a triggered notification from an intent (used by the receiver
     * when [ACTION_TRIGGERED] is broadcast with channel/title/text extras).
     */
    internal fun postTriggeredFromIntent(intent: Intent) {
        if (!notificationsEnabled()) return
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val id = intent.getIntExtra(EXTRA_NOTIF_ID, 7000)
        showNotification(channelId = channel, title = title, text = text, notificationId = id)
    }

    private fun postPopularRecipe() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.POPULAR_RECIPE)) return
        val recipe = fetchDailyRecipe() ?: return
        showNotification(
            channelId = CHANNEL_POPULAR_RECIPE,
            title = "Good Morning! \uD83C\uDF73",
            text = "Today's popular recipe: ${recipe.title} — tap to cook.",
            notificationId = REQUEST_POPULAR,
        )
    }

    private fun postMealtimeLunch() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.MEALTIME)) return
        val recipe = fetchDailyRecipe() ?: return
        showNotification(
            channelId = CHANNEL_MEALTIME,
            title = "Lunch Time! \uD83E\uDD6A",
            text = "How about ${recipe.title} for lunch? Quick ideas from CookScope.",
            notificationId = REQUEST_LUNCH,
        )
    }

    private fun postMealtimeDinner() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.MEALTIME)) return
        val recipe = fetchDailyRecipe() ?: return
        showNotification(
            channelId = CHANNEL_MEALTIME,
            title = "Dinner Time! \uD83C\uDF5D",
            text = "Tonight's pick: ${recipe.title}. Tap for the full recipe and cooking guide.",
            notificationId = REQUEST_DINNER,
        )
    }

    private fun postMealtimeWeekend() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.MEALTIME)) return
        val recipe = fetchDailyRecipe() ?: return
        showNotification(
            channelId = CHANNEL_MEALTIME,
            title = "Weekend Cooking! \uD83C\uDFD5\uFE0F",
            text = "Weekend project: ${recipe.title}. You've got time to make something special.",
            notificationId = REQUEST_WEEKEND,
        )
    }

    private fun postWeeklyTrending() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.TRENDING)) return
        showNotification(
            channelId = CHANNEL_TRENDING,
            title = "What's Trending This Week \uD83D\uDD25",
            text = "New trending recipes added this week — explore what's cooking on CookScope!",
            notificationId = REQUEST_WEEKLY_TRENDING,
        )
    }

    private fun postRateLimitReset() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.RATE_LIMIT_RESET)) return
        showNotification(
            channelId = CHANNEL_RATE_LIMIT,
            title = "Your Assistant is waiting for you",
            text = "Your CookScope questions and scans have refilled. Come back and keep cooking!",
            notificationId = REQUEST_RATE_LIMIT,
        )
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RATE_LIMIT_PENDING).apply()
    }

    private fun checkInactivity() {
        if (!NotificationPreferencesManager.isEnabled(NotificationCategory.ENGAGEMENT)) {
            scheduleInactivityCheck()
            return
        }
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastActive = prefs.getLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
        val daysInactive = (System.currentTimeMillis() - lastActive) / INTERVAL_DAY_MILLIS
        when {
            daysInactive >= 7 -> showTriggered(
                channel = CHANNEL_ENGAGEMENT,
                title = "We miss you! \uD83D\uDC9B",
                text = "It's been a week since you cooked with us. Come back for fresh recipes and AI inspiration.",
                notificationId = 7107,
            )
            daysInactive >= 3 -> showTriggered(
                channel = CHANNEL_ENGAGEMENT,
                title = "Ready to cook again? \uD83D\uDD25",
                text = "It's been a few days. Your next great meal is one tap away with CookScope AI.",
                notificationId = 7103,
            )
            daysInactive >= 1 -> showTriggered(
                channel = CHANNEL_ENGAGEMENT,
                title = "Hungry for ideas? \uD83C\uDF96\uFE0F",
                text = "A day without cooking is a day without adventure. Let's find your next recipe!",
                notificationId = 7101,
            )
        }
        scheduleInactivityCheck()
    }

    // ------------------------------------------------------------------
    // Core helpers
    // ------------------------------------------------------------------

    /**
     * Immediately post a triggered notification, gated by [notificationsEnabled].
     * Use this for event-based notifications (post-scan, post-save, achievements).
     */
    private fun showTriggered(channel: String, title: String, text: String, notificationId: Int) {
        if (!notificationsEnabled()) return
        showNotification(channelId = channel, title = title, text = text, notificationId = notificationId)
    }

    /** Public wrapper so triggered actions can also be sent via broadcast. */
    fun showTriggeredNotification(channel: String, title: String, text: String, notificationId: Int) {
        showTriggered(channel, title, text, notificationId)
    }

    private fun showNotification(
        channelId: String,
        title: String,
        text: String,
        notificationId: Int,
    ) {
        if (!::appContext.isInitialized) return
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

    private fun fetchDailyRecipe(): Recipe? {
        return runBlocking {
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
    }

    private fun broadcastPI(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun broadcastPINullable(action: String, requestCode: Int): PendingIntent? {
        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(): AlarmManager? =
        appContext.getSystemService(AlarmManager::class.java)

    private fun nextDailyTrigger(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun nextWeeklyTrigger(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    /** Open the system app-notification settings for CookScope. */
    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
