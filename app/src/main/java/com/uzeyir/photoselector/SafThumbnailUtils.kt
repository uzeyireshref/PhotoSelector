package com.uzeyir.photoselector

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

const val GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX = 512
private const val FAST_THUMBNAIL_MAX_SIZE_PX = 512
private const val FAST_THUMBNAIL_MEMORY_CACHE_MAX_BYTES = 64 * 1024 * 1024
private const val HIGH_QUALITY_THUMBNAIL_MEMORY_CACHE_MAX_ITEMS = 600

fun fastThumbnailRequestSizePx(sizePx: Int): Int =
    sizePx.coerceIn(1, FAST_THUMBNAIL_MAX_SIZE_PX)

fun shouldRequestHighQualityGalleryThumbnail(
    isScrollInProgress: Boolean,
    hasCachedHighQualityThumbnail: Boolean
): Boolean =
    !isScrollInProgress && !hasCachedHighQualityThumbnail

fun fastThumbnailCacheKey(uri: String, sizePx: Int): String =
    "$uri#$sizePx"

fun highQualityGalleryThumbnailCacheKey(uri: String): String =
    "$uri#high-quality#$GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX"

private val fastThumbnailMemoryCache = object : LruCache<String, Bitmap>(FAST_THUMBNAIL_MEMORY_CACHE_MAX_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private val highQualityThumbnailPainterMemoryCache =
    object : LruCache<String, Painter>(HIGH_QUALITY_THUMBNAIL_MEMORY_CACHE_MAX_ITEMS) {
        override fun sizeOf(key: String, value: Painter): Int = 1
    }

fun cachedHighQualityGalleryThumbnailPainter(uri: Uri): Painter? =
    highQualityThumbnailPainterMemoryCache.get(highQualityGalleryThumbnailCacheKey(uri.toString()))

fun cacheHighQualityGalleryThumbnailPainter(uri: Uri, painter: Painter) {
    highQualityThumbnailPainterMemoryCache.put(highQualityGalleryThumbnailCacheKey(uri.toString()), painter)
}

suspend fun loadFastSafThumbnail(
    contentResolver: ContentResolver,
    uri: Uri,
    sizePx: Int
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val boundedSizePx = fastThumbnailRequestSizePx(sizePx)
        val cacheKey = fastThumbnailCacheKey(uri.toString(), boundedSizePx)
        fastThumbnailMemoryCache.get(cacheKey)?.let { return@withContext it }

        val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        thumbnail?.also { fastThumbnailMemoryCache.put(cacheKey, it) }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        null
    }
}
