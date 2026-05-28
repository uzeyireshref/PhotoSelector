package com.uzeyir.photoselector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.uzeyir.photoselector.ui.theme.PhotoSelectorTheme
import org.junit.Rule
import org.junit.Test

class FolderSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun languageSelectorChangesStartScreenTextToEnglish() {
        var language by mutableStateOf(AppLanguage.Turkish)

        composeRule.setContent {
            val strings = UiText.strings(language)
            PhotoSelectorTheme(dynamicColor = false) {
                FolderSelectionScreen(
                    onFolderSelected = {},
                    onOpenSdCard = {},
                    updateStatus = AppUpdateStatus.Idle,
                    isLoadingMedia = false,
                    language = language,
                    onLanguageSelected = { language = it },
                    strings = strings,
                    onCheckUpdate = {}
                )
            }
        }

        composeRule.onNodeWithText("English").performTouchInput {
            click(center)
        }

        composeRule.onNodeWithText("Select Photo Folder").assertIsDisplayed()
        composeRule.onNodeWithText("Select Folder").assertIsDisplayed()
    }

    @Test
    fun galleryScreenAcceptsExternalGridState() {
        composeRule.setContent {
            val gridState = rememberLazyGridState()
            PhotoSelectorTheme(dynamicColor = false) {
                GalleryScreen(
                    photos = listOf(galleryPhoto("IMG_0001.JPG")),
                    likedPhotoUris = emptySet(),
                    strings = UiText.strings(AppLanguage.Turkish),
                    gridState = gridState,
                    onPhotoClick = {},
                    onLikeToggle = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("IMG_0001.JPG").assertIsDisplayed()
    }

    private fun galleryPhoto(displayName: String): MediaItemData =
        MediaItemData(
            uri = android.net.Uri.parse("content://test/$displayName"),
            displayName = displayName,
            mimeType = "image/jpeg",
            mediaType = MediaType.Photo
        )
}
