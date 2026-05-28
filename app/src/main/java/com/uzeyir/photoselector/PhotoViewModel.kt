package com.uzeyir.photoselector

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaType {
    Photo,
    Video
}

data class MediaItemData(
    val uri: Uri,
    val displayName: String,
    val mimeType: String = "image/jpeg",
    val mediaType: MediaType = MediaType.Photo,
    val lastModified: Long = 0
)

typealias PhotoItemData = MediaItemData

data class FolderDocumentData(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val lastModified: Long = 0
)

data class ExportSummary(
    val selectedJpgCount: Int,
    val matchedRawCount: Int,
    val selectedVideoCount: Int = 0
) {
    val totalFileCount: Int
        get() = selectedJpgCount + matchedRawCount + selectedVideoCount
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
    data class Error(
        val message: UiMessage,
        val argument: String? = null
    ) : ExportStatus()
}

internal data class FolderLoadResult(
    val documents: List<FolderDocumentData>,
    val mediaItems: List<MediaItemData>
)

enum class PhotoViewerSource {
    Gallery,
    Review
}

fun FolderDocumentData.toMediaItemOrNull(): MediaItemData? {
    val type = when {
        isJpegDocument() -> MediaType.Photo
        isVideoDocument() -> MediaType.Video
        else -> null
    } ?: return null

    return MediaItemData(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        mediaType = type,
        lastModified = lastModified
    )
}

fun sortMediaItemsForGallery(mediaItems: List<MediaItemData>): List<MediaItemData> =
    mediaItems.sortedWith(
        compareBy<MediaItemData> { if (it.mediaType == MediaType.Video) 0 else 1 }
            .thenByDescending { it.lastModified }
            .thenByDescending { it.displayName.lowercase(Locale.US) }
    )

private fun FolderDocumentData.isJpegDocument(): Boolean =
    mimeType.equals("image/jpeg", ignoreCase = true) ||
        displayName.extension().lowercase(Locale.US) in setOf("jpg", "jpeg")

private fun FolderDocumentData.isVideoDocument(): Boolean =
    mimeType.startsWith("video/", ignoreCase = true) ||
        displayName.extension().lowercase(Locale.US) in videoExtensions

private fun String.baseName(): String =
    substringBeforeLast('.', this)

private fun String.extension(): String =
    substringAfterLast('.', "")

private val videoExtensions = setOf(
    "mp4",
    "m4v",
    "mov",
    "avi",
    "mkv",
    "webm",
    "3gp",
    "3gpp",
    "mts",
    "m2ts",
    "ts",
    "wmv",
    "flv"
)

class PhotoViewModel : ViewModel() {
    var currentScreen by mutableStateOf(Screen.FolderSelection)
    val photos = mutableStateListOf<MediaItemData>()
    val photoUris = mutableStateListOf<Uri>()
    val likedPhotos = mutableStateListOf<Uri>()
    val folderDocuments = mutableStateListOf<FolderDocumentData>()
    private val rotationByUri = mutableStateMapOf<Uri, Int>()
    var selectedFolderUri by mutableStateOf<Uri?>(null)
        private set
    var exportStatus by mutableStateOf<ExportStatus>(ExportStatus.Idle)
        private set
    var selectionWarningMessage by mutableStateOf<UiMessage?>(null)
        private set
    var viewerSource by mutableStateOf(PhotoViewerSource.Gallery)
        private set
    var selectedPhotoIndex by mutableIntStateOf(-1)
        private set

    val likedPhotoItems: List<MediaItemData>
        get() = photos.filter { likedPhotos.contains(it.uri) && it.mediaType == MediaType.Photo }

    val likedVideoItems: List<MediaItemData>
        get() = photos.filter { likedPhotos.contains(it.uri) && it.mediaType == MediaType.Video }

    val likedMediaItems: List<MediaItemData>
        get() = photos.filter { likedPhotos.contains(it.uri) }

    val viewerPhotos: List<MediaItemData>
        get() = when (viewerSource) {
            PhotoViewerSource.Gallery -> photos
            PhotoViewerSource.Review -> likedMediaItems
        }

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

