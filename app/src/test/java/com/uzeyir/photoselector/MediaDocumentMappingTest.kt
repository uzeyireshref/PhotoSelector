package com.uzeyir.photoselector

import android.net.FakeUri
import android.net.Uri
import androidx.media3.common.Player
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MediaDocumentMappingTest {

    @Test
    fun appleDoubleMetadataFilesAreNotLoadedAsMedia() {
        val metadataPhoto = FolderDocumentData(
            uri = FakeUri("folder/._9S8A7592.JPG"),
            displayName = "._9S8A7592.JPG",
            mimeType = "image/jpeg"
        )
        val metadataVideo = FolderDocumentData(
            uri = FakeUri("folder/._5R7A4816.MP4"),
            displayName = "._5R7A4816.MP4",
            mimeType = "video/mp4"
        )

        assertEquals(null, metadataPhoto.toMediaItemOrNull())
        assertEquals(null, metadataVideo.toMediaItemOrNull())
    }

    @Test
    fun mediaItemsKeepKnownDocumentSizeForExportVerification() {
        val document = FolderDocumentData(
            uri = FakeUri("folder/photo.jpg"),
            displayName = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1234L
        )

        assertEquals(1234L, document.toMediaItemOrNull()?.sizeBytes)
    }

    @Test
    fun mediaClassificationIncludesJpgAndVideosOnly() {
        val documents = listOf(
            testDocument("IMG_0001.JPG", "image/jpeg", lastModified = 10),
            testDocument("clip.MP4", "", lastModified = 20),
            testDocument("movie.mov", "application/octet-stream", lastModified = 30),
            testDocument("IMG_0001.CR3", "application/octet-stream", lastModified = 40),
            testDocument("notes.txt", "text/plain", lastModified = 50)
        )

        val media = documents.mapNotNull { it.toMediaItemOrNull() }

        assertEquals(
            listOf(MediaType.Photo, MediaType.Video, MediaType.Video),
            media.map { it.mediaType }
        )
        assertEquals(
            listOf("IMG_0001.JPG", "clip.MP4", "movie.mov"),
            media.map { it.displayName }
        )
    }

    @Test
    fun galleryMediaIsSortedByNewestLastModifiedFirst() {
        val media = listOf(
            testPhoto("old.jpg", lastModified = 100),
            testVideo("new.mp4", lastModified = 300),
            testPhoto("middle.jpg", lastModified = 200)
        )

        val sorted = sortMediaItemsForGallery(media)

        assertEquals(listOf("new.mp4", "middle.jpg", "old.jpg"), sorted.map { it.displayName })
    }

    @Test
    fun galleryMediaSortsVideosFirstThenEachGroupByNewest() {
        val media = listOf(
            testPhoto("newest-photo.jpg", lastModified = 500),
            testVideo("older-video.mp4", lastModified = 200),
            testPhoto("older-photo.jpg", lastModified = 100),
            testVideo("newest-video.mp4", lastModified = 300)
        )

        val sorted = sortMediaItemsForGallery(media)

        assertEquals(
            listOf("newest-video.mp4", "older-video.mp4", "newest-photo.jpg", "older-photo.jpg"),
            sorted.map { it.displayName }
        )
    }

    @Test
    fun galleryMediaWithNoLastModifiedFallsBackToReverseDisplayName() {
        val media = listOf(
            testPhoto("IMG_0001.JPG", lastModified = 0),
            testPhoto("IMG_0003.JPG", lastModified = 0),
            testVideo("IMG_0002.MP4", lastModified = 0)
        )

        val sorted = sortMediaItemsForGallery(media)

        assertEquals(listOf("IMG_0002.MP4", "IMG_0003.JPG", "IMG_0001.JPG"), sorted.map { it.displayName })
    }
}
