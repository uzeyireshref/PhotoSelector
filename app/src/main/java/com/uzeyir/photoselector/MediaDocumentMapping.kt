package com.uzeyir.photoselector

import java.util.Locale

internal data class FolderLoadResult(
    val documents: List<FolderDocumentData>,
    val mediaItems: List<MediaItemData>
)

enum class PhotoViewerSource {
    Gallery,
    Favorites
}

fun FolderDocumentData.toMediaItemOrNull(): MediaItemData? {
    if (displayName.isHiddenDocumentName()) return null

    val type = when {
        isJpegDocument() -> MediaType.Photo
        isVideoDocument() -> MediaType.Video
        else -> null
    } ?: return null

    return MediaItemData(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        mediaType = type,
        lastModified = lastModified,
        sizeBytes = sizeBytes
    )
}

fun sortMediaItemsForGallery(mediaItems: List<MediaItemData>): List<MediaItemData> =
    mediaItems.sortedWith(
        compareBy<MediaItemData> { if (it.mediaType == MediaType.Video) 0 else 1 }
            .thenByDescending { it.lastModified }
            .thenByDescending { it.displayName.lowercase(Locale.US) }
    )

private fun FolderDocumentData.isJpegDocument(): Boolean =
    mimeType.equals("image/jpeg", ignoreCase = true) ||
        displayName.extension().lowercase(Locale.US) in setOf("jpg", "jpeg")

private fun FolderDocumentData.isVideoDocument(): Boolean =
    mimeType.startsWith("video/", ignoreCase = true) ||
        displayName.extension().lowercase(Locale.US) in videoExtensions

internal fun String.baseNameKey(): String =
    baseName().lowercase(Locale.US)

internal fun String.extension(): String =
    substringAfterLast('.', "")

private fun String.baseName(): String =
    substringBeforeLast('.', this)

private fun String.isHiddenDocumentName(): Boolean =
    startsWith(".")

private val videoExtensions = setOf(
    "mp4",
    "m4v",
    "mov",
    "avi",
    "mkv",
    "webm",
    "3gp",
    "3gpp",
    "mts",
    "m2ts",
    "ts",
    "wmv",
    "flv"
)
