package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cookscopeai.com.data.NotificationCategory
import cookscopeai.com.data.NotificationPreferencesManager
import cookscopeai.com.data.NotificationPreferencesManager.NotificationPrefs
import cookscopeai.com.data.NotificationService
import cookscopeai.com.ui.theme.GoldPrimary

/**
 * Settings screen for per-category notification preferences.
 *
 * Each toggle maps to a boolean column in the
 * `user_notification_preferences` Supabase table and a notification channel
 * on the device. Toggling off stops both local scheduled notifications and
 * (once FCM is configured) backend pushes for that category.
 */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
) {
    val prefs by NotificationPreferencesManager.preferences.collectAsState()
    val context = LocalContext.current

    val categories = listOf(
        CategoryItem(
            category = NotificationCategory.POPULAR_RECIPE,
            icon = Icons.Default.Explore,
            title = "Popular Recipe",
            subtitle = "Daily popular recipe highlight at 8 AM",
        ),
        CategoryItem(
            category = NotificationCategory.MEALTIME,
            icon = Icons.Default.WbSunny,
            title = "Mealtime Inspiration",
            subtitle = "Lunch (12 PM), dinner (5:30 PM), weekend ideas",
        ),
        CategoryItem(
            category = NotificationCategory.SCAN_AI,
            icon = Icons.Default.AutoAwesome,
            title = "Scan & AI Assistant",
            subtitle = "Post-scan suggestions and daily AI cooking tips",
        ),
        CategoryItem(
            category = NotificationCategory.SAVED_RECIPES,
            icon = Icons.Default.Bookmark,
            title = "Saved Recipes",
            subtitle = "Reminders after saving recipes and new collections",
        ),
        CategoryItem(
            category = NotificationCategory.ENGAGEMENT,
            icon = Icons.Default.SentimentSatisfied,
            title = "Re-engagement",
            subtitle = "We miss you nudges after 1, 3, or 7 days away",
        ),
        CategoryItem(
            category = NotificationCategory.ACHIEVEMENTS,
            icon = Icons.Default.Star,
            title = "Achievements",
            subtitle = "First recipe, scan & favorite milestones",
        ),
        CategoryItem(
            category = NotificationCategory.TRENDING,
            icon = Icons.Default.TrendingUp,
            title = "Trending & Seasonal",
            subtitle = "Weekly trending and seasonal recipe highlights",
        ),
        CategoryItem(
            category = NotificationCategory.PREMIUM,
            icon = Icons.Default.LocalDining,
            title = "Premium",
            subtitle = "Upgrade nudges and trial-ending reminders",
        ),
        CategoryItem(
            category = NotificationCategory.RATE_LIMIT_RESET,
            icon = Icons.Default.Schedule,
            title = "Assistant Reminders",
            subtitle = "Tap on the shoulder when your limits refill",
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Description
        item {
            Text(
                "Choose which CookScope notifications you'd like to receive. " +
                    "You can always change these later in your device settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // Category toggles
        items(categories) { item ->
            CategoryToggleRow(
                item = item,
                enabled = isEnabled(prefs, item.category),
                onToggle = { enabled ->
                    NotificationPreferencesManager.setCategory(item.category, enabled)
                },
            )
        }

        // System settings button
        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { NotificationService.openAppNotificationSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.Default.NotificationImportant,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Open System Notification Settings", fontWeight = FontWeight.SemiBold)
            }
        }

        // Footer note
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Toggling a category off stops both scheduled device " +
                    "notifications and server pushes for that category.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

private data class CategoryItem(
    val category: NotificationCategory,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
private fun CategoryToggleRow(
    item: CategoryItem,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = GoldPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

private fun isEnabled(prefs: NotificationPrefs, category: NotificationCategory): Boolean {
    return when (category) {
        NotificationCategory.POPULAR_RECIPE -> prefs.popularRecipe
        NotificationCategory.MEALTIME -> prefs.mealtime
        NotificationCategory.SCAN_AI -> prefs.scanAi
        NotificationCategory.SAVED_RECIPES -> prefs.savedRecipes
        NotificationCategory.ENGAGEMENT -> prefs.engagement
        NotificationCategory.ACHIEVEMENTS -> prefs.achievements
        NotificationCategory.TRENDING -> prefs.trending
        NotificationCategory.PREMIUM -> prefs.premium
        NotificationCategory.RATE_LIMIT_RESET -> prefs.rateLimitReset
    }
}
