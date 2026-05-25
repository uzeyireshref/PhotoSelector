package com.uzeyir.photoselector

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoItemData(
    val uri: Uri,
    val displayName: String
)

data class FolderDocumentData(
    val uri: Uri,
    val displayName: String,
    val mimeType: String
)

data class ExportSummary(
    val selectedJpgCount: Int,
    val matchedRawCount: Int
) {
    val totalFileCount: Int
        get() = selectedJpgCount + matchedRawCount
}

data class PricingDiscountTier(
    val photoCount: Int,
    val discountPercent: Int
)

sealed class ExportStatus {
    data object Idle : ExportStatus()
    data object Copying : ExportStatus()
    data class Success(
        val folderName: String,
        val copiedFiles: Int
    ) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

enum class PhotoViewerSource {
    Gallery,
    Review
}

class PhotoViewModel : ViewModel() {
    var currentScreen by mutableStateOf(Screen.FolderSelection)
    val photos = mutableStateListOf<PhotoItemData>()
    val photoUris = mutableStateListOf<Uri>()
    val likedPhotos = mutableStateListOf<Uri>()
    val folderDocuments = mutableStateListOf<FolderDocumentData>()
    var selectedFolderUri by mutableStateOf<Uri?>(null)
        private set
    var exportStatus by mutableStateOf<ExportStatus>(ExportStatus.Idle)
        private set
    var selectionWarningMessage by mutableStateOf<String?>(null)
        private set
    var viewerSource by mutableStateOf(PhotoViewerSource.Gallery)
        private set
    var selectedPhotoIndex by mutableStateOf(-1)
        private set

    val likedPhotoItems: List<PhotoItemData>
        get() = photos.filter { likedPhotos.contains(it.uri) }

    val viewerPhotos: List<PhotoItemData>
        get() = when (viewerSource) {
            PhotoViewerSource.Gallery -> photos
            PhotoViewerSource.Review -> likedPhotoItems
        }

    val selectedPhoto: PhotoItemData?
        get() = viewerPhotos.getOrNull(selectedPhotoIndex)

    val selectedPhotoUri: Uri?
        get() = selectedPhoto?.uri

    val exportSummary: ExportSummary
        get() = ExportSummary(
            selectedJpgCount = likedPhotoItems.size,
            matchedRawCount = likedPhotoItems.sumOf { matchingRawFilesFor(it).size }
        )
    
    private val pricePerPhoto = 300
    private val maxBillablePhotos = 10
    private val discountTiers = listOf(
        PricingDiscountTier(photoCount = 4, discountPercent = 5),
        PricingDiscountTier(photoCount = 5, discountPercent = 10),
        PricingDiscountTier(photoCount = 6, discountPercent = 15),
        PricingDiscountTier(photoCount = 7, discountPercent = 20),
        PricingDiscountTier(photoCount = 8, discountPercent = 25),
        PricingDiscountTier(photoCount = 9, discountPercent = 30),
        PricingDiscountTier(photoCount = 10, discountPercent = 35)
    )
    
    val basePrice: Int
        get() = likedPhotos.size * pricePerPhoto
        
    val discount: Int
        get() = basePrice - totalDisplayPrice
        
    val totalDisplayPrice: Int
        get() {
            val billablePhotoCount = likedPhotos.size.coerceAtMost(maxBillablePhotos)
            val subtotal = billablePhotoCount * pricePerPhoto
            val discountPercent = discountTiers
                .lastOrNull { billablePhotoCount >= it.photoCount }
                ?.discountPercent
                ?: 0
            return subtotal * (100 - discountPercent) / 100
        }

    fun toggleLike(uri: Uri) {
        if (likedPhotos.contains(uri)) {
            likedPhotos.remove(uri)
        } else {
            likedPhotos.add(uri)
        }
        normalizeReviewViewerSelection()
    }
    
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun goToReviewOrWarn(): Boolean {
        if (likedPhotos.isEmpty()) {
            selectionWarningMessage = UiText.selectAtLeastOnePhoto
            return false
        }
        selectionWarningMessage = null
        currentScreen = Screen.Review
        return true
    }

    fun goToConfirmationOrWarn(): Boolean {
        if (likedPhotos.isEmpty()) {
            selectionWarningMessage = UiText.selectAtLeastOnePhoto
            return false
        }
        selectionWarningMessage = null
        currentScreen = Screen.Confirmation
        return true
    }

    fun clearSelectionWarning() {
        selectionWarningMessage = null
    }

    fun handleBack(): Boolean {
        currentScreen = when (currentScreen) {
            Screen.FolderSelection -> return false
            Screen.Gallery -> Screen.FolderSelection
            Screen.PhotoDetail -> if (viewerSource == PhotoViewerSource.Review) Screen.Review else Screen.Gallery
            Screen.Review -> Screen.Gallery
            Screen.Confirmation -> Screen.Review
        }
        return true
    }

    fun setPhotos(newPhotos: List<PhotoItemData>) {
        photos.clear()
        photos.addAll(newPhotos)
        photoUris.clear()
        photoUris.addAll(newPhotos.map { it.uri })
        if (selectedPhotoIndex !in photos.indices) {
            selectedPhotoIndex = -1
        }
    }

