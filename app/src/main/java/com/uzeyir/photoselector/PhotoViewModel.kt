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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
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
    val lastModified: Long = 0,
    val sizeBytes: Long? = null
)

data class FolderDocumentData(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val lastModified: Long = 0,
    val sizeBytes: Long? = null
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

private val defaultDiscountTiers = listOf(
    PricingDiscountTier(photoCount = 4, discountPercent = 5),
    PricingDiscountTier(photoCount = 5, discountPercent = 10),
    PricingDiscountTier(photoCount = 6, discountPercent = 15),
    PricingDiscountTier(photoCount = 7, discountPercent = 20),
    PricingDiscountTier(photoCount = 8, discountPercent = 25),
    PricingDiscountTier(photoCount = 9, discountPercent = 30),
    PricingDiscountTier(photoCount = 10, discountPercent = 35)
)

internal fun discountedPayablePrice(count: Int, unitPrice: Int): Int {
    val billableCount = count.coerceAtMost(10)
    val subtotal = billableCount * unitPrice
    val discountPercent = defaultDiscountTiers
        .lastOrNull { billableCount >= it.photoCount }
        ?.discountPercent
        ?: 0
    return subtotal * (100 - discountPercent) / 100
}

sealed class ExportStatus {
    data object Idle : ExportStatus()
    data class Copying(
        val copiedFiles: Int = 0,
        val totalFiles: Int = 0,
        val copiedBytes: Long = 0L,
        val totalBytes: Long? = null,
        val currentFileName: String? = null,
        val currentFileBytes: Long = 0L,
        val currentFileTotalBytes: Long? = null
    ) : ExportStatus() {
        val progressFraction: Float?
            get() = totalBytes
                ?.takeIf { it > 0L }
                ?.let { total -> (copiedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) }

        val currentFileProgressFraction: Float?
            get() = currentFileTotalBytes
                ?.takeIf { it > 0L }
                ?.let { total -> (currentFileBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) }
    }

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
    Gallery
}

fun FolderDocumentData.toMediaItemOrNull(): MediaItemData? {
    if (displayName.isHiddenDocumentName()) return null

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
        lastModified = lastModified,
        sizeBytes = sizeBytes
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

private fun String.baseNameKey(): String =
    baseName().lowercase(Locale.US)

private fun String.extension(): String =
    substringAfterLast('.', "")

private fun String.isHiddenDocumentName(): Boolean =
    startsWith(".")

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

    private val pricePerPhoto = 300
    private val pricePerVideo = 1000

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

    private fun discountedPrice(count: Int, unitPrice: Int): Int =
        discountedPayablePrice(count, unitPrice)

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
        val exportDocuments = newDocuments.filter { document ->
            document.displayName.extension().uppercase(Locale.US) in rawExtensions
        }
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
        if (photo.mediaType != MediaType.Photo) return emptyList()
        return rawDocumentsByBaseName[photo.displayName.baseNameKey()].orEmpty()
    }

    private fun rebuildRawDocumentIndex(documents: List<FolderDocumentData>) {
        rawDocumentsByBaseName.clear()
        rawDocumentsByBaseName.putAll(
            documents
                .filter { document ->
                    document.displayName.extension().uppercase(Locale.US) in rawExtensions
                }
                .groupBy { document -> document.displayName.baseNameKey() }
                .mapValues { (_, rawDocuments) ->
                    rawDocuments.sortedBy { it.displayName.lowercase(Locale.US) }
                }
        )
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
            runCatching {
                copySelectedFiles(
                    contentResolver = contentResolver,
                    treeUri = treeUri,
                    folderName = folderName,
                    selectedMedia = selectedMedia,
                    includeRawFiles = includeRawFiles,
                    onProgress = { status ->
                        runBlocking(Dispatchers.Main.immediate) {
                            exportStatus = status
                        }
                    }
                )
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

    private fun queryFolderMedia(treeUri: Uri, contentResolver: ContentResolver): FolderLoadResult {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )

        val documents = mutableListOf<FolderDocumentData>()
        val mediaItems = mutableListOf<MediaItemData>()
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val lastModifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumn)
                val mime = cursor.getString(mimeColumn).orEmpty()
                val name = cursor.getString(nameColumn).orEmpty()
                val lastModified = if (lastModifiedColumn >= 0 && !cursor.isNull(lastModifiedColumn)) {
                    cursor.getLong(lastModifiedColumn)
                } else {
                    0L
                }
                val sizeBytes = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    cursor.getLong(sizeColumn).takeIf { it >= 0L }
                } else {
                    null
                }
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                val document = FolderDocumentData(
                    uri = uri,
                    displayName = name,
                    mimeType = mime,
                    lastModified = lastModified,
                    sizeBytes = sizeBytes
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

    private suspend fun copySelectedFiles(
        contentResolver: ContentResolver,
        treeUri: Uri,
        folderName: String,
        selectedMedia: List<MediaItemData>,
        includeRawFiles: Boolean,
        onProgress: (ExportStatus.Copying) -> Unit
    ): ExportStatus {
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val (exportFolderUri, actualFolderName) = createExportFolder(contentResolver, parentDocumentUri, folderName)

        val filesToCopy = selectedTransferFiles(
            selectedMedia = selectedMedia,
            includeRawFiles = includeRawFiles,
            matchingRawFiles = ::matchingRawFilesFor
        )
        val totalBytes = filesToCopy
            .map { it.sizeBytes }
            .takeIf { sizes -> sizes.all { it != null } }
            ?.sumOf { it ?: 0L }
        val copiedBytesByFile = LongArray(filesToCopy.size)
        var lastProgressPublishMs = 0L

        fun totalCopiedBytes(): Long =
            copiedBytesByFile.sum()

        fun publishProgress(
            index: Int,
            copiedCount: Int,
            currentFileName: String,
            currentFileBytes: Long,
            currentFileTotalBytes: Long?,
            force: Boolean = false
        ) {
            val now = System.currentTimeMillis()
            if (!force && now - lastProgressPublishMs < 150L) return
            lastProgressPublishMs = now
            onProgress(
                ExportStatus.Copying(
                    copiedFiles = copiedCount,
                    totalFiles = filesToCopy.size,
                    copiedBytes = totalCopiedBytes(),
                    totalBytes = totalBytes,
                    currentFileName = currentFileName,
                    currentFileBytes = currentFileBytes,
                    currentFileTotalBytes = currentFileTotalBytes
                )
            )
        }

        val createdFileUris = mutableListOf<Uri>()
        try {
            filesToCopy.forEachIndexed { index, source ->
                publishProgress(
                    index = index,
                    copiedCount = index,
                    currentFileName = source.displayName,
                    currentFileBytes = 0L,
                    currentFileTotalBytes = source.sizeBytes,
                    force = true
                )
                val destinationUri = DocumentsContract.createDocument(
                    contentResolver,
                    exportFolderUri,
                    source.mimeType.ifBlank { "application/octet-stream" },
                    source.displayName
                ) ?: localizedError(UiMessage.CouldNotCreateFile, source.displayName)
                createdFileUris.add(destinationUri)
                copyDocument(
                    contentResolver = contentResolver,
                    sourceUri = source.uri,
                    destinationUri = destinationUri,
                    displayName = source.displayName,
                    expectedBytes = source.sizeBytes,
                    onProgress = { copiedBytes, _ ->
                        copiedBytesByFile[index] = copiedBytes
                        publishProgress(
                            index = index,
                            copiedCount = index,
                            currentFileName = source.displayName,
                            currentFileBytes = copiedBytes,
                            currentFileTotalBytes = source.sizeBytes
                        )
                    }
                )
                copiedBytesByFile[index] = source.sizeBytes ?: copiedBytesByFile[index]
                publishProgress(
                    index = index,
                    copiedCount = index + 1,
                    currentFileName = source.displayName,
                    currentFileBytes = copiedBytesByFile[index],
                    currentFileTotalBytes = source.sizeBytes,
                    force = true
                )
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

        return ExportStatus.Success(folderName = actualFolderName, copiedFiles = filesToCopy.size)
    }

    private fun createExportFolder(
        contentResolver: ContentResolver,
        parentDocumentUri: Uri,
        baseFolderName: String
    ): Pair<Uri, String> {
        for (attempt in 1..MAX_EXPORT_FOLDER_CREATE_ATTEMPTS) {
            val candidateName = exportFolderName(baseFolderName, attempt)
            val folderUri = DocumentsContract.createDocument(
                contentResolver,
                parentDocumentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                candidateName
            )
            if (folderUri != null) return folderUri to candidateName
        }
        localizedError(UiMessage.CouldNotCreateExportFolder)
    }

    private fun copyDocument(
        contentResolver: ContentResolver,
        sourceUri: Uri,
        destinationUri: Uri,
        displayName: String,
        expectedBytes: Long?,
        onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
    ) {
        val resolvedExpectedBytes = expectedBytes ?: queryDocumentSize(contentResolver, sourceUri)
        val copiedBytes = copyDocumentFileDescriptors(
            contentResolver = contentResolver,
            sourceUri = sourceUri,
            destinationUri = destinationUri,
            displayName = displayName,
            expectedBytes = resolvedExpectedBytes,
            onProgress = onProgress
        )
        verifyCopiedDocumentSize(contentResolver, destinationUri, copiedBytes, resolvedExpectedBytes, displayName)
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
        const val MAX_EXPORT_FOLDER_CREATE_ATTEMPTS = 20
    }
}

internal fun formatExportFolderTimestamp(date: Date): String =
    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)

internal fun exportFolderName(date: Date, attempt: Int = 1): String =
    exportFolderName(formatExportFolderTimestamp(date), attempt)

internal fun exportFolderName(baseName: String, attempt: Int = 1): String =
    if (attempt <= 1) baseName else "${baseName}_$attempt"

private class LocalizedExportException(
    val uiMessage: UiMessage,
    val argument: String? = null
) : IllegalStateException(uiMessage.name)

internal fun copyDocumentBytes(
    input: InputStream,
    output: OutputStream,
    displayName: String,
    expectedBytes: Long? = null,
    onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copiedBytes = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        output.write(buffer, 0, read)
        copiedBytes += read
        onProgress(copiedBytes, expectedBytes)
    }
    output.flush()
    if (copiedBytes <= 0L) {
        throw LocalizedExportException(UiMessage.CopyVerificationFailed, displayName)
    }
    return copiedBytes
}

internal fun shouldBeginExport(exportStatus: ExportStatus): Boolean =
    exportStatus !is ExportStatus.Copying

internal fun exportProgressCountLabel(status: ExportStatus.Copying): String? =
    if (status.totalFiles > 0) "${status.copiedFiles.coerceIn(0, status.totalFiles)}/${status.totalFiles}" else null

internal fun selectedTransferFiles(
    selectedMedia: List<MediaItemData>,
    includeRawFiles: Boolean,
    matchingRawFiles: (MediaItemData) -> List<FolderDocumentData>
): List<FolderDocumentData> =
    selectedMedia.flatMap { media ->
        when (media.mediaType) {
            MediaType.Photo -> {
                val jpg = media.toFolderDocumentDataForTransfer()
                if (includeRawFiles) listOf(jpg) + matchingRawFiles(media) else listOf(jpg)
            }
            MediaType.Video -> listOf(media.toFolderDocumentDataForTransfer())
        }
    }

private fun MediaItemData.toFolderDocumentDataForTransfer(): FolderDocumentData =
    FolderDocumentData(
        uri = uri,
        displayName = displayName,
        lastModified = lastModified,
        sizeBytes = sizeBytes,
        mimeType = mimeType.ifBlank {
            if (mediaType == MediaType.Photo) "image/jpeg" else "application/octet-stream"
        }
    )

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
    displayName: String,
    expectedBytes: Long? = null,
    onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
): Long {
    val channelCopiedBytes = runCatching {
        copyDocumentFileDescriptorsWithChannel(contentResolver, sourceUri, destinationUri, displayName, onProgress)
    }.getOrNull()
    if (channelCopiedBytes != null && channelCopiedBytes > 0L) {
        return channelCopiedBytes
    }

    return copyDocumentFileDescriptorsWithStreams(contentResolver, sourceUri, destinationUri, displayName, expectedBytes, onProgress)
}

