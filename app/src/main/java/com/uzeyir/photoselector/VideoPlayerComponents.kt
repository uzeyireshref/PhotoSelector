package com.uzeyir.photoselector

import android.net.Uri
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

internal class VideoPlaybackController(
    val state: VideoPlaybackState,
    val player: ExoPlayer?,
    val playFullscreen: (Uri) -> Unit,
    val exitFullscreen: () -> Unit,
    val clearIfDifferentPage: (Uri) -> Unit
) {
    val activeMedia: MediaItemData? = state.activeMedia
    val fullscreenMedia: MediaItemData? = state.fullscreenMedia
    val isFullscreen: Boolean = state.isFullscreen
}

@Composable
internal fun rememberVideoPlaybackController(mediaItems: List<MediaItemData>): VideoPlaybackController {
    var session by remember { mutableStateOf(VideoPlaybackSession()) }
    val state = remember(mediaItems, session) {
        videoPlaybackStateFor(mediaItems, session)
    }
    val player = rememberSharedVideoPlayer(state.activeMedia)
    return VideoPlaybackController(
        state = state,
        player = player,
        playFullscreen = { uri -> session = session.playFullscreen(uri) },
        exitFullscreen = { session = session.exitFullscreen() },
        clearIfDifferentPage = { uri -> session = session.clearIfDifferentPage(uri) }
    )
}

@Composable
fun rememberSharedVideoPlayer(media: MediaItemData?): ExoPlayer? {
    val context = LocalContext.current
    return if (media == null) {
        null
    } else {
        val player = remember(media.uri) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(PlayerMediaItem.fromUri(media.uri))
                prepare()
            }
        }
        DisposableEffect(player) {
            onDispose {
                player.release()
            }
        }
        player
    }
}

@Composable
fun VideoPoster(
    media: MediaItemData,
    strings: LocalizedStrings,
    onPlay: () -> Unit,
    onSingleTap: () -> Unit
) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(media.uri) {
                detectTapGestures(onTap = { onSingleTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        val thumbnailRequest = remember(media.uri, constraints.maxWidth, constraints.maxHeight) {
            val (requestWidth, requestHeight) = videoPosterRequestSizePx(
                containerWidthPx = constraints.maxWidth,
                containerHeightPx = constraints.maxHeight
            )
            ImageRequest.Builder(context)
                .data(media.uri)
                .size(requestWidth, requestHeight)
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = thumbnailRequest,
            contentDescription = media.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        FilledIconButton(
            onClick = onPlay,
            modifier = Modifier.size(84.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.48f),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = strings.video,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
@OptIn(UnstableApi::class)
fun VideoFullscreenPlayer(
    player: ExoPlayer,
    media: MediaItemData,
    strings: LocalizedStrings,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BackHandler(onBack = onExit)

    var currentPositionMs by remember(media.uri) { mutableLongStateOf(0L) }
    var durationMs by remember(media.uri) { mutableLongStateOf(0L) }
    var isVideoPlaying by remember(media.uri) { mutableStateOf(false) }
    var playbackState by remember(media.uri) { mutableIntStateOf(player.playbackState) }
    var videoWidth by remember(media.uri) { mutableIntStateOf(0) }
    var videoHeight by remember(media.uri) { mutableIntStateOf(0) }

    fun updatePlaybackState() {
        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it > 0L } ?: 0L
        isVideoPlaying = player.isPlaying
        playbackState = player.playbackState
    }

    LaunchedEffect(player) {
        player.playWhenReady = true
        player.play()
        while (true) {
            updatePlaybackState()
            delay(500)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
                if (shouldExitFullscreenForPlaybackState(playbackState)) {
                    player.pause()
                    player.seekTo(0L)
                    onExit()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.pause()
            player.seekTo(0L)
            player.removeListener(listener)
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val rotationDegrees = videoFullscreenRotationDegrees(videoWidth, videoHeight)
        val (mediaWidthPx, mediaHeightPx) = fullscreenVideoSurfaceSize(
            containerWidthPx = constraints.maxWidth,
            containerHeightPx = constraints.maxHeight,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            rotationDegrees = rotationDegrees
        )
        val mediaWidth = with(density) { mediaWidthPx.toDp() }
        val mediaHeight = with(density) { mediaHeightPx.toDp() }

        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = Modifier
                .requiredSize(width = mediaWidth, height = mediaHeight)
                .graphicsLayer(rotationZ = rotationDegrees.toFloat())
        )

        if (shouldShowFullscreenVideoLoading(playbackState)) {
            Surface(
                color = Color.Black.copy(alpha = 0.46f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        FilledIconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.48f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.FullscreenExit, contentDescription = strings.exitFullscreen)
        }

        VideoPlaybackControls(
            isPlaying = isVideoPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onPlayPause = {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                updatePlaybackState()
            },
            onSeekTo = { positionMs ->
                player.seekTo(positionMs)
                updatePlaybackState()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 20.dp)
        )
    }
}

@Composable
fun VideoPlaybackControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text(
                text = formatPlaybackTime(currentPositionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            val sliderMax = durationMs.takeIf { it > 0L } ?: 1L
            Slider(
                value = currentPositionMs.coerceIn(0L, sliderMax).toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..sliderMax.toFloat(),
                enabled = durationMs > 0L,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatPlaybackTime(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1_000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
