package com.uzeyir.photoselector

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uzeyir.photoselector.ui.theme.PhotoSelectorTheme
import org.junit.Rule
import org.junit.Test

class PhotoDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailControlsStartVisibleHideOnTapAndReappearWhenMediaChanges() {
        val media = listOf(
            detailPhoto("IMG_0001.JPG"),
            detailPhoto("IMG_0002.JPG")
        )
        var selectedIndex by mutableIntStateOf(0)

        composeRule.setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                PhotoDetailScreen(
                    photos = media,
                    selectedPhotoIndex = selectedIndex,
                    likedPhotos = emptyList(),
                    photoCount = 1,
                    videoCount = 0,
                    photoOriginalPrice = 300,
                    photoPayablePrice = 300,
                    videoOriginalPrice = 0,
                    videoPayablePrice = 0,
                    totalPayablePrice = 300,
                    strings = UiText.strings(AppLanguage.Turkish),
                    onBack = {},
                    onPhotoSelected = { selectedIndex = it },
                    rotationFor = { 0 },
                    onRotate = {},
                    onLikeToggle = {},
                    onReviewClick = {}
                )
            }
        }

        composeRule.onNodeWithText("300 TL").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("IMG_0001.JPG").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("300 TL").assertIsNotDisplayed()

        composeRule.runOnIdle {
            selectedIndex = 1
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("300 TL").assertIsDisplayed()
    }

    @Test
    fun videoControlsCanBeHiddenWithTap() {
        val media = listOf(detailVideo("clip_1.mp4"))

        composeRule.setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                PhotoDetailScreen(
                    photos = media,
                    selectedPhotoIndex = 0,
                    likedPhotos = emptyList(),
                    photoCount = 0,
                    videoCount = 1,
                    photoOriginalPrice = 0,
                    photoPayablePrice = 0,
                    videoOriginalPrice = 1000,
                    videoPayablePrice = 1000,
                    totalPayablePrice = 1000,
                    strings = UiText.strings(AppLanguage.Turkish),
                    onBack = {},
                    onPhotoSelected = {},
                    rotationFor = { 0 },
                    onRotate = {},
                    onLikeToggle = {},
                    onReviewClick = {}
                )
            }
        }

        composeRule.onNodeWithText("1000 TL").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("clip_1.mp4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1000 TL").assertIsNotDisplayed()
    }

    private fun detailPhoto(displayName: String): MediaItemData =
        MediaItemData(
            uri = Uri.parse("content://test/$displayName"),
            displayName = displayName,
            mimeType = "image/jpeg",
            mediaType = MediaType.Photo
        )

    private fun detailVideo(displayName: String): MediaItemData =
        MediaItemData(
            uri = Uri.parse("content://test/$displayName"),
            displayName = displayName,
            mimeType = "video/mp4",
            mediaType = MediaType.Video
        )
}
