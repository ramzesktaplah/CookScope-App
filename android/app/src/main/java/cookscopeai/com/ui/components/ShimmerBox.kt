package cookscopeai.com.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cookscopeai.com.ui.theme.GoldPrimary

/**
 * A single shimmering placeholder box. The shimmer sweep moves left-to-right
 * in a loop using an [infiniteRepeatable] animation and a linear gradient
 * brush whose position is driven by the animated [translate] value.
 *
 * Use as a building block for skeleton loading states.
 *
 * @param modifier modifier applied to the outer box (size, padding, etc.)
 * @param cornerRadius corner radius of the placeholder
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, delayMillis = 0),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val shimmerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)

    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, shimmerColor, highlight, base),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 0f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

/**
 * A skeleton placeholder for a chat message bubble — an avatar circle plus
 * two or three lines of varying width, mirrored to either the start or end
 * edge to mimic a user vs assistant message.
 *
 * @param isUser when true the skeleton is right-aligned (user side)
 * @param lines number of text lines to render
 */
@Composable
fun ChatBubbleSkeleton(
    isUser: Boolean = false,
    lines: Int = 2,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxBubbleWidth = (screenWidthDp * 0.7f).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        if (!isUser) {
            ShimmerBox(
                modifier = Modifier.size(32.dp),
                cornerRadius = 10.dp,
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .padding(top = 2.dp),
        ) {
            repeat(lines) { i ->
                val fraction = when {
                    isUser && i == lines - 1 -> 0.55f
                    i == lines - 1 -> 0.7f
                    i == 0 -> 0.95f
                    else -> 0.8f
                }
                ShimmerBox(
                    modifier = Modifier
                        .padding(top = if (i == 0) 0.dp else 6.dp)
                        .fillMaxWidth(fraction)
                        .height(12.dp),
                    cornerRadius = 6.dp,
                )
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            ShimmerBox(
                modifier = Modifier.size(32.dp),
                cornerRadius = 16.dp,
            )
        }
    }
}

/**
 * A compact three-dot pulse indicator with a soft gold shimmer, used as a
 * fallback typing hint when the bubble skeleton is not desired.
 */
@Composable
fun ShimmerDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 7.dp,
    color: Color = GoldPrimary,
) {
    val transition = rememberInfiniteTransition(label = "shimmerDots")
    Row(modifier = modifier) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 180),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dotAlpha$i",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha)),
            )
        }
    }
}