    private val pricePerPhoto = 300
    private val pricePerVideo = 1000
    private val maxBillableItems = 10
    private val discountTiers = listOf(
        PricingDiscountTier(photoCount = 4, discountPercent = 5),
        PricingDiscountTier(photoCount = 5, discountPercent = 10),
        PricingDiscountTier(photoCount = 6, discountPercent = 15),
        PricingDiscountTier(photoCount = 7, discountPercent = 20),
        PricingDiscountTier(photoCount = 8, discountPercent = 25),
        PricingDiscountTier(photoCount = 9, discountPercent = 30),
        PricingDiscountTier(photoCount = 10, discountPercent = 35)
    )

    val selectedPhotoCount: Int
        get() = likedPhotoItems.size

    val selectedVideoCount: Int
        get() = likedVideoItems.size

    val photoBasePrice: Int
        get() = selectedPhotoCount * pricePerPhoto

    val photoDisplayPrice: Int
        get() = discountedPrice(selectedPhotoCount, pricePerPhoto)

    val photoDiscount: Int
        get() = photoBasePrice - photoDisplayPrice

    val videoBasePrice: Int
        get() = selectedVideoCount * pricePerVideo

    val videoDisplayPrice: Int
        get() = discountedPrice(selectedVideoCount, pricePerVideo)

    val videoDiscount: Int
        get() = videoBasePrice - videoDisplayPrice

    val basePrice: Int
        get() = photoBasePrice + videoBasePrice

    val discount: Int
        get() = basePrice - totalDisplayPrice

    val totalDisplayPrice: Int
        get() = photoDisplayPrice + videoDisplayPrice

    private fun discountedPrice(count: Int, unitPrice: Int): Int {
        val billableCount = count.coerceAtMost(maxBillableItems)
        val subtotal = billableCount * unitPrice
        val discountPercent = discountTiers
            .lastOrNull { billableCount >= it.photoCount }
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
        if (likedMediaItems.isEmpty()) {
            selectionWarningMessage = UiMessage.SelectAtLeastOnePhoto
            return false
        }
        selectionWarningMessage = null
        currentScreen = Screen.Review
        return true
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

    fun warn(message: UiMessage) {
        selectionWarningMessage = message
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
        setMediaItems(newPhotos)
    }

    fun setMediaItems(newMediaItems: List<MediaItemData>) {
        photos.clear()
        photos.addAll(sortMediaItemsForGallery(newMediaItems))
        photoUris.clear()
        photoUris.addAll(photos.map { it.uri })
        likedPhotos.clear()
        rotationByUri.clear()
        if (selectedPhotoIndex !in photos.indices) {
            selectedPhotoIndex = -1
        }
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
        folderDocuments.clear()
        folderDocuments.addAll(newDocuments)
    }

    fun openPhoto(uri: Uri) {
        viewerSource = PhotoViewerSource.Gallery
        openPhotoAt(photos.indexOfFirst { it.uri == uri })
    }

    fun openLikedPhoto(uri: Uri) {
        viewerSource = PhotoViewerSource.Review
        openPhotoAt(likedMediaItems.indexOfFirst { it.uri == uri })
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
        setMediaItems(emptyList())
        setFolderDocuments(emptyList())
        selectedFolderUri = null
        selectedPhotoIndex = -1
        viewerSource = PhotoViewerSource.Gallery
        exportStatus = ExportStatus.Idle
        currentScreen = Screen.FolderSelection
    }

    private fun normalizeReviewViewerSelection() {
        if (currentScreen != Screen.PhotoDetail || viewerSource != PhotoViewerSource.Review) return
        if (likedMediaItems.isEmpty()) {
            selectedPhotoIndex = -1
            currentScreen = Screen.Review
            return
        }
        if (selectedPhotoIndex > likedMediaItems.lastIndex) {
            selectedPhotoIndex = likedMediaItems.lastIndex
        }
    }

    fun matchingRawFilesFor(photo: MediaItemData): List<FolderDocumentData> {
        if (photo.mediaType != MediaType.Photo) return emptyList()
        val photoBaseName = photo.displayName.baseName()
        return folderDocuments
            .filter { document ->
                document.displayName.baseName().equals(photoBaseName, ignoreCase = true) &&
                    document.displayName.extension().uppercase(Locale.US) in rawExtensions
            }
            .sortedBy { it.displayName.lowercase(Locale.US) }
    }

    suspend fun exportSelection(contentResolver: ContentResolver): ExportStatus {
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

        exportStatus = ExportStatus.Copying
        val folderName = timestamp()
        exportStatus = withContext(Dispatchers.IO) {
            runCatching {
                copySelectedFiles(contentResolver, treeUri, folderName, selectedMedia)
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

    suspend fun loadPhotosFromFolder(treeUri: Uri, contentResolver: ContentResolver) {
        loadMediaFromFolder(treeUri, contentResolver)
    }

    suspend fun loadMediaFromFolder(treeUri: Uri, contentResolver: ContentResolver) {
        setMediaItems(emptyList())
        setFolderDocuments(emptyList())
        selectedFolderUri = treeUri
        exportStatus = ExportStatus.Idle
        val result = withContext(Dispatchers.IO) {
            queryFolderMedia(treeUri, contentResolver)
        }
        setFolderDocuments(result.documents)
        setMediaItems(result.mediaItems)
    }

    private fun queryFolderMedia(treeUri: Uri, contentResolver: ContentResolver): FolderLoadResult {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val documents = mutableListOf<FolderDocumentData>()
        val mediaItems = mutableListOf<MediaItemData>()
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val lastModifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumn)
                val mime = cursor.getString(mimeColumn).orEmpty()
                val name = cursor.getString(nameColumn).orEmpty()
                val lastModified = if (lastModifiedColumn >= 0 && !cursor.isNull(lastModifiedColumn)) {
                    cursor.getLong(lastModifiedColumn)
                } else {
                    0L
                }
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                val document = FolderDocumentData(
                    uri = uri,
                    displayName = name,
                    mimeType = mime,
                    lastModified = lastModified
                )
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                    documents.add(document)
                    document.toMediaItemOrNull()?.let { media ->
                        mediaItems.add(media)
                    }
                }
            }
        }
        return FolderLoadResult(documents = documents, mediaItems = mediaItems)
    }

