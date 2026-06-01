package com.uzeyir.photoselector

import android.content.ContentResolver
import android.net.Uri
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatExportFolderTimestamp(date: Date): String =
    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)

internal fun exportFolderName(date: Date, attempt: Int = 1): String =
    exportFolderName(formatExportFolderTimestamp(date), attempt)

internal fun exportFolderName(baseName: String, attempt: Int = 1): String =
    if (attempt <= 1) baseName else "${baseName}_$attempt"

internal class LocalizedExportException(
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