private fun copyDocumentFileDescriptorsWithChannel(
    contentResolver: ContentResolver,
    sourceUri: Uri,
    destinationUri: Uri,
    displayName: String,
    onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit
): Long {
    val sourceDescriptor = contentResolver.openFileDescriptor(sourceUri, "r")
        ?: throw LocalizedExportException(UiMessage.CouldNotOpenInputStream)
    val destinationDescriptor = contentResolver.openFileDescriptor(destinationUri, "rwt")
        ?: throw LocalizedExportException(UiMessage.CouldNotOpenOutputStream)

    sourceDescriptor.use { source ->
        destinationDescriptor.use { destination ->
            FileInputStream(source.fileDescriptor).channel.use { inputChannel ->
                FileOutputStream(destination.fileDescriptor).channel.use { outputChannel ->
                    val inputSize = inputChannel.size()
                    val copiedBytes = copyChannelBytes(inputChannel, outputChannel, onProgress)
                    if (copiedBytes != inputSize) {
                        throw LocalizedExportException(UiMessage.CopyVerificationFailed, displayName)
                    }
                    return copiedBytes
                }
            }
        }
    }
}

private fun copyDocumentFileDescriptorsWithStreams(
    contentResolver: ContentResolver,
    sourceUri: Uri,
    destinationUri: Uri,
    displayName: String,
    expectedBytes: Long?,
    onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit
): Long {
    val sourceDescriptor = contentResolver.openFileDescriptor(sourceUri, "r")
        ?: throw LocalizedExportException(UiMessage.CouldNotOpenInputStream)
    val destinationDescriptor = contentResolver.openFileDescriptor(destinationUri, "rwt")
        ?: throw LocalizedExportException(UiMessage.CouldNotOpenOutputStream)

    sourceDescriptor.use { source ->
        destinationDescriptor.use { destination ->
            FileInputStream(source.fileDescriptor).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    return copyDocumentBytes(input, output, displayName, expectedBytes, onProgress)
                }
            }
        }
    }
}

internal fun copyChannelBytes(
    inputChannel: FileChannel,
    outputChannel: FileChannel,
    onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
): Long {
    val inputSize = inputChannel.size()
    var copiedBytes = 0L
    while (copiedBytes < inputSize) {
        val transferred = inputChannel.transferTo(
            copiedBytes,
            inputSize - copiedBytes,
            outputChannel
        )
        if (transferred <= 0L) break
        copiedBytes += transferred
        onProgress(copiedBytes, inputSize)
    }
    return copiedBytes
}
