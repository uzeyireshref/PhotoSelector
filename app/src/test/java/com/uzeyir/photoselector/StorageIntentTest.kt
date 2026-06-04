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

class StorageIntentTest {

    @Test
    fun sdCardDcimInitialUriIsBuiltFromStorageVolumeRootUri() {
        assertEquals(
            "content://com.android.externalstorage.documents/document/1234-5678%3ADCIM",
            sdCardDcimInitialUriStringFromRootUri(
                "content://com.android.externalstorage.documents/root/1234-5678"
            )
        )
    }

    @Test
    fun sdCardDcimInitialUriFallsBackForUnsupportedRootUri() {
        assertEquals(
            null,
            sdCardDcimInitialUriStringFromRootUri("content://other.provider/root/1234-5678")
        )
    }

    @Test
    fun sdCardInitialUriSupportsSavedRelativePath() {
        assertEquals(
            "content://com.android.externalstorage.documents/document/1234-5678%3ADCIM%2FSECILENLER",
            sdCardInitialUriStringFromRootUri(
                rootUri = "content://com.android.externalstorage.documents/root/1234-5678",
                relativePath = "DCIM/SECILENLER"
            )
        )
    }

    @Test
    fun sdCardVolumeIdIsReadFromStorageVolumeRootUri() {
        assertEquals(
            "1234-5678",
            sdCardVolumeIdFromRootUri("content://com.android.externalstorage.documents/root/1234-5678")
        )
    }
}
