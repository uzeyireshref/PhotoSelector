package com.uzeyir.photoselector

enum class AppLanguage {
    Turkish,
    English
}

enum class UiMessage {
    SelectAtLeastOnePhoto,
    SdCardNotInserted,
    SdCardPickerUnsupported,
    FolderLoadFailed,
    NoSourceFolder,
    NoLikedPhotos,
    ExportFailedFallback,
    CouldNotCreateExportFolder,
    CouldNotCreateFile,
    CouldNotOpenOutputStream,
    CouldNotOpenInputStream,
    CopyVerificationFailed
}

data class LocalizedStrings(
    val languageOptionTurkish: String,
    val languageOptionEnglish: String,
    val reviewSelection: String,
    val confirmSelection: String,
    val folderTitle: String,
    val selectFolder: String,
    val openSdCard: String,
    val chooseSdCard: String,
    val photo: String,
    val video: String,
    val priceBreakdown: String,
    val selected: String,
    val total: String,
    val review: String,
    val back: String,
    val backToGallery: String,
    val reviewLikedPhotos: String,
    val finalConfirmation: String,
    val selectedJpg: String,
    val matchingRaw: String,
    val selectedVideo: String,
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
    val rotate: String,
    val fullscreen: String,
    val exitFullscreen: String,
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
    private val sdCardNotInserted: String,
    private val sdCardPickerUnsupported: String,
    private val folderLoadFailed: String,
    private val noSourceFolder: String,
    private val noLikedPhotos: String,
    private val exportFailedFallback: String,
    private val couldNotCreateExportFolder: String,
    private val couldNotOpenOutputStream: String,
    private val couldNotOpenInputStream: String,
    private val couldNotCreateFile: (String) -> String,
    private val copyVerificationFailed: (String) -> String
) {
    fun price(amount: Int): String = "$amount TL"
    fun selectedCount(count: Int): String = "$selected: $count"
    fun selectedJpgCount(count: Int): String = "$selectedJpg: $count"
    fun matchingRawCount(count: Int): String = "$matchingRaw: $count"
    fun selectedVideoCount(count: Int): String = "$selectedVideo: $count"
    fun totalFileCount(count: Int): String = "$totalFiles: $count"
    fun folderName(name: String): String = "$folder: $name"
    fun copiedFileCount(count: Int): String = "$copiedFiles: $count"
    fun updateAvailable(versionName: String): String = updateAvailableText(versionName)

    fun message(message: UiMessage, argument: String? = null): String = when (message) {
        UiMessage.SelectAtLeastOnePhoto -> selectAtLeastOnePhoto
        UiMessage.SdCardNotInserted -> sdCardNotInserted
        UiMessage.SdCardPickerUnsupported -> sdCardPickerUnsupported
        UiMessage.FolderLoadFailed -> folderLoadFailed
        UiMessage.NoSourceFolder -> noSourceFolder
        UiMessage.NoLikedPhotos -> noLikedPhotos
        UiMessage.ExportFailedFallback -> exportFailedFallback
        UiMessage.CouldNotCreateExportFolder -> couldNotCreateExportFolder
        UiMessage.CouldNotOpenOutputStream -> couldNotOpenOutputStream
        UiMessage.CouldNotOpenInputStream -> couldNotOpenInputStream
        UiMessage.CouldNotCreateFile -> couldNotCreateFile(argument.orEmpty())
        UiMessage.CopyVerificationFailed -> copyVerificationFailed(argument.orEmpty())
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
        reviewSelection = "Beğendiklerine Bak",
        confirmSelection = "Seçimi Onayla",
        folderTitle = "Fotoğraf Klasörünü Seç",
        selectFolder = "Klasör Seç",
        openSdCard = "SD Kartı Aç",
        chooseSdCard = "SD Kart Seç",
        photo = "Fotoğraf",
        video = "Video",
        priceBreakdown = "Fiyat",
        selected = "Seçilen",
        total = "Toplam",
        review = "İncele",
        back = "Geri",
        backToGallery = "Galeriye Dön",
        reviewLikedPhotos = "Seçilen Fotoğraflar",
        finalConfirmation = "Son Onay",
        selectedJpg = "Seçilen JPG",
        matchingRaw = "Eşleşen RAW",
        selectedVideo = "Seçilen video",
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
        rotate = "Döndür",
        fullscreen = "Tam ekran",
        exitFullscreen = "Tam ekrandan çık",
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
        sdCardNotInserted = "SD kart takılı değil.",
        sdCardPickerUnsupported = "Bu Android sürümünde SD kart doğrudan açılamıyor. Lütfen Klasör Seç ile devam edin.",
        folderLoadFailed = "Klasör açılamadı. Lütfen izinleri kontrol edip tekrar seçin.",
        noSourceFolder = "Kaynak klasör seçilmedi.",
        noLikedPhotos = "Seçilen fotoğraf yok.",
        exportFailedFallback = "Dışa aktarma başarısız oldu.",
        couldNotCreateExportFolder = "Dışa aktarma klasörü oluşturulamadı.",
        couldNotOpenOutputStream = "Hedef dosya açılamadı.",
        couldNotOpenInputStream = "Kaynak dosya açılamadı.",
        couldNotCreateFile = { name -> "$name oluşturulamadı." },
        copyVerificationFailed = { name -> "$name doğru kopyalanamadı. Lütfen SD kart izinlerini kontrol edip tekrar deneyin." }
    )

    internal val english = LocalizedStrings(
        languageOptionTurkish = "Türkçe",
        languageOptionEnglish = "English",
        reviewSelection = "View Likes",
        confirmSelection = "Confirm Selection",
        folderTitle = "Select Photo Folder",
        selectFolder = "Select Folder",
        openSdCard = "Open SD Card",
        chooseSdCard = "Choose SD Card",
        photo = "Photo",
        video = "Video",
        priceBreakdown = "Price",
        selected = "Selected",
        total = "Total",
        review = "Review",
        back = "Back",
        backToGallery = "Back to Gallery",
        reviewLikedPhotos = "Selected Photos",
        finalConfirmation = "Final Confirmation",
        selectedJpg = "Selected JPG",
        matchingRaw = "Matching RAW",
        selectedVideo = "Selected video",
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
        rotate = "Rotate",
        fullscreen = "Fullscreen",
        exitFullscreen = "Exit fullscreen",
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
        sdCardNotInserted = "SD card is not inserted.",
        sdCardPickerUnsupported = "This Android version cannot open the SD card directly. Please use Select Folder.",
        folderLoadFailed = "Could not open the folder. Please check permissions and select it again.",
        noSourceFolder = "Source folder was not selected.",
        noLikedPhotos = "No selected photos.",
        exportFailedFallback = "Export failed.",
        couldNotCreateExportFolder = "Could not create export folder.",
        couldNotOpenOutputStream = "Could not open destination file.",
        couldNotOpenInputStream = "Could not open source file.",
        couldNotCreateFile = { name -> "$name could not be created." },
        copyVerificationFailed = { name -> "$name was not copied correctly. Please check SD card permissions and try again." }
    )

    fun strings(language: AppLanguage): LocalizedStrings = when (language) {
        AppLanguage.Turkish -> turkish
        AppLanguage.English -> english
    }
}
