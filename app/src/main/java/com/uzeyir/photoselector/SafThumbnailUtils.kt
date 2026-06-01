package com.uzeyir.photoselector

import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.File
import java.util.concurrent.CancellationException
import java.security.MessageDigest

const val GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX = 512
private const val FAST_THUMBNAIL_MAX_SIZE_PX = 512
private const val FAST_THUMBNAIL_MEMORY_CACHE_MAX_BYTES = 64 * 1024 * 1024
private const val HIGH_QUALITY_THUMBNAIL_MEMORY_CACHE_MAX_ITEMS = 600
private const val HIGH_QUALITY_THUMBNAIL_DISK_CACHE_DIR = "gallery_thumbnail_cache"
private const val HIGH_QUALITY_THUMBNAIL_JPEG_QUALITY = 92

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

fun highQualityGalleryThumbnailCacheKey(media: MediaItemData): String {
    val stableSize = media.sizeBytes
    val stableModified = media.lastModified.takeIf { it > 0L }
    return if (stableSize != null || stableModified != null) {
        "${media.displayName}#${stableSize ?: "unknown-size"}#${stableModified ?: "unknown-modified"}#high-quality#$GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX"
    } else {
        highQualityGalleryThumbnailCacheKey(media.uri.toString())
    }
}

fun highQualityGalleryThumbnailDiskCacheFileName(cacheKey: String): String =
    cacheKey.sha256Hex() + ".jpg"

private val fastThumbnailMemoryCache = object : LruCache<String, Bitmap>(FAST_THUMBNAIL_MEMORY_CACHE_MAX_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private val highQualityThumbnailPainterMemoryCache =
    object : LruCache<String, Painter>(HIGH_QUALITY_THUMBNAIL_MEMORY_CACHE_MAX_ITEMS) {
        override fun sizeOf(key: String, value: Painter): Int = 1
    }

fun cachedHighQualityGalleryThumbnailPainter(cacheKey: String): Painter? =
    highQualityThumbnailPainterMemoryCache.get(cacheKey)

fun cacheHighQualityGalleryThumbnailPainter(cacheKey: String, painter: Painter) {
    highQualityThumbnailPainterMemoryCache.put(cacheKey, painter)
}

suspend fun loadCachedHighQualityGalleryThumbnailBitmap(context: Context, cacheKey: String): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val cacheFile = highQualityGalleryThumbnailDiskCacheFile(context, cacheKey)
            if (!cacheFile.isFile) return@withContext null
            BitmapFactory.decodeFile(cacheFile.absolutePath)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }
    }

suspend fun cacheHighQualityGalleryThumbnailBitmap(context: Context, cacheKey: String, bitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            val cacheFile = highQualityGalleryThumbnailDiskCacheFile(context, cacheKey)
            cacheFile.parentFile?.mkdirs()
            cacheFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, HIGH_QUALITY_THUMBNAIL_JPEG_QUALITY, output)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Disk cache is best-effort. UI already has the in-memory thumbnail.
        }
    }
}

private fun highQualityGalleryThumbnailDiskCacheFile(context: Context, cacheKey: String): File {
    return File(
        File(context.cacheDir, HIGH_QUALITY_THUMBNAIL_DISK_CACHE_DIR),
        highQualityGalleryThumbnailDiskCacheFileName(cacheKey)
    )
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

private fun String.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
