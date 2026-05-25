package com.uzeyir.photoselector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGesturePolicyTest {
    @Test
    fun singleFingerAtDefaultZoomIsLeftForPagerSwipe() {
        assertFalse(shouldHandlePhotoTransform(pointerCount = 1, scale = 1f))
    }

    @Test
    fun twoFingerGestureAtDefaultZoomIsHandledForPinchZoom() {
        assertTrue(shouldHandlePhotoTransform(pointerCount = 2, scale = 1f))
    }

    @Test
    fun singleFingerAtZoomedScaleIsHandledForPan() {
        assertTrue(shouldHandlePhotoTransform(pointerCount = 1, scale = 2.5f))
    }
}
