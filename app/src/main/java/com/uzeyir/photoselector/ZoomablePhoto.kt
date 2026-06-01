package com.uzeyir.photoselector

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ZoomablePhoto(
    photo: MediaItemData,
    rotationDegrees: Int,
    onSingleTap: () -> Unit,
    onDoubleTapOrTransform: () -> Unit
) {
    var scale by remember(photo.uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(photo.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(photo.uri) { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photo.uri) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        onDoubleTapOrTransform()
                        if (scale > 1f) {
                            resetZoom()
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(photo.uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pressed: Boolean
                    do {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                        val pointerCount = event.changes.count { it.pressed }
                        if (!shouldHandlePhotoTransform(pointerCount = pointerCount, scale = scale)) {
                            continue
                        }

                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (zoom != 1f || pan != Offset.Zero) {
                            onDoubleTapOrTransform()
                        }

                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }

                        event.changes.forEach { change ->
                            if (change.positionChanged()) {
                                change.consume()
                            }
                        }
                    } while (pressed)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val (mediaWidthPx, mediaHeightPx) = rotatedMediaSize(
            constraints.maxWidth,
            constraints.maxHeight,
            rotationDegrees
        )
        val mediaWidth = if (mediaWidthPx == constraints.maxWidth) maxWidth else maxHeight
        val mediaHeight = if (mediaHeightPx == constraints.maxHeight) maxHeight else maxWidth
        val imageRequest = remember(photo.uri, mediaWidthPx, mediaHeightPx) {
            ImageRequest.Builder(context)
                .data(photo.uri)
                .size(mediaWidthPx.coerceAtLeast(1), mediaHeightPx.coerceAtLeast(1))
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = photo.displayName,
            modifier = Modifier
                .requiredSize(width = mediaWidth, height = mediaHeight)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotationDegrees.toFloat()
                ),
            contentScale = ContentScale.Fit
        )
    }
}
