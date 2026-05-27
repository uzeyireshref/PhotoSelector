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
        val turkish = UiText.strings(AppLanguage.Turkish)
        val english = UiText.strings(AppLanguage.English)

        assertEquals("Fotoğraf Klasörünü Seç", turkish.folderTitle)
        assertEquals("Klasör Seç", turkish.selectFolder)
        assertEquals("SD Kartı Aç", turkish.openSdCard)
        assertEquals("Güncellemeyi Kontrol Et", turkish.checkUpdate)
        assertEquals("Beğendiklerini İncele", turkish.reviewSelection)
        assertEquals("Beğendiklerini İncele", turkish.review)

        assertEquals("Select Photo Folder", english.folderTitle)
        assertEquals("Select Folder", english.selectFolder)
        assertEquals("Open SD Card", english.openSdCard)
        assertEquals("Check for Updates", english.checkUpdate)
        assertEquals("Review Liked", english.reviewSelection)
        assertEquals("Review Liked", english.review)
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
    fun copyVerificationErrorMessageIncludesFileName() {
        assertEquals(
            "IMG_0001.JPG doğru kopyalanamadı. Lütfen SD kart izinlerini kontrol edip tekrar deneyin.",
            UiText.strings(AppLanguage.Turkish).message(UiMessage.CopyVerificationFailed, "IMG_0001.JPG")
        )
        assertEquals(
            "IMG_0001.JPG was not copied correctly. Please check SD card permissions and try again.",
            UiText.strings(AppLanguage.English).message(UiMessage.CopyVerificationFailed, "IMG_0001.JPG")
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
