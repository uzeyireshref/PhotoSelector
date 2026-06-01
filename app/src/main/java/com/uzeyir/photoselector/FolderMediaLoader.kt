package com.uzeyir.photoselector

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

internal fun queryFolderMedia(treeUri: Uri, contentResolver: ContentResolver): FolderLoadResult {
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
