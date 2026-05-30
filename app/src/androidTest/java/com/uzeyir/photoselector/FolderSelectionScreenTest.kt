package com.uzeyir.photoselector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test
    fun galleryScreenSwitchesBetweenAllMediaAndFavorites() {
        val favorite = galleryPhoto("IMG_0001.JPG")
        val other = galleryPhoto("IMG_0002.JPG")

        composeRule.setContent {
            val gridState = rememberLazyGridState()
            PhotoSelectorTheme(dynamicColor = false) {
                GalleryScreen(
                    photos = listOf(favorite, other),
                    likedPhotoUris = setOf(favorite.uri),
                    strings = UiText.strings(AppLanguage.Turkish),
                    gridState = gridState,
                    onPhotoClick = {},
                    onLikeToggle = {}
                )
            }
        }

        composeRule.onNodeWithText("Tüm Fotoğraflar").assertIsDisplayed()
        composeRule.onNodeWithText("Favorilerim (1)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("IMG_0001.JPG").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("IMG_0002.JPG").assertIsDisplayed()

        composeRule.onNodeWithText("Favorilerim (1)").performClick()

        composeRule.onNodeWithContentDescription("IMG_0001.JPG").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("IMG_0002.JPG").assertCountEquals(0)
    }

    @Test
    fun galleryScreenCanOpenWithFavoritesSelectedExternally() {
        val favorite = galleryPhoto("IMG_0001.JPG")
        val other = galleryPhoto("IMG_0002.JPG")

        composeRule.setContent {
            val gridState = rememberLazyGridState()
            PhotoSelectorTheme(dynamicColor = false) {
                GalleryScreen(
                    photos = listOf(favorite, other),
                    likedPhotoUris = setOf(favorite.uri),
                    selectedTab = GalleryTab.Favorites,
                    onTabSelected = {},
                    strings = UiText.strings(AppLanguage.Turkish),
                    gridState = gridState,
                    onPhotoClick = {},
                    onLikeToggle = {}
                )
            }
        }

        composeRule.onNodeWithText("Favorilerim (1)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("IMG_0001.JPG").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("IMG_0002.JPG").assertCountEquals(0)
    }

    @Test
    fun confirmationSuccessShowsReturnHomeAction() {
        composeRule.setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                ConfirmationScreen(
                    summary = ExportSummary(selectedJpgCount = 1, matchedRawCount = 1),
                    exportStatus = ExportStatus.Success(folderName = "2026-05-30", copiedFiles = 2),
                    strings = UiText.strings(AppLanguage.Turkish),
                    onBack = {},
                    onConfirmExport = {},
                    onFinished = {}
                )
            }
        }

        composeRule.onNodeWithText("Ana sayfaya dön").assertIsDisplayed()
    }

    @Test
    fun returnHomeConfirmationDialogShowsDestructiveChoice() {
        val viewModel = PhotoViewModel()
        viewModel.setMediaItems(listOf(galleryPhoto("IMG_0001.JPG")))
        viewModel.navigateTo(Screen.Gallery)
        viewModel.requestReturnToFolderSelection()

        composeRule.setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                PhotoSelectorApp(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("Ana sayfaya dönülsün mü?").assertIsDisplayed()
        composeRule.onNodeWithText("Vazgeç").assertIsDisplayed()
        composeRule.onNodeWithText("Ana sayfaya dön").assertIsDisplayed()
    }

    @Test
    fun confirmationScreenShowsPriceAndDiscountSummary() {
        composeRule.setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                ConfirmationScreen(
                    summary = ExportSummary(
                        selectedJpgCount = 4,
                        matchedRawCount = 4,
                        selectedVideoCount = 4
                    ),
                    exportStatus = ExportStatus.Idle,
                    strings = UiText.strings(AppLanguage.Turkish),
                    onBack = {},
                    onConfirmExport = {},
                    onFinished = {}
                )
            }
        }

        composeRule.onNodeWithText("Foto: 4").assertIsDisplayed()
        composeRule.onNodeWithText("Video: 4").assertIsDisplayed()
        composeRule.onNodeWithText("Kopyalanacak: 12 dosya").assertIsDisplayed()
        composeRule.onNodeWithText("İndirim: 260 TL").assertIsDisplayed()
        composeRule.onNodeWithText("Toplam: 4940 TL").assertIsDisplayed()
        composeRule.onAllNodesWithText("Fiyat").assertCountEquals(0)
    }

    @Test
    fun confirmationCopyingShowsOverallAndFileProgress() {
        composeRule.setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                ConfirmationScreen(
                    summary = ExportSummary(selectedJpgCount = 1, matchedRawCount = 0),
                    exportStatus = ExportStatus.Copying(
                        copiedFiles = 0,
                        totalFiles = 1,
                        copiedBytes = 50,
                        totalBytes = 100,
                        currentFileName = "IMG_0001.JPG",
                        files = listOf(
                            ExportFileProgress(
                                fileName = "IMG_0001.JPG",
                                state = ExportFileState.Copying,
                                bytesCopied = 50,
                                totalBytes = 100
                            )
                        )
                    ),
                    strings = UiText.strings(AppLanguage.Turkish),
                    onBack = {},
                    onConfirmExport = {},
                    onFinished = {}
                )
            }
        }

        composeRule.onNodeWithText("Kopyalama ilerlemesi").assertIsDisplayed()
        composeRule.onAllNodesWithText("50%").assertCountEquals(2)
        composeRule.onNodeWithText("IMG_0001.JPG").assertIsDisplayed()
    }

    private fun galleryPhoto(displayName: String): MediaItemData =
        MediaItemData(
            uri = android.net.Uri.parse("content://test/$displayName"),
            displayName = displayName,
            mimeType = "image/jpeg",
            mediaType = MediaType.Photo
        )
}
