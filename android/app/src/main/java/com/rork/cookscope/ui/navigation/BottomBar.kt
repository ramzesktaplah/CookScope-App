package com.rork.cookscope.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.rork.cookscope.ui.theme.GoldPrimary
import com.rork.cookscope.ui.theme.TextSecondary

data class GlassTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val glassTabs = listOf(
    GlassTabItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    GlassTabItem("explore", "Watch", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    GlassTabItem("chat", "Chat", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    GlassTabItem("create", "Create", Icons.Filled.Book, Icons.Outlined.Book),
    GlassTabItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

/**
 * CookScope glassmorphism bottom navigation bar.
 * Features a frosted glass effect with a floating pill-shaped container,
 * translucent dark background, gold highlights, and snappy animations.
 */
@Composable
fun CookScopeBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        )
                    )
                ),
        ) {
            // Subtle top highlight line for glass edge effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                GoldPrimary.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0f),
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                glassTabs.forEach { item ->
                    val selected = currentRoute == item.route
                    GlassTabButton(
                        item = item,
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTabButton(
    item: GlassTabItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "iconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "labelColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = tween(150),
        label = "scale",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
    }
}
