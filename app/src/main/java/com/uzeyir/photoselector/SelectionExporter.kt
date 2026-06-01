package com.uzeyir.photoselector

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns

internal class SelectionExporter(
    private val contentResolver: ContentResolver,
    private val treeUri: Uri,
    private val selectedMedia: List<MediaItemData>,
    private val includeRawFiles: Boolean,
    private val matchingRawFiles: (MediaItemData) -> List<FolderDocumentData>,
    private val onProgress: (ExportStatus.Copying) -> Unit
) {
    suspend fun export(folderName: String): ExportStatus {
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val (exportFolderUri, actualFolderName) = createExportFolder(parentDocumentUri, folderName)

        val filesToCopy = selectedTransferFiles(
            selectedMedia = selectedMedia,
            includeRawFiles = includeRawFiles,
            matchingRawFiles = matchingRawFiles
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
        sourceUri: Uri,
        destinationUri: Uri,
        displayName: String,
        expectedBytes: Long?,
        onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
    ) {
        val resolvedExpectedBytes = expectedBytes ?: queryDocumentSize(sourceUri)
        val copiedBytes = copyDocumentFileDescriptors(
            contentResolver = contentResolver,
            sourceUri = sourceUri,
            destinationUri = destinationUri,
            displayName = displayName,
            expectedBytes = resolvedExpectedBytes,
            onProgress = onProgress
        )
        verifyCopiedDocumentSize(destinationUri, copiedBytes, resolvedExpectedBytes, displayName)
    }

    private fun verifyCopiedDocumentSize(
        destinationUri: Uri,
        copiedBytes: Long,
        expectedBytes: Long?,
        displayName: String
    ) {
        val reportedSize = queryDocumentSize(destinationUri)

        if (expectedBytes != null && expectedBytes != copiedBytes) {
            localizedError(UiMessage.CopyVerificationFailed, displayName)
        }
        if (reportedSize != null && reportedSize != copiedBytes) {
            localizedError(UiMessage.CopyVerificationFailed, displayName)
        }
    }

    private fun queryDocumentSize(documentUri: Uri): Long? =
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

    private companion object {
        const val MAX_EXPORT_FOLDER_CREATE_ATTEMPTS = 20
    }
}
