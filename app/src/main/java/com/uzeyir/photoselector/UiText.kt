package com.uzeyir.photoselector

enum class AppLanguage {
    Turkish,
    English
}

enum class UiMessage {
    SelectAtLeastOnePhoto,
    NoSourceFolder,
    NoLikedPhotos,
    ExportFailedFallback,
    CouldNotCreateExportFolder,
    CouldNotCreateFile,
    CouldNotOpenOutputStream,
    CouldNotOpenInputStream
}

data class LocalizedStrings(
    val languageOptionTurkish: String,
    val languageOptionEnglish: String,
    val reviewSelection: String,
    val confirmSelection: String,
    val folderTitle: String,
    val selectFolder: String,
    val selected: String,
    val total: String,
    val review: String,
    val back: String,
    val backToGallery: String,
    val reviewLikedPhotos: String,
    val finalConfirmation: String,
    val selectedJpg: String,
    val matchingRaw: String,
    val totalFiles: String,
    val copyJpgAndRaw: String,
    val copyingSelectedFiles: String,
    val exportComplete: String,
    val folder: String,
    val copiedFiles: String,
    val finish: String,
    val exportFailed: String,
    val retry: String,
    val like: String,
    val previousPhoto: String,
    val nextPhoto: String,
    val checkUpdate: String,
    val checkingUpdate: String,
    val appUpToDate: String,
    val downloadingUpdate: String,
    val readyToInstall: String,
    val updateCheckFailed: String,
    private val updateAvailableText: (String) -> String,
    private val selectAtLeastOnePhoto: String,
    private val noSourceFolder: String,
    private val noLikedPhotos: String,
    private val exportFailedFallback: String,
    private val couldNotCreateExportFolder: String,
    private val couldNotOpenOutputStream: String,
    private val couldNotOpenInputStream: String,
    private val couldNotCreateFile: (String) -> String
) {
    fun price(amount: Int): String = "$amount TL"
    fun selectedCount(count: Int): String = "$selected: $count"
    fun selectedJpgCount(count: Int): String = "$selectedJpg: $count"
    fun matchingRawCount(count: Int): String = "$matchingRaw: $count"
    fun totalFileCount(count: Int): String = "$totalFiles: $count"
    fun folderName(name: String): String = "$folder: $name"
    fun copiedFileCount(count: Int): String = "$copiedFiles: $count"
    fun updateAvailable(versionName: String): String = updateAvailableText(versionName)

    fun message(message: UiMessage, argument: String? = null): String = when (message) {
        UiMessage.SelectAtLeastOnePhoto -> selectAtLeastOnePhoto
        UiMessage.NoSourceFolder -> noSourceFolder
        UiMessage.NoLikedPhotos -> noLikedPhotos
        UiMessage.ExportFailedFallback -> exportFailedFallback
        UiMessage.CouldNotCreateExportFolder -> couldNotCreateExportFolder
        UiMessage.CouldNotOpenOutputStream -> couldNotOpenOutputStream
        UiMessage.CouldNotOpenInputStream -> couldNotOpenInputStream
        UiMessage.CouldNotCreateFile -> couldNotCreateFile(argument.orEmpty())
    }
}

fun AppUpdateStatus.label(strings: LocalizedStrings): String = when (this) {
    AppUpdateStatus.Idle -> strings.checkUpdate
    AppUpdateStatus.Checking -> strings.checkingUpdate
    AppUpdateStatus.UpToDate -> strings.appUpToDate
    is AppUpdateStatus.Available -> strings.updateAvailable(versionName)
    AppUpdateStatus.Downloading -> strings.downloadingUpdate
    AppUpdateStatus.ReadyToInstall -> strings.readyToInstall
    AppUpdateStatus.Error -> strings.updateCheckFailed
}

internal object UiText {
    val defaultLanguage = AppLanguage.Turkish

