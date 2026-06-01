package com.uzeyir.photoselector

import android.content.ClipData
import android.content.Intent
import android.net.Uri

private const val WHATSAPP_PACKAGE = "com.whatsapp"
private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
private const val DOCUMENT_SHARE_MIME_TYPE = "application/octet-stream"

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
    val clipData = ClipData.newUri(null, "PhotoSelector documents", firstUri)
    uris.drop(1).forEach { uri ->
        clipData.addItem(ClipData.Item(uri))
    }
    return clipData
}
