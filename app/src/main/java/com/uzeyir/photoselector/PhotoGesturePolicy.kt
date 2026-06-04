package com.uzeyir.photoselector

import androidx.compose.ui.geometry.Offset
import kotlin.math.max

internal const val MaxPhotoScale = 2.5f

internal fun shouldHandlePhotoTransform(pointerCount: Int, scale: Float): Boolean =
    pointerCount > 0 && (pointerCount > 1 || scale > 1f)

internal fun coercedPhotoScale(scale: Float): Float =
    scale.coerceIn(1f, MaxPhotoScale)

internal fun photoLayerPanDelta(screenPan: Offset, rotationDegrees: Int): Offset =
    rotateOffset(screenPan, -normalizedRotation(rotationDegrees))

internal fun clampedPhotoOffset(
    layerOffset: Offset,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    scale: Float,
    rotationDegrees: Int
): Offset {
    if (scale <= 1f || viewportWidthPx <= 0 || viewportHeightPx <= 0) return Offset.Zero

    val screenOffset = rotateOffset(layerOffset, normalizedRotation(rotationDegrees))
    val maxX = max(0f, viewportWidthPx * (scale - 1f) / 2f)
    val maxY = max(0f, viewportHeightPx * (scale - 1f) / 2f)
    val clampedScreenOffset = Offset(
        x = screenOffset.x.coerceIn(-maxX, maxX),
        y = screenOffset.y.coerceIn(-maxY, maxY)
    )
    return rotateOffset(clampedScreenOffset, -normalizedRotation(rotationDegrees))
}

internal fun offsetAfterZoomAroundPoint(
    currentLayerOffset: Offset,
    tapPosition: Offset,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    oldScale: Float,
    newScale: Float,
    rotationDegrees: Int
): Offset {
    if (newScale <= 1f || viewportWidthPx <= 0 || viewportHeightPx <= 0) return Offset.Zero

    val safeOldScale = oldScale.coerceAtLeast(1f)
    val zoomFactor = newScale / safeOldScale
    val focalPointFromCenter = Offset(
        x = tapPosition.x - viewportWidthPx / 2f,
        y = tapPosition.y - viewportHeightPx / 2f
    )
    val focalPointInLayer = photoLayerPanDelta(focalPointFromCenter, rotationDegrees)
    val rawOffset = currentLayerOffset * zoomFactor + focalPointInLayer * (1f - zoomFactor)
    return clampedPhotoOffset(
        layerOffset = rawOffset,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        scale = newScale,
        rotationDegrees = rotationDegrees
    )
}

private fun normalizedRotation(rotationDegrees: Int): Int =
    ((rotationDegrees % 360) + 360) % 360

private fun rotateOffset(offset: Offset, rotationDegrees: Int): Offset =
    when (normalizedRotation(rotationDegrees)) {
        90 -> Offset(-offset.y, offset.x)
        180 -> Offset(-offset.x, -offset.y)
        270 -> Offset(offset.y, -offset.x)
        else -> offset
    }
