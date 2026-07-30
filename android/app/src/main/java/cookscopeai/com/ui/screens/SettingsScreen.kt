package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Star
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
import cookscopeai.com.data.NotificationService
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.data.ThemeManager
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.theme.GoldPrimary

private const val TERMS_URL = "https://cookscope.ai/terms"
private const val PRIVACY_URL = "https://cookscope.ai/privacy"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPreferences: () -> Unit,
    onSavedRecipes: () -> Unit,
    onNotifications: () -> Unit,
    onUpgrade: () -> Unit,
    onSignOut: () -> Unit = {},
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val preferences by UserManager.preferences.collectAsState()
    val context = LocalContext.current

    val preferencesSubtitle = buildPreferencesSubtitle(preferences)

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
                    .padding(top = 16.dp, start = 4.dp, end = 16.dp, bottom = 8.dp),
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
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Account section
        item {
            SettingsSection("Account")
            SettingsRow(
                icon = Icons.Default.Person,
                title = "Preferences",
                subtitle = preferencesSubtitle,
                onClick = onPreferences,
            )
            SettingsRow(
                icon = Icons.Default.Bookmark,
                title = "Saved Recipes",
                subtitle = "View your bookmarked recipes",
                onClick = onSavedRecipes,
            )
            SettingsRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Push alerts, recipe updates, new content",
                onClick = onNotifications,
            )
        }

        // Appearance section
        item {
            Spacer(Modifier.height(24.dp))
            SettingsSection("Appearance")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Dark Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (isDarkMode) "On" else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { ThemeManager.setDarkMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GoldPrimary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }

        // Subscription section
        item {
            Spacer(Modifier.height(24.dp))
            SettingsSection("Subscription")
            SettingsRow(
                icon = Icons.Default.Star,
                title = if (isPremium) "Premium Active" else "Upgrade to Premium",
                subtitle = if (isPremium) "You have CookScope Premium" else "$4.99/mo or $49.99/yr",
                onClick = onUpgrade,
            )
        }

        // Legal section
        item {
            Spacer(Modifier.height(24.dp))
            SettingsSection("Legal")
            SettingsRow(
                icon = Icons.Default.Description,
                title = "Terms & Conditions",
                subtitle = "Read our terms of service",
                onClick = onTerms,
            )
            SettingsRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "How we handle your data",
                onClick = onPrivacy,
            )
        }

        // Sign out
        item {
            Spacer(Modifier.height(24.dp))
            SettingsRow(
                icon = Icons.Default.Logout,
                title = "Sign Out",
                subtitle = "Sign out of your CookScope account",
                onClick = onSignOut,
            )
        }

        // App info
        item {
            Spacer(Modifier.height(32.dp))
            Text(
                "CookScope v1.0\nCrafting culinary confidence with AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = GoldPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
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
                icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun buildPreferencesSubtitle(prefs: UserManager.UserPreferences): String {
    val parts = mutableListOf<String>()
    if (prefs.skillLevel.isNotBlank()) parts.add(prefs.skillLevel)
    if (prefs.dietaryTags.isNotEmpty()) parts.add(prefs.dietaryTags.take(2).joinToString(", "))
    if (prefs.favoriteCuisines.isNotEmpty()) parts.add(prefs.favoriteCuisines.take(2).joinToString(", "))
    if (prefs.allergies.isNotEmpty()) parts.add("Avoid: ${prefs.allergies.take(2).joinToString(", ")}")
    return if (parts.isEmpty()) "Dietary restrictions, skill level, favorite cuisines" else parts.joinToString(" · ")
}
