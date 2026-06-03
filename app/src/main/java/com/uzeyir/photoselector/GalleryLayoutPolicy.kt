package com.uzeyir.photoselector

import kotlin.math.roundToInt

fun galleryColumnCountForWidthDp(widthDp: Int): Int =
    when {
        widthDp >= 1200 -> 6
        widthDp >= 900 -> 5
        widthDp >= 600 -> 4
        else -> 3
    }

fun thumbnailRequestSizePx(widthDp: Int, density: Float): Int {
    val columns = galleryColumnCountForWidthDp(widthDp)
    val contentPaddingDp = 16
    val cellWidthDp = (widthDp - contentPaddingDp).coerceAtLeast(columns) / columns.toFloat()
    return (cellWidthDp * density).roundToInt().coerceAtLeast(1)
}
