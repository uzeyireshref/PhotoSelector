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

class VideoPlaybackPolicyTest {

    @Test
    fun rotatedMediaSizeSwapsContainerForQuarterTurns() {
        assertEquals(1080 to 1920, rotatedMediaSize(1080, 1920, 0))
        assertEquals(1080 to 1920, rotatedMediaSize(1080, 1920, 180))
        assertEquals(1920 to 1080, rotatedMediaSize(1080, 1920, 90))
        assertEquals(1920 to 1080, rotatedMediaSize(1080, 1920, 270))
    }

    @Test
    fun rotatedMediaSizeLeavesInvalidContainersUnchanged() {
        assertEquals(0 to 1000, rotatedMediaSize(0, 1000, 90))
        assertEquals(1000 to 0, rotatedMediaSize(1000, 0, 90))
    }

    @Test
    fun videoFullscreenRotationTurnsOnlyLandscapeVideoSideways() {
        assertEquals(90, videoFullscreenRotationDegrees(videoWidth = 1920, videoHeight = 1080))
        assertEquals(0, videoFullscreenRotationDegrees(videoWidth = 1080, videoHeight = 1920))
        assertEquals(0, videoFullscreenRotationDegrees(videoWidth = 0, videoHeight = 0))
    }

    @Test
    fun fullscreenVideoSurfaceSizePreservesLandscapeAspectAfterRotation() {
        assertEquals(
            2113 to 1189,
            fullscreenVideoSurfaceSize(
                containerWidthPx = 1080,
                containerHeightPx = 2113,
                videoWidth = 1920,
                videoHeight = 1080,
                rotationDegrees = 90
            )
        )
    }

    @Test
    fun sharedVideoSessionOpensFullscreenImmediately() {
        val videoUri = FakeUri("videos/clip.mp4")
        val initial = VideoPlaybackSession()

        val fullscreen = initial.playFullscreen(videoUri)
        val exited = fullscreen.exitFullscreen()

        assertEquals(videoUri, fullscreen.activeUri)
        assertEquals(videoUri, fullscreen.fullscreenUri)
        assertEquals(null, exited.activeUri)
        assertEquals(null, exited.fullscreenUri)
    }

    @Test
    fun sharedVideoSessionClearsWhenPageChanges() {
        val first = FakeUri("videos/first.mp4")
        val second = FakeUri("videos/second.mp4")
        val session = VideoPlaybackSession(activeUri = first, fullscreenUri = first)

        val cleared = session.clearIfDifferentPage(second)

        assertEquals(null, cleared.activeUri)
        assertEquals(null, cleared.fullscreenUri)
    }

    @Test
    fun videoFullscreenExitsWhenPlaybackEnds() {
        assertEquals(false, shouldExitFullscreenForPlaybackState(Player.STATE_READY))
        assertEquals(false, shouldExitFullscreenForPlaybackState(Player.STATE_BUFFERING))
        assertEquals(true, shouldExitFullscreenForPlaybackState(Player.STATE_ENDED))
    }

    @Test
    fun videoFullscreenShowsLoadingOnlyBeforeReady() {
        assertEquals(true, shouldShowFullscreenVideoLoading(Player.STATE_IDLE))
        assertEquals(true, shouldShowFullscreenVideoLoading(Player.STATE_BUFFERING))
        assertEquals(false, shouldShowFullscreenVideoLoading(Player.STATE_READY))
        assertEquals(false, shouldShowFullscreenVideoLoading(Player.STATE_ENDED))
    }

    @Test
    fun videoPlaybackStateSelectsActiveAndFullscreenMedia() {
        val first = testVideo("first.mp4")
        val second = testVideo("second.mp4")
        val session = VideoPlaybackSession().playFullscreen(second.uri)

        val state = videoPlaybackStateFor(listOf(first, second), session)

        assertEquals(second, state.activeMedia)
        assertEquals(second, state.fullscreenMedia)
        assertEquals(true, state.isFullscreen)
    }

    @Test
    fun videoPlaybackStateIgnoresMissingSessionMedia() {
        val media = testVideo("clip.mp4")
        val session = VideoPlaybackSession().playFullscreen(FakeUri("missing.mp4"))

        val state = videoPlaybackStateFor(listOf(media), session)

        assertEquals(null, state.activeMedia)
        assertEquals(null, state.fullscreenMedia)
        assertEquals(false, state.isFullscreen)
    }

    @Test
    fun videoPosterRequestSizeCapsLargeSurfacesWhileKeepingAspect() {
        assertEquals(640 to 360, videoPosterRequestSizePx(containerWidthPx = 1920, containerHeightPx = 1080))
        assertEquals(360 to 640, videoPosterRequestSizePx(containerWidthPx = 1080, containerHeightPx = 1920))
    }

    @Test
    fun videoPosterRequestSizeDoesNotUpscaleSmallOrInvalidSurfaces() {
        assertEquals(320 to 180, videoPosterRequestSizePx(containerWidthPx = 320, containerHeightPx = 180))
        assertEquals(1 to 1, videoPosterRequestSizePx(containerWidthPx = 0, containerHeightPx = 180))
        assertEquals(1 to 1, videoPosterRequestSizePx(containerWidthPx = 320, containerHeightPx = 0))
    }
}