    fun setFolderDocuments(newDocuments: List<FolderDocumentData>) {
        folderDocuments.clear()
        folderDocuments.addAll(newDocuments)
    }

    fun openPhoto(uri: Uri) {
        viewerSource = PhotoViewerSource.Gallery
        openPhotoAt(photos.indexOfFirst { it.uri == uri })
    }

    fun openLikedPhoto(uri: Uri) {
        viewerSource = PhotoViewerSource.Review
        openPhotoAt(likedPhotoItems.indexOfFirst { it.uri == uri })
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
        setPhotos(emptyList())
        setFolderDocuments(emptyList())
        selectedFolderUri = null
        selectedPhotoIndex = -1
        viewerSource = PhotoViewerSource.Gallery
        exportStatus = ExportStatus.Idle
        currentScreen = Screen.FolderSelection
    }

    private fun normalizeReviewViewerSelection() {
        if (currentScreen != Screen.PhotoDetail || viewerSource != PhotoViewerSource.Review) return
        if (likedPhotoItems.isEmpty()) {
            selectedPhotoIndex = -1
            currentScreen = Screen.Review
            return
        }
        if (selectedPhotoIndex > likedPhotoItems.lastIndex) {
            selectedPhotoIndex = likedPhotoItems.lastIndex
        }
    }

    fun matchingRawFilesFor(photo: PhotoItemData): List<FolderDocumentData> {
        val photoBaseName = photo.displayName.baseName()
        return folderDocuments
            .filter { document ->
                document.displayName.baseName().equals(photoBaseName, ignoreCase = true) &&
                    document.displayName.extension().uppercase(Locale.US) in rawExtensions
            }
            .sortedBy { it.displayName.lowercase(Locale.US) }
    }

    suspend fun exportSelection(contentResolver: ContentResolver): ExportStatus {
        val treeUri = selectedFolderUri
        val selectedPhotos = likedPhotoItems
        if (treeUri == null) {
            exportStatus = ExportStatus.Error(UiText.noSourceFolder)
            return exportStatus
        }
        if (selectedPhotos.isEmpty()) {
            exportStatus = ExportStatus.Error(UiText.noLikedPhotos)
            return exportStatus
        }

        exportStatus = ExportStatus.Copying
        val folderName = "PhotoSelector_Selected_${timestamp()}"
        exportStatus = withContext(Dispatchers.IO) {
            runCatching {
                copySelectedFiles(contentResolver, treeUri, folderName, selectedPhotos)
            }.getOrElse { error ->
                ExportStatus.Error(error.message ?: UiText.exportFailedFallback)
            }
        }
        return exportStatus
    }

    fun clearExportStatus() {
        exportStatus = ExportStatus.Idle
    }

    fun loadPhotosFromFolder(treeUri: Uri, contentResolver: ContentResolver) {
        setPhotos(emptyList())
        setFolderDocuments(emptyList())
        selectedFolderUri = treeUri
        exportStatus = ExportStatus.Idle
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumn)
                val mime = cursor.getString(mimeColumn)
                val name = cursor.getString(nameColumn)
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                val document = FolderDocumentData(uri = uri, displayName = name, mimeType = mime)
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                    folderDocuments.add(document)
                }
                
                if (mime == "image/jpeg" || name.lowercase().endsWith(".jpg") || name.lowercase().endsWith(".jpeg")) {
                    photos.add(PhotoItemData(uri = uri, displayName = name))
                    photoUris.add(uri)
                }
            }
        }
    }

    private fun copySelectedFiles(
        contentResolver: ContentResolver,
        treeUri: Uri,
        folderName: String,
        selectedPhotos: List<PhotoItemData>
    ): ExportStatus {
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val exportFolderUri = DocumentsContract.createDocument(
            contentResolver,
            parentDocumentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            folderName
        ) ?: error(UiText.couldNotCreateExportFolder)

        val filesToCopy = selectedPhotos.flatMap { photo ->
            listOf(FolderDocumentData(photo.uri, photo.displayName, "image/jpeg")) + matchingRawFilesFor(photo)
        }

        filesToCopy.forEach { source ->
            val destinationUri = DocumentsContract.createDocument(
                contentResolver,
                exportFolderUri,
                source.mimeType.ifBlank { "application/octet-stream" },
                source.displayName
            ) ?: error(UiText.couldNotCreateFile(source.displayName))
            copyDocument(contentResolver, source.uri, destinationUri)
        }

        return ExportStatus.Success(folderName = folderName, copiedFiles = filesToCopy.size)
    }

    private fun copyDocument(contentResolver: ContentResolver, sourceUri: Uri, destinationUri: Uri) {
        contentResolver.openInputStream(sourceUri)?.use { input ->
            contentResolver.openOutputStream(destinationUri)?.use { output ->
                input.copyTo(output)
            } ?: error(UiText.couldNotOpenOutputStream)
        } ?: error(UiText.couldNotOpenInputStream)
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())

    private fun String.baseName(): String =
        substringBeforeLast('.', this)

    private fun String.extension(): String =
        substringAfterLast('.', "")

    private companion object {
        val rawExtensions = setOf("CR3", "CR2", "NEF", "ARW", "DNG", "RAF", "RW2", "ORF")
    }
}
