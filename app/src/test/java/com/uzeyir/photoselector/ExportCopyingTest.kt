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

class ExportCopyingTest {

    @Test
    fun copyDocumentBytesCopiesNonEmptyStreams() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4)
        val destination = ByteArrayOutputStream()
        val progress = mutableListOf<Long>()

        val copiedBytes = copyDocumentBytes(
            input = ByteArrayInputStream(sourceBytes),
            output = destination,
            displayName = "IMG_0001.JPG",
            onProgress = { copied, _ -> progress.add(copied) }
        )

        assertEquals(4L, copiedBytes)
        assertArrayEquals(sourceBytes, destination.toByteArray())
        assertEquals(4L, progress.last())
    }

    @Test
    fun copyDocumentBytesRejectsZeroByteCopies() {
        val error = assertThrows(IllegalStateException::class.java) {
            copyDocumentBytes(
                input = ByteArrayInputStream(ByteArray(0)),
                output = ByteArrayOutputStream(),
                displayName = "IMG_0001.JPG"
            )
        }

        assertEquals(UiMessage.CopyVerificationFailed.name, error.message)
    }

    @Test
    fun exportCannotStartWhileCopying() {
        assertEquals(false, shouldBeginExport(ExportStatus.Copying()))
        assertEquals(true, shouldBeginExport(ExportStatus.Idle))
        assertEquals(true, shouldBeginExport(ExportStatus.Success(folderName = "done", copiedFiles = 1)))
    }

    @Test
    fun exportProgressFractionIsBoundedByCopiedAndTotalFiles() {
        assertEquals(0f, ExportStatus.Copying(copiedBytes = 0, totalBytes = 400).progressFraction)
        assertEquals(0.5f, ExportStatus.Copying(copiedBytes = 200, totalBytes = 400).progressFraction)
        assertEquals(1f, ExportStatus.Copying(copiedBytes = 500, totalBytes = 400).progressFraction)
        assertEquals(null, ExportStatus.Copying(copiedBytes = 0, totalBytes = null).progressFraction)
    }

    @Test
    fun activeFileProgressFractionUsesCurrentFileByteCountsWhenSizeIsKnown() {
        val status = ExportStatus.Copying(
            currentFileName = "video.mp4",
            currentFileBytes = 512,
            currentFileTotalBytes = 1024
        )

        assertEquals(0.5f, status.currentFileProgressFraction)
    }

    @Test
    fun exportProgressLabelShowsCopiedAndTotalCounts() {
        assertEquals("2/5", exportProgressCountLabel(ExportStatus.Copying(copiedFiles = 2, totalFiles = 5)))
        assertEquals(null, exportProgressCountLabel(ExportStatus.Copying()))
    }

    @Test
    fun failedExportCleanupDeletesCreatedDocumentsAndFolderInReverseOrder() {
        val firstFile = FakeUri("export/photo_1.jpg")
        val secondFile = FakeUri("export/photo_2.jpg")
        val exportFolder = FakeUri("export")
        val deleted = mutableListOf<String>()

        cleanupCreatedExportDocuments(
            createdFileUris = listOf(firstFile, secondFile),
            exportFolderUri = exportFolder
        ) { uri ->
            deleted.add(uri.toString())
            true
        }

        assertEquals(
            listOf(
                secondFile.toString(),
                firstFile.toString(),
                exportFolder.toString()
            ),
            deleted
        )
    }

    @Test
    fun exportFolderTimestampUsesHourMinuteSecondThenYearMonthDayFormat() {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MAY, 29, 19, 26, 0)
        }

        val timestamp = formatExportFolderTimestamp(calendar.time)

        assertEquals("19-26-00_2026-05-29", timestamp)
    }

    @Test
    fun exportFolderNameUsesTimestampWithoutPrefixForFirstAttempt() {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MAY, 29, 19, 26, 0)
        }

        val folderName = exportFolderName(calendar.time)

        assertEquals("19-26-00_2026-05-29", folderName)
    }

    @Test
    fun exportFolderNameAddsSuffixForRetryAttempts() {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MAY, 29, 19, 26, 0)
        }

        assertEquals("19-26-00_2026-05-29_2", exportFolderName(calendar.time, attempt = 2))
        assertEquals("19-26-00_2026-05-29_3", exportFolderName(calendar.time, attempt = 3))
    }
}
