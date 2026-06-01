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

class RawFileMatcherTest {

    @Test
    fun exportSummaryCountsLikedJpgsAndMatchingRawFiles() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(3)
        viewModel.setPhotos(photos)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("photo_1.CR3"),
                testDocument("photo_1.NEF"),
                testDocument("photo_2.CR3"),
                testDocument("photo_3.txt")
            )
        )
        viewModel.toggleLike(photos[0].uri)
        viewModel.toggleLike(photos[2].uri)

        val summary = viewModel.exportSummary

        assertEquals(2, summary.selectedJpgCount)
        assertEquals(2, summary.matchedRawCount)
        assertEquals(4, summary.totalFileCount)
    }

    @Test
    fun exportSummaryCountsSelectedVideosWithoutRawMatches() {
        val viewModel = PhotoViewModel()
        val media = listOf(
            testPhoto("clip_1.jpg"),
            testVideo("clip_1.mp4"),
            testVideo("movie.mov")
        )
        viewModel.setMediaItems(media)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("clip_1.CR3"),
                testDocument("movie.CR3")
            )
        )
        media.forEach { viewModel.toggleLike(it.uri) }

        val summary = viewModel.exportSummary

        assertEquals(1, summary.selectedJpgCount)
        assertEquals(1, summary.matchedRawCount)
        assertEquals(2, summary.selectedVideoCount)
        assertEquals(4, summary.totalFileCount)
    }

    @Test
    fun selectedTransferFilesIncludesRawFilesByDefault() {
        val viewModel = PhotoViewModel()
        val media = listOf(
            testPhoto("IMG_0001.JPG"),
            testVideo("clip.mp4")
        )
        viewModel.setMediaItems(media)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("IMG_0001.CR3"),
                testDocument("IMG_0002.CR3")
            )
        )
        media.forEach { viewModel.toggleLike(it.uri) }

        val files = viewModel.selectedTransferFiles(includeRawFiles = true)

        assertEquals(listOf("clip.mp4", "IMG_0001.JPG", "IMG_0001.CR3"), files.map { it.displayName })
    }

    @Test
    fun selectedTransferFilesCanExcludeRawFiles() {
        val viewModel = PhotoViewModel()
        val photo = testPhoto("IMG_0001.JPG")
        viewModel.setMediaItems(listOf(photo))
        viewModel.setFolderDocuments(listOf(testDocument("IMG_0001.CR3")))
        viewModel.toggleLike(photo.uri)

        val files = viewModel.selectedTransferFiles(includeRawFiles = false)

        assertEquals(listOf("IMG_0001.JPG"), files.map { it.displayName })
    }

    @Test
    fun matchingRawFilesAreCaseInsensitiveAndLimitedToSameBaseName() {
        val viewModel = PhotoViewModel()
        val photos = listOf(
            testPhoto("IMG_0001.JPG"),
            testPhoto("IMG_0002.JPG")
        )
        viewModel.setPhotos(photos)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("img_0001.arw"),
                testDocument("IMG_0002.CR2"),
                testDocument("IMG_00010.CR3")
            )
        )
        viewModel.toggleLike(photos[0].uri)

        val matches = viewModel.matchingRawFilesFor(photos[0])

        assertEquals(listOf("img_0001.arw"), matches.map { it.displayName })
    }
}
