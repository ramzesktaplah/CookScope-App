package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary

/**
 * Scan tab landing screen — lets the user choose between scanning a
 * prepared dish (AI nutrition & health analysis) or scanning raw
 * ingredients (AI ingredient detection + recipe suggestions).
 */
@Composable
fun ScanSelectionScreen(
    onScanDish: () -> Unit,
    onScanIngredients: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Spacer(Modifier.height(60.dp))
        Text(
            "Scan",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose what you'd like to scan with CookScope AI",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(36.dp))

        // Scan Your Dish card
        ScanOptionCard(
            title = "Scan Your Dish",
            description = "Snap a photo of any prepared meal. Get instant nutrition facts, health score, calorie breakdown & AI recommendations.",
            icon = Icons.Filled.Restaurant,
            gradient = listOf(GoldLight, GoldPrimary, GoldDeep),
            onClick = onScanDish,
        )

        Spacer(Modifier.height(18.dp))

        // Scan Your Ingredients card
        ScanOptionCard(
            title = "Scan Ingredients",
            description = "Point at raw ingredients in your kitchen. AI detects them and suggests matching recipes — or generate a new one instantly.",
            icon = Icons.Filled.AutoAwesome,
            gradient = listOf(GoldPrimary, GoldDeep, Color(0xFF003D99)),
            onClick = onScanIngredients,
        )
    }
}

@Composable
private fun ScanOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(gradient))
            .clickable { onClick() }
            .padding(24.dp),
    ) {
        // Icon circle top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        // Content bottom-left
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Start scanning",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
