package com.uzeyir.photoselector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
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
}
