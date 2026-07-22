package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.delay

/**
 * Reusable rate-limit dialog shown when a user hits the usage cap for a feature.
 *
 * @param featureName e.g. "recipe generation", "search", "scan"
 * @param freeLimit limit for free users
 * @param premiumLimit limit for premium users
 * @param windowDescription e.g. "30-minute window", "24-hour window"
 * @param timeRemaining live-formatted countdown string
 * @param isPremium whether the user is on premium
 * @param onUpgrade called when free user taps "Upgrade to Premium"
 * @param onDismiss called when user dismisses the dialog
 */
@Composable
fun RateLimitDialog(
    featureName: String,
    freeLimit: Int,
    premiumLimit: Int,
    windowDescription: String,
    timeRemaining: String,
    isPremium: Boolean,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Limit Reached",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column {
                Text(
                    if (isPremium) {
                        "You've used all $premiumLimit $featureName this $windowDescription. " +
                            "$featureName refill in $timeRemaining."
                    } else {
                        "You've used all $freeLimit free $featureName this $windowDescription. " +
                            "$featureName refill in $timeRemaining. " +
                            "Upgrade to Premium for $premiumLimit $featureName / $windowDescription."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (!isPremium) {
                TextButton(onClick = onUpgrade) {
                    Text("Upgrade", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Got it")
                }
            }
        },
        dismissButton = {
            if (!isPremium) {
                TextButton(onClick = onDismiss) {
                    Text("Got it")
                }
            }
        },
    )
}

/**
 * Full-screen rate-limit view (used when a whole feature is blocked).
 * Includes a live countdown timer.
 */
@Composable
fun RateLimitFullScreen(
    featureName: String,
    freeLimit: Int,
    premiumLimit: Int,
    windowDescription: String,
    formatTimeRemaining: () -> String,
    isPremium: Boolean,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
) {
    // Live countdown
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val timeRemaining = remember(tick) { formatTimeRemaining() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Limit Reached",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isPremium) {
                "You've used all $premiumLimit $featureName this $windowDescription. " +
                    "Refill in $timeRemaining."
            } else {
                "You've used all $freeLimit free $featureName this $windowDescription. " +
                    "Refill in $timeRemaining. Upgrade to Premium for $premiumLimit / $windowDescription."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(GoldLight, GoldPrimary, GoldDeep))
                    )
                    .clickable { onUpgrade() }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    "Upgrade to Premium",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "Go Back",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onBack() },
        )
    }
}
