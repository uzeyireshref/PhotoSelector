package com.uzeyir.photoselector

import android.net.Uri

enum class MediaType {
    Photo,
    Video
}

data class MediaItemData(
    val uri: Uri,
    val displayName: String,
    val mimeType: String = "image/jpeg",
    val mediaType: MediaType = MediaType.Photo,
    val lastModified: Long = 0,
    val sizeBytes: Long? = null
)

data class FolderDocumentData(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val lastModified: Long = 0,
    val sizeBytes: Long? = null
)

data class ExportSummary(
    val selectedJpgCount: Int,
    val matchedRawCount: Int,
    val selectedVideoCount: Int = 0
) {
    val totalFileCount: Int
        get() = selectedJpgCount + matchedRawCount + selectedVideoCount
}
