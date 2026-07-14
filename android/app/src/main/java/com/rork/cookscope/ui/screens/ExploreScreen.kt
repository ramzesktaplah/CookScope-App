package com.rork.cookscope.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rork.cookscope.data.FoodVideoData
import com.rork.cookscope.ui.components.VideoFeedItem

/**
 * Explore (Watch) tab — a TikTok-style vertical food video feed.
 * A single full-screen PlayerView hosts the shared ExoPlayer surface.
 * The VerticalPager only drives the overlay UI; the player surface stays stable,
 * so videos do not flicker or black out while swiping.
 */
@Composable
fun ExploreScreen(onRecipeClick: (String) -> Unit) {
    val videos = FoodVideoData.videos
    val pagerState = rememberPagerState(pageCount = { videos.size })
    val context = LocalContext.current

    var volume by remember { mutableFloatStateOf(1f) }
    var isPlayerReady by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    val activeIndex = pagerState.currentPage

    // Load the active video and keep it playing
    LaunchedEffect(activeIndex) {
        val video = videos[activeIndex]
        exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(volume) {
        exoPlayer.volume = volume
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Single stable video surface behind the pager UI
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val video = videos[page]
            val isActive = page == activeIndex

            VideoFeedItem(
                video = video,
                exoPlayer = exoPlayer,
                isActive = isActive,
                volume = volume,
                onToggleVolume = { volume = if (volume > 0f) 0f else 1f },
                onTogglePlay = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onRecipeClick = {
                    video.recipeId?.let { onRecipeClick(it) }
                },
            )
        }

        // Top header overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        ) {
            Text(
                "Watch",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Text(
                "Swipe to discover · Tap to play/pause",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopStart).padding(top = 34.dp),
            )
        }
    }
}