    private fun copySelectedFiles(
        contentResolver: ContentResolver,
        treeUri: Uri,
        folderName: String,
        selectedMedia: List<MediaItemData>
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
        ) ?: localizedError(UiMessage.CouldNotCreateExportFolder)

        val filesToCopy = selectedMedia.flatMap { media ->
            when (media.mediaType) {
                MediaType.Photo -> listOf(media.toFolderDocumentData()) + matchingRawFilesFor(media)
                MediaType.Video -> listOf(media.toFolderDocumentData())
            }
        }

        val createdFileUris = mutableListOf<Uri>()
        try {
            filesToCopy.forEach { source ->
                val destinationUri = DocumentsContract.createDocument(
                    contentResolver,
                    exportFolderUri,
                    source.mimeType.ifBlank { "application/octet-stream" },
                    source.displayName
                ) ?: localizedError(UiMessage.CouldNotCreateFile, source.displayName)
                createdFileUris.add(destinationUri)
                copyDocument(contentResolver, source.uri, destinationUri, source.displayName)
            }
        } catch (error: Throwable) {
            cleanupCreatedExportDocuments(
                createdFileUris = createdFileUris,
                exportFolderUri = exportFolderUri
            ) { uri ->
                DocumentsContract.deleteDocument(contentResolver, uri)
            }
            throw error
        }

