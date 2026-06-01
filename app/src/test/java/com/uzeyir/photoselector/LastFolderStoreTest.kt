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

class LastFolderStoreTest {

    @Test
    fun lastFolderStoreResolvesOnlyWhenReadAndWritePermissionsPersisted() {
        val store = FakeLastFolderStore()
        val folderUri = "content://tree/sdcard"
        store.save(folderUri)

        assertEquals(
            folderUri,
            store.resolveAvailableFolder(
                persistedReadUris = setOf(folderUri),
                persistedWriteUris = setOf(folderUri)
            )
        )
        assertEquals(
            null,
            store.resolveAvailableFolder(
                persistedReadUris = setOf(folderUri),
                persistedWriteUris = emptySet()
            )
        )
    }

    @Test
    fun lastFolderStoreClearsUnavailableFolder() {
        val store = FakeLastFolderStore()
        store.save("content://tree/missing")

        val resolved = store.resolveAvailableFolder(
            persistedReadUris = emptySet(),
            persistedWriteUris = emptySet()
        )

        assertEquals(null, resolved)
        assertEquals(null, store.savedUri)
    }
}
