package com.uzeyir.photoselector

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGesturePolicyTest {
    @Test
    fun singleFingerAtDefaultZoomIsLeftForPagerSwipe() {
        assertFalse(shouldHandlePhotoTransform(pointerCount = 1, scale = 1f))
    }

    @Test
    fun liftedPointersAreNeverHandledAsPhotoTransform() {
        assertFalse(shouldHandlePhotoTransform(pointerCount = 0, scale = 2.5f))
    }

    @Test
    fun twoFingerGestureAtDefaultZoomIsHandledForPinchZoom() {
        assertTrue(shouldHandlePhotoTransform(pointerCount = 2, scale = 1f))
    }

    @Test
    fun singleFingerAtZoomedScaleIsHandledForPan() {
        assertTrue(shouldHandlePhotoTransform(pointerCount = 1, scale = 2.5f))
    }

    @Test
    fun photoScaleIsCappedBeforeLayerGetsTooLargeForDeviceRendering() {
        assertEquals(2.5f, coercedPhotoScale(5f), 0.01f)
    }

    @Test
    fun photoOffsetIsClampedToScaledViewportBounds() {
        val clamped = clampedPhotoOffset(
            layerOffset = Offset(900f, -900f),
            viewportWidthPx = 1000,
            viewportHeightPx = 600,
            scale = 2f,
            rotationDegrees = 0
        )

        assertEquals(500f, clamped.x, 0.01f)
        assertEquals(-300f, clamped.y, 0.01f)
    }

    @Test
    fun photoOffsetResetsWhenScaleReturnsToOne() {
        val clamped = clampedPhotoOffset(
            layerOffset = Offset(120f, 80f),
            viewportWidthPx = 1000,
            viewportHeightPx = 600,
            scale = 1f,
            rotationDegrees = 0
        )

        assertEquals(Offset.Zero, clamped)
    }

    @Test
    fun doubleTapZoomUsesTappedPointInsteadOfViewportCenter() {
        val offset = offsetAfterZoomAroundPoint(
            currentLayerOffset = Offset.Zero,
            tapPosition = Offset(750f, 300f),
            viewportWidthPx = 1000,
            viewportHeightPx = 600,
            oldScale = 1f,
            newScale = 2f,
            rotationDegrees = 0
        )

        assertEquals(-250f, offset.x, 0.01f)
        assertEquals(0f, offset.y, 0.01f)
    }

    @Test
    fun panDeltaIsConvertedForQuarterTurnRotation() {
        val delta = photoLayerPanDelta(
            screenPan = Offset(20f, 0f),
            rotationDegrees = 90
        )

        assertEquals(0f, delta.x, 0.01f)
        assertEquals(-20f, delta.y, 0.01f)
    }

    @Test
    fun rotatedPhotoOffsetIsClampedInScreenSpace() {
        val clamped = clampedPhotoOffset(
            layerOffset = Offset(900f, 0f),
            viewportWidthPx = 1000,
            viewportHeightPx = 600,
            scale = 2f,
            rotationDegrees = 90
        )

        assertEquals(300f, clamped.x, 0.01f)
        assertEquals(0f, clamped.y, 0.01f)
    }
}
