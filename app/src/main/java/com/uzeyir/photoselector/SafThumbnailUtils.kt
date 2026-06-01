package com.uzeyir.photoselector

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

const val GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX = 1024
private const val FAST_THUMBNAIL_MAX_SIZE_PX = 512

fun fastThumbnailRequestSizePx(sizePx: Int): Int =
    sizePx.coerceIn(1, FAST_THUMBNAIL_MAX_SIZE_PX)

suspend fun loadFastSafThumbnail(
    contentResolver: ContentResolver,
    uri: Uri,
    sizePx: Int
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val boundedSizePx = fastThumbnailRequestSizePx(sizePx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(uri, Size(boundedSizePx, boundedSizePx), CancellationSignal())
        } else {
            @Suppress("DEPRECATION")
            DocumentsContract.getDocumentThumbnail(
                contentResolver,
                uri,
                Point(boundedSizePx, boundedSizePx),
                CancellationSignal()
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        null
    }
}
