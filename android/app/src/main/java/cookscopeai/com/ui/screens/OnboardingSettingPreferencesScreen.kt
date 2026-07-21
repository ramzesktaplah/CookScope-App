package cookscopeai.com.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cookscopeai.com.R
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.delay

/**
 * Setting Preferences loading screen — shows a friendly "tailoring your
 * assistant" message while the app prepares the collected preferences, then
 * automatically advances to the notification permission screen after a short
 * delay.
 */
@Composable
fun OnboardingSettingPreferencesScreen(
    onContinue: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(2200)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GoldPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )

            Spacer(Modifier.height(48.dp))

            Image(
                painter = painterResource(R.drawable.ic_cookscope_mascot),
                contentDescription = "CookScope assistant",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(180.dp),
            )

            Spacer(Modifier.height(32.dp))

            SpeechBubble(
                text = "Setting your preferences...",
                highlighted = "preferences",
            )

            Spacer(Modifier.height(24.dp))

            CircularProgressIndicator(
                color = GoldPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Tailoring recipes, tips, and flavors just for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
