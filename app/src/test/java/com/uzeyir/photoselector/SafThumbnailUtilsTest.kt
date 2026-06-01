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
    fun highQualityThumbnailCacheKeyPrefersStableMediaMetadata() {
        val firstUri = android.net.FakeUri("content://first/photo.jpg")
        val secondUri = android.net.FakeUri("content://second/photo.jpg")
        val first = MediaItemData(
            uri = firstUri,
            displayName = "IMG_001.jpg",
            lastModified = 1234L,
            sizeBytes = 5_678L
        )
        val second = first.copy(uri = secondUri)

        assertEquals(highQualityGalleryThumbnailCacheKey(first), highQualityGalleryThumbnailCacheKey(second))
    }

    @Test
    fun highQualityDiskCacheFileNameIsStableAndSafeForFileSystem() {
        val fileName = highQualityGalleryThumbnailDiskCacheFileName(
            cacheKey = "content://folder/photo.jpg#high-quality#512"
        )

        assertEquals(68, fileName.length)
        assertEquals(true, fileName.endsWith(".jpg"))
        assertEquals(true, fileName.removeSuffix(".jpg").all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun coilDiskCacheUsesOneGigabyteLimit() {
        assertEquals(1_073_741_824L, IMAGE_DISK_CACHE_MAX_SIZE_BYTES)
    }
}
