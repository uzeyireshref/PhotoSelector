package com.uzeyir.photoselector

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationTest {
    @Test
    fun defaultLanguageIsTurkish() {
        assertEquals(AppLanguage.Turkish, UiText.defaultLanguage)
    }

    @Test
    fun selectFolderIsLocalized() {
        assertEquals("Klasör Seç", UiText.strings(AppLanguage.Turkish).selectFolder)
        assertEquals("Select Folder", UiText.strings(AppLanguage.English).selectFolder)
    }

    @Test
    fun selectAtLeastOnePhotoMessageIsLocalized() {
        assertEquals(
            "En az bir fotoğraf seçin.",
            UiText.strings(AppLanguage.Turkish).message(UiMessage.SelectAtLeastOnePhoto)
        )
        assertEquals(
            "Select at least one photo.",
            UiText.strings(AppLanguage.English).message(UiMessage.SelectAtLeastOnePhoto)
        )
    }

    @Test
    fun exportErrorMessageIsLocalized() {
        assertEquals(
            "Kaynak klasör seçilmedi.",
            UiText.strings(AppLanguage.Turkish).message(UiMessage.NoSourceFolder)
        )
        assertEquals(
            "Source folder was not selected.",
            UiText.strings(AppLanguage.English).message(UiMessage.NoSourceFolder)
        )
    }

    @Test
    fun updateErrorLabelIsLocalized() {
        assertEquals(
            "Güncelleme kontrol edilemedi",
            AppUpdateStatus.Error.label(UiText.strings(AppLanguage.Turkish))
        )
        assertEquals(
            "Update check failed",
            AppUpdateStatus.Error.label(UiText.strings(AppLanguage.English))
        )
    }
}
