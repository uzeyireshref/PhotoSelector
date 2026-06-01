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

class GalleryLayoutPolicyTest {

    @Test
    fun galleryGridUsesMoreColumnsOnTabletWidth() {
        assertEquals(3, galleryColumnCountForWidthDp(411))
        assertEquals(3, galleryColumnCountForWidthDp(599))
        assertEquals(4, galleryColumnCountForWidthDp(600))
        assertEquals(4, galleryColumnCountForWidthDp(866))
    }

    @Test
    fun thumbnailRequestSizeFollowsGridCellWidth() {
        assertEquals(362, thumbnailRequestSizePx(widthDp = 411, density = 2.75f))
        assertEquals(452, thumbnailRequestSizePx(widthDp = 866, density = 2.125f))
    }
}