        return ExportStatus.Success(folderName = folderName, copiedFiles = filesToCopy.size)
    }

    private fun MediaItemData.toFolderDocumentData(): FolderDocumentData =
        FolderDocumentData(
            uri = uri,
            displayName = displayName,
            lastModified = lastModified,
            mimeType = mimeType.ifBlank {
                if (mediaType == MediaType.Photo) "image/jpeg" else "application/octet-stream"
            }
        )

    private fun copyDocument(
        contentResolver: ContentResolver,
        sourceUri: Uri,
        destinationUri: Uri,
        displayName: String
    ) {
        val expectedBytes = queryDocumentSize(contentResolver, sourceUri)
        val copiedBytes = copyDocumentFileDescriptors(contentResolver, sourceUri, destinationUri, displayName)
        verifyCopiedDocumentSize(contentResolver, destinationUri, copiedBytes, expectedBytes, displayName)
    }

    private fun verifyCopiedDocumentSize(
        contentResolver: ContentResolver,
        destinationUri: Uri,
        copiedBytes: Long,
        expectedBytes: Long?,
        displayName: String
    ) {
        val reportedSize = queryDocumentSize(contentResolver, destinationUri)

        if (expectedBytes != null && expectedBytes != copiedBytes) {
            localizedError(UiMessage.CopyVerificationFailed, displayName)
        }
        if (reportedSize != null && reportedSize != copiedBytes) {
            localizedError(UiMessage.CopyVerificationFailed, displayName)
        }
    }

    private fun queryDocumentSize(contentResolver: ContentResolver, documentUri: Uri): Long? =
        contentResolver.query(
            documentUri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    cursor.getLong(sizeColumn).takeIf { it >= 0L }
                } else {
                    null
                }
            } else {
                null
            }
        }

    private fun localizedError(message: UiMessage, argument: String? = null): Nothing {
        throw LocalizedExportException(message, argument)
    }

    private fun timestamp(): String =
        formatExportFolderTimestamp(Date())

    private companion object {
        val rawExtensions = setOf("CR3", "CR2", "NEF", "ARW", "DNG", "RAF", "RW2", "ORF")
    }
}

internal fun formatExportFolderTimestamp(date: Date): String =
    SimpleDateFormat("HH.mm_dd-MM", Locale.US).format(date)

internal fun exportFolderName(date: Date): String =
    formatExportFolderTimestamp(date)

private class LocalizedExportException(
    val uiMessage: UiMessage,
    val argument: String? = null
) : IllegalStateException(uiMessage.name)

internal fun copyDocumentBytes(input: InputStream, output: OutputStream, displayName: String): Long {
    val copiedBytes = input.copyTo(output)
    output.flush()
    if (copiedBytes <= 0L) {
        throw LocalizedExportException(UiMessage.CopyVerificationFailed, displayName)
    }
    return copiedBytes
}

internal fun shouldBeginExport(exportStatus: ExportStatus): Boolean =
    exportStatus != ExportStatus.Copying

internal fun cleanupCreatedExportDocuments(
    createdFileUris: List<Uri>,
    exportFolderUri: Uri,
    deleteDocument: (Uri) -> Boolean
) {
    createdFileUris.asReversed().forEach { uri ->
        runCatching { deleteDocument(uri) }
    }
    runCatching { deleteDocument(exportFolderUri) }
}

internal fun copyDocumentFileDescriptors(
    contentResolver: ContentResolver,
    sourceUri: Uri,
    destinationUri: Uri,
    displayName: String
): Long {
    val sourceDescriptor = contentResolver.openFileDescriptor(sourceUri, "r")
        ?: throw LocalizedExportException(UiMessage.CouldNotOpenInputStream)
    val destinationDescriptor = contentResolver.openFileDescriptor(destinationUri, "rwt")
        ?: throw LocalizedExportException(UiMessage.CouldNotOpenOutputStream)

    sourceDescriptor.use { source ->
        destinationDescriptor.use { destination ->
            FileInputStream(source.fileDescriptor).channel.use { inputChannel ->
                FileOutputStream(destination.fileDescriptor).channel.use { outputChannel ->
                    var copiedBytes = 0L
                    while (true) {
                        val transferred = inputChannel.transferTo(
                            copiedBytes,
                            inputChannel.size() - copiedBytes,
                            outputChannel
                        )
                        if (transferred <= 0L) break
                        copiedBytes += transferred
                    }
                    outputChannel.force(true)
                    if (copiedBytes <= 0L) {
                        throw LocalizedExportException(UiMessage.CopyVerificationFailed, displayName)
                    }
                    return copiedBytes
                }
            }
        }
    }
}
