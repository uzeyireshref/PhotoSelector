package com.uzeyir.photoselector

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Date

class PhotoViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    var currentScreen by mutableStateOf(Screen.FolderSelection)
    val photos = mutableStateListOf<MediaItemData>()
    val likedPhotos = mutableStateListOf<Uri>()
    val folderDocuments = mutableStateListOf<FolderDocumentData>()
    private val likedMediaItemCache = mutableStateListOf<MediaItemData>()
    private val likedPhotoUriMembership = mutableStateMapOf<Uri, Unit>()
    private val rawDocumentsByBaseName = mutableStateMapOf<String, List<FolderDocumentData>>()
    private val rotationByUri = mutableStateMapOf<Uri, Int>()
    var selectedFolderUri by mutableStateOf<Uri?>(null)
        private set
    var mediaLoadVersion by mutableIntStateOf(0)
        private set
    var isLoadingMedia by mutableStateOf(false)
        private set
    var exportStatus by mutableStateOf<ExportStatus>(ExportStatus.Idle)
        private set
    var selectionWarningMessage by mutableStateOf<UiMessage?>(null)
        private set
    var viewerSource by mutableStateOf(PhotoViewerSource.Gallery)
        private set
    var selectedPhotoIndex by mutableIntStateOf(-1)
        private set
    var galleryTab by mutableStateOf(GalleryTab.All)
        private set
    var pendingReturnToFolderConfirmation by mutableStateOf(false)
        private set
    var language by mutableStateOf(savedLanguage())
        private set
    var includeRawFiles by mutableStateOf(savedStateHandle[KEY_INCLUDE_RAW_FILES] ?: true)
        private set
    var updateStatus by mutableStateOf(savedUpdateStatus())
        private set
    private var lastFolderRestoreAttempted by mutableStateOf(
        savedStateHandle[KEY_LAST_FOLDER_RESTORE_ATTEMPTED] ?: false
    )

    val likedPhotoItems: List<MediaItemData>
        get() = likedMediaItemCache.filter { it.mediaType == MediaType.Photo }

    val likedVideoItems: List<MediaItemData>
        get() = likedMediaItemCache.filter { it.mediaType == MediaType.Video }

    val likedMediaItems: List<MediaItemData>
        get() = likedMediaItemCache

    val likedPhotoUriSet: Set<Uri>
        get() = likedPhotoUriMembership.keys

    val viewerPhotos: List<MediaItemData>
        get() = photos

    val selectedPhoto: MediaItemData?
        get() = viewerPhotos.getOrNull(selectedPhotoIndex)

    val selectedPhotoUri: Uri?
        get() = selectedPhoto?.uri

    val exportSummary: ExportSummary
        get() = ExportSummary(
            selectedJpgCount = likedPhotoItems.size,
            matchedRawCount = likedPhotoItems.sumOf { matchingRawFilesFor(it).size },
            selectedVideoCount = likedVideoItems.size
        )

    private val selectionPricing: SelectionPricing
        get() = SelectionPricing(
            photoCount = selectedPhotoCount,
            videoCount = selectedVideoCount
        )

    val selectedPhotoCount: Int
        get() = likedPhotoItems.size

    val selectedVideoCount: Int
        get() = likedVideoItems.size

    val photoBasePrice: Int
        get() = selectionPricing.photoBasePrice

    val photoDisplayPrice: Int
        get() = selectionPricing.photoDisplayPrice

    val photoDiscount: Int
        get() = selectionPricing.photoDiscount

    val videoBasePrice: Int
        get() = selectionPricing.videoBasePrice

    val videoDisplayPrice: Int
        get() = selectionPricing.videoDisplayPrice

    val videoDiscount: Int
        get() = selectionPricing.videoDiscount

    val basePrice: Int
        get() = selectionPricing.basePrice

    val discount: Int
        get() = selectionPricing.discount

    val totalDisplayPrice: Int
        get() = selectionPricing.totalDisplayPrice

    fun toggleLike(uri: Uri) {
        if (likedPhotoUriMembership.containsKey(uri)) {
            likedPhotos.remove(uri)
            likedPhotoUriMembership.remove(uri)
        } else {
            likedPhotos.add(uri)
            likedPhotoUriMembership[uri] = Unit
        }
        rebuildLikedMediaCache()
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun selectGalleryTab(tab: GalleryTab) {
        galleryTab = tab
    }

    fun shouldRestoreLastFolderOnStartup(): Boolean =
        !lastFolderRestoreAttempted &&
            currentScreen == Screen.FolderSelection &&
            !hasActiveFolderSession()

    fun markLastFolderRestoreAttempted() {
        lastFolderRestoreAttempted = true
        savedStateHandle[KEY_LAST_FOLDER_RESTORE_ATTEMPTED] = true
    }

    fun selectLanguage(selectedLanguage: AppLanguage) {
        language = selectedLanguage
        savedStateHandle[KEY_LANGUAGE] = selectedLanguage.name
    }

    fun chooseIncludeRawFiles(include: Boolean) {
        includeRawFiles = include
        savedStateHandle[KEY_INCLUDE_RAW_FILES] = include
    }

    fun replaceUpdateStatus(status: AppUpdateStatus) {
        updateStatus = status
        savedStateHandle[KEY_UPDATE_STATUS] = status.savedName
        savedStateHandle[KEY_UPDATE_VERSION_NAME] = (status as? AppUpdateStatus.Available)?.versionName
    }

    fun goToConfirmationOrWarn(): Boolean {
        if (likedMediaItems.isEmpty()) {
            selectionWarningMessage = UiMessage.SelectAtLeastOnePhoto
            return false
        }
        selectionWarningMessage = null
        currentScreen = Screen.Confirmation
        return true
    }

    fun showFavoritesFromViewerOrWarn(): Boolean {
        selectionWarningMessage = null
        galleryTab = GalleryTab.Favorites
        currentScreen = Screen.Gallery
        return true
    }

    fun warn(message: UiMessage) {
        selectionWarningMessage = message
    }

    fun clearSelectionWarning() {
        selectionWarningMessage = null
    }

    fun handleBack(): Boolean {
        if (currentScreen == Screen.Confirmation && exportStatus is ExportStatus.Success) {
            reset()
            return true
        }
        currentScreen = when (currentScreen) {
            Screen.FolderSelection -> return false
            Screen.Gallery -> Screen.FolderSelection
            Screen.PhotoDetail -> Screen.Gallery
            Screen.Confirmation -> Screen.Gallery
        }
        return true
    }

    fun requestReturnToFolderSelection(): Boolean {
        if (!hasActiveFolderSession()) {
            currentScreen = Screen.FolderSelection
            return true
        }
        pendingReturnToFolderConfirmation = true
        return true
    }

    fun cancelReturnToFolderSelection() {
        pendingReturnToFolderConfirmation = false
    }

    fun confirmReturnToFolderSelection() {
        pendingReturnToFolderConfirmation = false
        reset()
    }

    fun replaceExportStatus(status: ExportStatus) {
        exportStatus = status
    }

    fun setPhotos(newPhotos: List<MediaItemData>) {
        setMediaItems(newPhotos)
    }

    fun setMediaItems(newMediaItems: List<MediaItemData>) {
        photos.clear()
        photos.addAll(sortMediaItemsForGallery(newMediaItems))
        likedPhotos.clear()
        likedPhotoUriMembership.clear()
        likedMediaItemCache.clear()
        rotationByUri.clear()
        if (selectedPhotoIndex !in photos.indices) {
            selectedPhotoIndex = -1
        }
        mediaLoadVersion += 1
    }

    fun rotationFor(uri: Uri): Int =
        rotationByUri[uri] ?: 0

    fun rotateSelectedMedia() {
        val uri = selectedPhotoUri ?: return
        val nextRotation = (rotationFor(uri) + 90) % 360
        if (nextRotation == 0) {
            rotationByUri.remove(uri)
        } else {
            rotationByUri[uri] = nextRotation
        }
    }

    fun setFolderDocuments(newDocuments: List<FolderDocumentData>) {
        val exportDocuments = exportableRawDocuments(newDocuments)
        folderDocuments.clear()
        folderDocuments.addAll(exportDocuments)
        rebuildRawDocumentIndex(exportDocuments)
    }

    fun openPhoto(uri: Uri) {
        viewerSource = PhotoViewerSource.Gallery
        openPhotoAt(photos.indexOfFirst { it.uri == uri })
    }

    fun openPhotoAt(index: Int) {
        if (index !in viewerPhotos.indices) return
        selectedPhotoIndex = index
        currentScreen = Screen.PhotoDetail
    }

    fun showNextPhoto() {
        if (selectedPhotoIndex < viewerPhotos.lastIndex) {
            selectedPhotoIndex += 1
        }
    }

    fun showPreviousPhoto() {
        if (selectedPhotoIndex > 0) {
            selectedPhotoIndex -= 1
        }
    }

    fun reset() {
        likedPhotos.clear()
        likedPhotoUriMembership.clear()
        setMediaItems(emptyList())
        setFolderDocuments(emptyList())
        selectedFolderUri = null
        selectedPhotoIndex = -1
        viewerSource = PhotoViewerSource.Gallery
        galleryTab = GalleryTab.All
        pendingReturnToFolderConfirmation = false
        exportStatus = ExportStatus.Idle
        isLoadingMedia = false
        currentScreen = Screen.FolderSelection
    }

    private fun hasActiveFolderSession(): Boolean =
        selectedFolderUri != null ||
            photos.isNotEmpty() ||
            folderDocuments.isNotEmpty() ||
            likedPhotoUriMembership.isNotEmpty()

    fun matchingRawFilesFor(photo: MediaItemData): List<FolderDocumentData> {
        return matchingRawFilesFor(rawDocumentsByBaseName, photo)
    }

    private fun rebuildRawDocumentIndex(documents: List<FolderDocumentData>) {
        rawDocumentsByBaseName.clear()
        rawDocumentsByBaseName.putAll(rawDocumentsByBaseName(documents))
    }

    private fun rebuildLikedMediaCache() {
        likedMediaItemCache.clear()
        likedMediaItemCache.addAll(photos.filter { likedPhotoUriMembership.containsKey(it.uri) })
    }

    suspend fun exportSelection(contentResolver: ContentResolver, includeRawFiles: Boolean = true): ExportStatus {
        if (!shouldBeginExport(exportStatus)) {
            return exportStatus
        }
        val treeUri = selectedFolderUri
        val selectedMedia = likedMediaItems
        if (treeUri == null) {
            exportStatus = ExportStatus.Error(UiMessage.NoSourceFolder)
            return exportStatus
        }
        if (selectedMedia.isEmpty()) {
            exportStatus = ExportStatus.Error(UiMessage.NoLikedPhotos)
            return exportStatus
        }

        exportStatus = ExportStatus.Copying()
        val folderName = timestamp()
        exportStatus = withContext(Dispatchers.IO) {
            cancellationSafeRunCatching {
                SelectionExporter(
                    contentResolver = contentResolver,
                    treeUri = treeUri,
                    selectedMedia = selectedMedia,
                    includeRawFiles = includeRawFiles,
                    matchingRawFiles = ::matchingRawFilesFor,
                    onProgress = { status ->
                        runBlocking(Dispatchers.Main.immediate) {
                            exportStatus = status
                        }
                    }
                ).export(folderName)
            }.getOrElse { error ->
                if (error is LocalizedExportException) {
                    ExportStatus.Error(error.uiMessage, error.argument)
                } else {
                    ExportStatus.Error(UiMessage.ExportFailedFallback)
                }
            }
        }
        return exportStatus
    }

    fun clearExportStatus() {
        exportStatus = ExportStatus.Idle
    }

    fun selectedTransferFiles(includeRawFiles: Boolean = true): List<FolderDocumentData> =
        selectedTransferFiles(
            selectedMedia = likedMediaItems,
            includeRawFiles = includeRawFiles,
            matchingRawFiles = ::matchingRawFilesFor
        )

    fun returnHomeAfterExport() {
        reset()
    }

    fun returnToGalleryAfterExport() {
        exportStatus = ExportStatus.Idle
        currentScreen = Screen.Gallery
    }

    suspend fun loadPhotosFromFolder(treeUri: Uri, contentResolver: ContentResolver) {
        loadMediaFromFolder(treeUri, contentResolver)
    }

    suspend fun loadMediaFromFolder(treeUri: Uri, contentResolver: ContentResolver) {
        isLoadingMedia = true
        try {
            setMediaItems(emptyList())
            setFolderDocuments(emptyList())
            selectedFolderUri = treeUri
            exportStatus = ExportStatus.Idle
            val result = withContext(Dispatchers.IO) {
                queryFolderMedia(treeUri, contentResolver)
            }
            setFolderDocuments(result.documents)
            setMediaItems(result.mediaItems)
        } finally {
            isLoadingMedia = false
        }
    }

    private fun timestamp(): String =
        formatExportFolderTimestamp(Date())

    private fun savedLanguage(): AppLanguage {
        val languageName = savedStateHandle[KEY_LANGUAGE] ?: UiText.defaultLanguage.name
        return AppLanguage.entries.firstOrNull { it.name == languageName } ?: UiText.defaultLanguage
    }

    private fun savedUpdateStatus(): AppUpdateStatus =
        when (savedStateHandle[KEY_UPDATE_STATUS] ?: AppUpdateStatus.Idle.savedName) {
            AppUpdateStatus.Checking.savedName -> AppUpdateStatus.Checking
            AppUpdateStatus.UpToDate.savedName -> AppUpdateStatus.UpToDate
            AppUpdateStatus.Available("").savedName -> AppUpdateStatus.Available(
                savedStateHandle[KEY_UPDATE_VERSION_NAME] ?: ""
            )
            AppUpdateStatus.Downloading.savedName -> AppUpdateStatus.Downloading
            AppUpdateStatus.ReadyToInstall.savedName -> AppUpdateStatus.ReadyToInstall
            AppUpdateStatus.Error.savedName -> AppUpdateStatus.Error
            else -> AppUpdateStatus.Idle
        }

    private val AppUpdateStatus.savedName: String
        get() = when (this) {
            AppUpdateStatus.Idle -> "Idle"
            AppUpdateStatus.Checking -> "Checking"
            AppUpdateStatus.UpToDate -> "UpToDate"
            is AppUpdateStatus.Available -> "Available"
            AppUpdateStatus.Downloading -> "Downloading"
            AppUpdateStatus.ReadyToInstall -> "ReadyToInstall"
            AppUpdateStatus.Error -> "Error"
        }

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_INCLUDE_RAW_FILES = "includeRawFiles"
        const val KEY_UPDATE_STATUS = "updateStatus"
        const val KEY_UPDATE_VERSION_NAME = "updateVersionName"
        const val KEY_LAST_FOLDER_RESTORE_ATTEMPTED = "lastFolderRestoreAttempted"
    }
}
