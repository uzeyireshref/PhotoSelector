package com.uzeyir.photoselector

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val WHATSAPP_PACKAGE = "com.whatsapp"
private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
internal const val DOCUMENT_SHARE_MIME_TYPE = "application/octet-stream"
private const val DOCUMENT_SHARE_CACHE_DIR = "whatsapp-documents"

internal data class WhatsAppDocumentShareRequest(
    val uris: List<Uri>,
    val mimeType: String = DOCUMENT_SHARE_MIME_TYPE,
    val packageName: String = WHATSAPP_PACKAGE
)

internal fun whatsAppDocumentShareRequest(files: List<FolderDocumentData>): WhatsAppDocumentShareRequest =
    WhatsAppDocumentShareRequest(uris = files.map { it.uri })

internal fun whatsAppPackageCandidates(): List<String> =
    listOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)

internal fun whatsAppDocumentShareIntents(files: List<FolderDocumentData>): List<Intent> =
    whatsAppPackageCandidates().map { packageName ->
        whatsAppDocumentShareIntent(files, packageName = packageName)
    }

internal fun fallbackDocumentShareIntent(files: List<FolderDocumentData>): Intent =
    whatsAppDocumentShareIntent(files, packageName = null)

suspend fun prepareWhatsAppDocumentShareFiles(
    context: Context,
    files: List<FolderDocumentData>,
    onProgress: (ShareStatus.Preparing) -> Unit = {}
): List<FolderDocumentData> = withContext(Dispatchers.IO) {
    val shareDir = File(context.cacheDir, DOCUMENT_SHARE_CACHE_DIR).apply {
        deleteRecursively()
        mkdirs()
    }
    val totalBytes = files
        .map { it.sizeBytes }
        .takeIf { sizes -> sizes.all { it != null } }
        ?.sumOf { it ?: 0L }
    val preparedBytesByFile = LongArray(files.size)

    fun totalPreparedBytes(): Long =
        preparedBytesByFile.sum()

    fun publishProgress(
        preparedCount: Int,
        currentFileName: String,
        currentFileBytes: Long,
        currentFileTotalBytes: Long?
    ) {
        onProgress(
            ShareStatus.Preparing(
                preparedFiles = preparedCount,
                totalFiles = files.size,
                preparedBytes = totalPreparedBytes(),
                totalBytes = totalBytes,
                currentFileName = currentFileName,
                currentFileBytes = currentFileBytes,
                currentFileTotalBytes = currentFileTotalBytes
            )
        )
    }

    files.mapIndexed { index, file ->
        publishProgress(
            preparedCount = index,
            currentFileName = file.displayName,
            currentFileBytes = 0L,
            currentFileTotalBytes = file.sizeBytes
        )
        val outputFile = File(shareDir, documentShareCacheFileName(file.displayName, index))
        context.contentResolver.openInputStream(file.uri).use { input ->
            requireNotNull(input) { "Could not open ${file.displayName}" }
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copiedBytes = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copiedBytes += read
                    preparedBytesByFile[index] = copiedBytes
                    publishProgress(
                        preparedCount = index,
                        currentFileName = file.displayName,
                        currentFileBytes = copiedBytes,
                        currentFileTotalBytes = file.sizeBytes
                    )
                }
            }
        }
        preparedBytesByFile[index] = file.sizeBytes ?: outputFile.length()
        publishProgress(
            preparedCount = index + 1,
            currentFileName = file.displayName,
            currentFileBytes = preparedBytesByFile[index],
            currentFileTotalBytes = file.sizeBytes
        )
        FolderDocumentData(
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.documentshareprovider",
                outputFile
            ),
            displayName = file.displayName,
            mimeType = DOCUMENT_SHARE_MIME_TYPE,
            lastModified = file.lastModified,
            sizeBytes = outputFile.length()
        )
    }
}

internal fun documentShareCacheFileName(displayName: String, index: Int): String {
    val safeName = displayName
        .replace('\\', '_')
        .replace('/', '_')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim()
        .trim('.', '_')
        .ifBlank { "document" }
    return "${(index + 1).toString().padStart(3, '0')}_$safeName"
}

internal fun whatsAppDocumentShareIntent(
    files: List<FolderDocumentData>,
    packageName: String? = WHATSAPP_PACKAGE
): Intent {
    val request = whatsAppDocumentShareRequest(files)
    val uris = ArrayList(request.uris)
    return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = request.mimeType
        packageName?.let { setPackage(it) }
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = shareClipData(uris)
    }
}

private fun shareClipData(uris: List<Uri>): ClipData? {
    val firstUri = uris.firstOrNull() ?: return null
    val clipData = ClipData.newRawUri("PhotoSelector documents", firstUri)
    uris.drop(1).forEach { uri ->
        clipData.addItem(ClipData.Item(uri))
    }
    return clipData
}
