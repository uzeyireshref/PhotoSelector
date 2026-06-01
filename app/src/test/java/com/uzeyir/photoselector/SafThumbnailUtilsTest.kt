package com.uzeyir.photoselector

import org.junit.Assert.assertEquals
import org.junit.Test

class SafThumbnailUtilsTest {

    @Test
    fun fastThumbnailSizeIsBoundedForSafRequests() {
        assertEquals(1, fastThumbnailRequestSizePx(0))
        assertEquals(320, fastThumbnailRequestSizePx(320))
        assertEquals(512, fastThumbnailRequestSizePx(2048))
    }

    @Test
    fun galleryHighQualityImageRequestUsesFixedSize() {
        assertEquals(1024, GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX)
    }

    @Test
    fun coilDiskCacheUsesOneGigabyteLimit() {
        assertEquals(1_073_741_824L, IMAGE_DISK_CACHE_MAX_SIZE_BYTES)
    }
}
