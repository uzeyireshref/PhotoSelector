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
        assertEquals(512, GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX)
    }

    @Test
    fun highQualityDecodeWaitsUntilGridIsIdle() {
        assertEquals(
            false,
            shouldRequestHighQualityGalleryThumbnail(
                isScrollInProgress = true,
                hasCachedHighQualityThumbnail = false
            )
        )
        assertEquals(
            true,
            shouldRequestHighQualityGalleryThumbnail(
                isScrollInProgress = false,
                hasCachedHighQualityThumbnail = false
            )
        )
        assertEquals(
            false,
            shouldRequestHighQualityGalleryThumbnail(
                isScrollInProgress = false,
                hasCachedHighQualityThumbnail = true
            )
        )
    }

    @Test
    fun fastThumbnailCacheKeyIncludesUriAndSize() {
        assertEquals(
            "content://folder/photo.jpg#320",
            fastThumbnailCacheKey(uri = "content://folder/photo.jpg", sizePx = 320)
        )
    }

    @Test
    fun highQualityThumbnailCacheKeyIsStableAndSeparateFromFastThumbnail() {
        assertEquals(
            "content://folder/photo.jpg#high-quality#512",
            highQualityGalleryThumbnailCacheKey(uri = "content://folder/photo.jpg")
        )
    }

    @Test
    fun coilDiskCacheUsesOneGigabyteLimit() {
        assertEquals(1_073_741_824L, IMAGE_DISK_CACHE_MAX_SIZE_BYTES)
    }
}
