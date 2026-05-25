package com.uzeyir.photoselector

internal fun shouldHandlePhotoTransform(pointerCount: Int, scale: Float): Boolean =
    pointerCount > 1 || scale > 1f
