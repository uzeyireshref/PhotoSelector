package com.uzeyir.photoselector

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoPagerPolicyTest {
    @Test
    fun wrappedPhotoIndexLoopsPastLastPageToFirstItem() {
        assertEquals(0, wrappedPhotoIndex(page = 3, itemCount = 3))
        assertEquals(1, wrappedPhotoIndex(page = 4, itemCount = 3))
    }

    @Test
    fun wrappedPhotoIndexLoopsBeforeFirstPageToLastItem() {
        assertEquals(2, wrappedPhotoIndex(page = -1, itemCount = 3))
    }

    @Test
    fun loopingInitialPageStartsNearMiddleAtSelectedItem() {
        val page = loopingPhotoPagerInitialPage(itemCount = 4, selectedIndex = 2)

        assertEquals(2, wrappedPhotoIndex(page, 4))
    }

    @Test
    fun nearestLoopingPageChoosesShortestMoveToTarget() {
        val current = loopingPhotoPagerInitialPage(itemCount = 4, selectedIndex = 3)
        val target = nearestLoopingPhotoPage(
            currentPage = current,
            targetIndex = 0,
            itemCount = 4
        )

        assertEquals(current + 1, target)
        assertEquals(0, wrappedPhotoIndex(target, 4))
    }
}
