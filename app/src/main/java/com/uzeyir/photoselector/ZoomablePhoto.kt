package com.uzeyir.photoselector

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
    var scale by remember(photo.uri, rotationDegrees) { mutableFloatStateOf(1f) }
    var offsetX by remember(photo.uri, rotationDegrees) { mutableFloatStateOf(0f) }
    var offsetY by remember(photo.uri, rotationDegrees) { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val viewportWidthPx = constraints.maxWidth
        val viewportHeightPx = constraints.maxHeight
        val (mediaWidthPx, mediaHeightPx) = rotatedMediaSize(
            viewportWidthPx,
            viewportHeightPx,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(photo.uri, rotationDegrees, viewportWidthPx, viewportHeightPx) {
                    detectTapGestures(
                        onTap = { onSingleTap() },
                        onDoubleTap = { tapPosition ->
                            onDoubleTapOrTransform()
                            if (scale > 1f) {
                                resetZoom()
                            } else {
                                val targetScale = MaxPhotoScale
                                val targetOffset = offsetAfterZoomAroundPoint(
                                    currentLayerOffset = Offset(offsetX, offsetY),
                                    tapPosition = tapPosition,
                                    viewportWidthPx = viewportWidthPx,
                                    viewportHeightPx = viewportHeightPx,
                                    oldScale = scale,
                                    newScale = targetScale,
                                    rotationDegrees = rotationDegrees
                                )
                                scale = targetScale
                                offsetX = targetOffset.x
                                offsetY = targetOffset.y
                            }
                        }
                    )
                }
                .pointerInput(photo.uri, rotationDegrees, viewportWidthPx, viewportHeightPx) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var pressed: Boolean
                        do {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            val pointerCount = event.changes.count { it.pressed }
                            if (pointerCount == 0) {
                                continue
                            }
                            if (!shouldHandlePhotoTransform(pointerCount = pointerCount, scale = scale)) {
                                continue
                            }

                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                onDoubleTapOrTransform()
                            }

                            val oldScale = scale
                            val newScale = coercedPhotoScale(scale * zoom)
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val zoomedOffset = offsetAfterZoomAroundPoint(
                                currentLayerOffset = Offset(offsetX, offsetY),
                                tapPosition = centroid,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                oldScale = oldScale,
                                newScale = newScale,
                                rotationDegrees = rotationDegrees
                            )
                            val pannedOffset = zoomedOffset + photoLayerPanDelta(pan, rotationDegrees)
                            val clampedOffset = clampedPhotoOffset(
                                layerOffset = pannedOffset,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                scale = newScale,
                                rotationDegrees = rotationDegrees
                            )
                            scale = newScale
                            offsetX = clampedOffset.x
                            offsetY = clampedOffset.y

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
}
