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
}
