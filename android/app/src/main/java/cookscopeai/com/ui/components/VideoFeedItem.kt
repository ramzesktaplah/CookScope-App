package cookscopeai.com.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause as PauseIcon
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import cookscopeai.com.data.model.FoodVideo
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.delay

/**
 * A single full-screen video item in the Explore feed.
 * Shows the video surface (when active) or thumbnail, with overlay UI.
 *
 * Progress and play state are observed locally for the active page only,
 * so the entire pager does not recompose on every progress tick.
 */
@Composable
fun VideoFeedItem(
    video: FoodVideo,
    exoPlayer: ExoPlayer,
    isActive: Boolean,
    volume: Float,
    onToggleVolume: () -> Unit,
    onTogglePlay: () -> Unit,
    onRecipeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }

    // Local progress polling for the active item only
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        progress = 0f
        while (true) {
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition
            if (duration > 0) {
                progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            isPlaying = exoPlayer.isPlaying
            delay(100)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Thumbnail as background (always visible behind video)
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier.fillMaxSize(),
        )

        // Play/pause indicator overlay
        AnimatedVisibility(
            visible = isActive && !isPlaying,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // Tap layer for play/pause
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { if (isActive) onTogglePlay() },
        )

        // Bottom gradient + content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.3f),
                        1f to Color.Black.copy(alpha = 0.85f),
                    )
                )
                .padding(start = 16.dp, end = 80.dp, bottom = 32.dp, top = 80.dp),
        ) {
            // Chef row
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = video.chefAvatar,
                    contentDescription = video.chefName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    video.chefName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "Follow",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Title
            Text(
                video.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            // Description
            Text(
                video.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            // Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                video.tags.take(3).forEach { tag ->
                    Text(
                        "#$tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Right action rail — isolated so like/save toggles don't recompose the video content
        ActionRail(
            video = video,
            volume = volume,
            onToggleVolume = onToggleVolume,
            onRecipeClick = onRecipeClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 40.dp),
        )

        // Progress bar at very bottom
        if (isActive) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(bottom = 0.dp),
                color = GoldPrimary,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
private fun ActionRail(
    video: FoodVideo,
    volume: Float,
    onToggleVolume: () -> Unit,
    onRecipeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var liked by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val isMuted = volume == 0f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ActionButton(
            icon = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            label = formatCount(video.likes + if (liked) 1 else 0),
            tint = if (liked) Color(0xFFFF4757) else Color.White,
            onClick = { liked = !liked },
        )
        ActionButton(
            icon = Icons.Outlined.Comment,
            label = formatCount(video.comments),
            tint = Color.White,
            onClick = {},
        )
        ActionButton(
            icon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            label = if (saved) "Saved" else "Save",
            tint = if (saved) GoldPrimary else Color.White,
            onClick = { saved = !saved },
        )
        ActionButton(
            icon = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
            label = if (isMuted) "Mute" else "Sound",
            tint = if (isMuted) GoldPrimary else Color.White,
            onClick = onToggleVolume,
        )
        ActionButton(
            icon = Icons.Outlined.Share,
            label = formatCount(video.shares),
            tint = Color.White,
            onClick = {},
        )
        if (video.recipeId != null) {
            ActionButton(
                icon = Icons.Filled.MenuBook,
                label = "Recipe",
                tint = GoldPrimary,
                onClick = onRecipeClick,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
