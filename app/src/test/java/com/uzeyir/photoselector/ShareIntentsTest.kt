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

class ShareIntentsTest {

    @Test
    fun whatsAppDocumentShareRequestUsesDocumentMimeAndAllUris() {
        val files = listOf(
            testDocument("IMG_0001.JPG", mimeType = "image/jpeg"),
            testDocument("IMG_0001.CR3")
        )

        val request = whatsAppDocumentShareRequest(files)

        assertEquals("application/octet-stream", request.mimeType)
        assertEquals("com.whatsapp", request.packageName)
        assertEquals(
            listOf(FakeUri("IMG_0001.JPG"), FakeUri("IMG_0001.CR3")),
            request.uris
        )
    }

    @Test
    fun whatsAppDocumentSharePackageCandidatesIncludeBusinessVariant() {
        assertEquals(
            listOf("com.whatsapp", "com.whatsapp.w4b"),
            whatsAppPackageCandidates()
        )
    }

    @Test
    fun documentShareCacheFileNameKeepsExtensionAndRemovesPathSeparators() {
        assertEquals("001_IMG_0001.JPG", documentShareCacheFileName("../IMG/0001.JPG", 0))
        assertEquals("012_IMG_0001.CR3", documentShareCacheFileName("IMG_0001.CR3", 11))
    }

    @Test
    fun sharePreparingStatusReportsOverallAndCurrentFileProgress() {
        val status = ShareStatus.Preparing(
            preparedFiles = 1,
            totalFiles = 3,
            preparedBytes = 750L,
            totalBytes = 1_500L,
            currentFileName = "IMG_0002.CR3",
            currentFileBytes = 250L,
            currentFileTotalBytes = 500L
        )

        assertEquals(0.5f, status.progressFraction)
        assertEquals(0.5f, status.currentFileProgressFraction)
    }
}
