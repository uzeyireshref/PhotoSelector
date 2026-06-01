package com.uzeyir.photoselector

import android.net.FakeUri
import android.net.Uri

internal fun testPhotos(count: Int): List<MediaItemData> =
    (1..count).map { index ->
        testPhoto("photo_$index.jpg")
    }

internal fun testVideos(count: Int): List<MediaItemData> =
    (1..count).map { index ->
        testVideo("video_$index.mp4")
    }

internal fun testPhoto(displayName: String, lastModified: Long = 0): MediaItemData =
    MediaItemData(
        uri = FakeUri(displayName),
        displayName = displayName,
        mimeType = "image/jpeg",
        mediaType = MediaType.Photo,
        lastModified = lastModified
    )

internal fun testVideo(displayName: String, lastModified: Long = 0): MediaItemData =
    MediaItemData(
        uri = FakeUri(displayName),
        displayName = displayName,
        mimeType = "video/mp4",
        mediaType = MediaType.Video,
        lastModified = lastModified
    )

internal fun testDocument(
    displayName: String,
    mimeType: String = "application/octet-stream",
    lastModified: Long = 0
): FolderDocumentData =
    FolderDocumentData(
        uri = FakeUri(displayName),
        displayName = displayName,
        mimeType = mimeType,
        lastModified = lastModified
    )

internal class FakeLastFolderStore : LastFolderStore {
    var savedUri: String? = null

    override fun save(folderUri: String) {
        savedUri = folderUri
    }

    override fun clear() {
        savedUri = null
    }

    override fun resolveAvailableFolder(persistedReadUris: Set<String>, persistedWriteUris: Set<String>): String? {
        val current = savedUri
        return if (current != null && current in persistedReadUris && current in persistedWriteUris) {
            current
        } else {
            clear()
            null
        }
    }
}
