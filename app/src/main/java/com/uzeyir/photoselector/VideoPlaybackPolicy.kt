package com.uzeyir.photoselector

import android.net.Uri
import androidx.media3.common.Player
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

fun rotatedMediaSize(containerWidthPx: Int, containerHeightPx: Int, rotationDegrees: Int): Pair<Int, Int> {
    if (containerWidthPx <= 0 || containerHeightPx <= 0) return containerWidthPx to containerHeightPx
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    return if (normalizedRotation == 90 || normalizedRotation == 270) {
        containerHeightPx to containerWidthPx
    } else {
        containerWidthPx to containerHeightPx
    }
}

fun videoFullscreenRotationDegrees(videoWidth: Int, videoHeight: Int): Int =
    if (videoWidth > 0 && videoHeight > 0 && videoWidth > videoHeight) 90 else 0

fun fullscreenVideoSurfaceSize(
    containerWidthPx: Int,
    containerHeightPx: Int,
    videoWidth: Int,
    videoHeight: Int,
    rotationDegrees: Int
): Pair<Int, Int> {
    if (containerWidthPx <= 0 || containerHeightPx <= 0 || videoWidth <= 0 || videoHeight <= 0) {
        return rotatedMediaSize(containerWidthPx, containerHeightPx, rotationDegrees)
    }

    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    val isQuarterTurn = normalizedRotation == 90 || normalizedRotation == 270
    val visualVideoWidth = if (isQuarterTurn) videoHeight else videoWidth
    val visualVideoHeight = if (isQuarterTurn) videoWidth else videoHeight
    val containerAspect = containerWidthPx.toDouble() / containerHeightPx.toDouble()
    val visualVideoAspect = visualVideoWidth.toDouble() / visualVideoHeight.toDouble()
    val visualSize = if (visualVideoAspect > containerAspect) {
        ceil(containerHeightPx * visualVideoAspect).toInt() to containerHeightPx
    } else {
        containerWidthPx to ceil(containerWidthPx / visualVideoAspect).toInt()
    }

    return if (isQuarterTurn) {
        visualSize.second to visualSize.first
    } else {
        visualSize
    }
}

fun shouldExitFullscreenForPlaybackState(playbackState: Int): Boolean =
    playbackState == Player.STATE_ENDED

fun shouldShowFullscreenVideoLoading(playbackState: Int): Boolean =
    playbackState == Player.STATE_IDLE || playbackState == Player.STATE_BUFFERING

data class VideoPlaybackSession(
    val activeUri: Uri? = null,
    val fullscreenUri: Uri? = null
) {
    fun playFullscreen(uri: Uri): VideoPlaybackSession =
        copy(activeUri = uri, fullscreenUri = uri)

    fun exitFullscreen(): VideoPlaybackSession =
        VideoPlaybackSession()

    fun clearIfDifferentPage(currentUri: Uri): VideoPlaybackSession =
        if (activeUri != null && activeUri != currentUri) {
            VideoPlaybackSession()
        } else {
            this
        }
}

data class VideoPlaybackState(
    val session: VideoPlaybackSession,
    val activeMedia: MediaItemData?,
    val fullscreenMedia: MediaItemData?
) {
    val isFullscreen: Boolean = fullscreenMedia != null
}

fun videoPlaybackStateFor(
    mediaItems: List<MediaItemData>,
    session: VideoPlaybackSession
): VideoPlaybackState {
    val activeMedia = session.activeUri?.let { uri -> mediaItems.firstOrNull { it.uri == uri } }
    val fullscreenMedia = session.fullscreenUri?.let { uri -> mediaItems.firstOrNull { it.uri == uri } }
    return VideoPlaybackState(
        session = session,
        activeMedia = activeMedia,
        fullscreenMedia = fullscreenMedia
    )
}

fun videoPosterRequestSizePx(
    containerWidthPx: Int,
    containerHeightPx: Int,
    maxSizePx: Int = 640
): Pair<Int, Int> {
    if (containerWidthPx <= 0 || containerHeightPx <= 0 || maxSizePx <= 0) return 1 to 1
    val scale = min(
        1.0,
        min(
            maxSizePx.toDouble() / containerWidthPx.toDouble(),
            maxSizePx.toDouble() / containerHeightPx.toDouble()
        )
    )
    return (containerWidthPx * scale).roundToInt().coerceAtLeast(1) to
        (containerHeightPx * scale).roundToInt().coerceAtLeast(1)
}