    internal val turkish = LocalizedStrings(
        languageOptionTurkish = "Türkçe",
        languageOptionEnglish = "English",
        reviewSelection = "Seçimi İncele",
        confirmSelection = "Seçimi Onayla",
        folderTitle = "Fotoğraf Klasörünü Seç",
        selectFolder = "Klasör Seç",
        selected = "Seçilen",
        total = "Toplam",
        review = "İncele",
        back = "Geri",
        backToGallery = "Galeriye Dön",
        reviewLikedPhotos = "Seçilen Fotoğraflar",
        finalConfirmation = "Son Onay",
        selectedJpg = "Seçilen JPG",
        matchingRaw = "Eşleşen RAW",
        totalFiles = "Toplam dosya",
        copyJpgAndRaw = "JPG + RAW Kopyala",
        copyingSelectedFiles = "Seçilen dosyalar kopyalanıyor...",
        exportComplete = "Dışa aktarma tamamlandı",
        folder = "Klasör",
        copiedFiles = "Kopyalanan dosya",
        finish = "Bitir",
        exportFailed = "Dışa aktarma başarısız",
        retry = "Tekrar Dene",
        like = "Beğen",
        previousPhoto = "Önceki fotoğraf",
        nextPhoto = "Sonraki fotoğraf",
        checkUpdate = "Güncellemeyi Kontrol Et",
        checkingUpdate = "Kontrol ediliyor",
        appUpToDate = "Uygulama güncel",
        downloadingUpdate = "İndiriliyor",
        readyToInstall = "Kuruluma hazır",
        updateCheckFailed = "Güncelleme kontrol edilemedi",
        updateAvailableText = { versionName -> "Yeni sürüm var: $versionName" },
        selectAtLeastOnePhoto = "En az bir fotoğraf seçin.",
        noSourceFolder = "Kaynak klasör seçilmedi.",
        noLikedPhotos = "Seçilen fotoğraf yok.",
        exportFailedFallback = "Dışa aktarma başarısız oldu.",
        couldNotCreateExportFolder = "Dışa aktarma klasörü oluşturulamadı.",
        couldNotOpenOutputStream = "Hedef dosya açılamadı.",
        couldNotOpenInputStream = "Kaynak dosya açılamadı.",
        couldNotCreateFile = { name -> "$name oluşturulamadı." }
    )

    internal val english = LocalizedStrings(
        languageOptionTurkish = "Türkçe",
        languageOptionEnglish = "English",
        reviewSelection = "Review Selection",
        confirmSelection = "Confirm Selection",
        folderTitle = "Select Photo Folder",
        selectFolder = "Select Folder",
        selected = "Selected",
        total = "Total",
        review = "Review",
        back = "Back",
        backToGallery = "Back to Gallery",
        reviewLikedPhotos = "Selected Photos",
        finalConfirmation = "Final Confirmation",
        selectedJpg = "Selected JPG",
        matchingRaw = "Matching RAW",
        totalFiles = "Total files",
        copyJpgAndRaw = "Copy JPG + RAW",
        copyingSelectedFiles = "Copying selected files...",
        exportComplete = "Export complete",
        folder = "Folder",
        copiedFiles = "Copied files",
        finish = "Finish",
        exportFailed = "Export failed",
        retry = "Retry",
        like = "Like",
        previousPhoto = "Previous photo",
        nextPhoto = "Next photo",
        checkUpdate = "Check for Updates",
        checkingUpdate = "Checking",
        appUpToDate = "App is up to date",
        downloadingUpdate = "Downloading",
        readyToInstall = "Ready to install",
        updateCheckFailed = "Update check failed",
        updateAvailableText = { versionName -> "New version available: $versionName" },
        selectAtLeastOnePhoto = "Select at least one photo.",
        noSourceFolder = "Source folder was not selected.",
        noLikedPhotos = "No selected photos.",
        exportFailedFallback = "Export failed.",
        couldNotCreateExportFolder = "Could not create export folder.",
        couldNotOpenOutputStream = "Could not open destination file.",
        couldNotOpenInputStream = "Could not open source file.",
        couldNotCreateFile = { name -> "$name could not be created." }
    )

    fun strings(language: AppLanguage): LocalizedStrings = when (language) {
        AppLanguage.Turkish -> turkish
        AppLanguage.English -> english
    }
}
