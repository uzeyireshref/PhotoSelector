package com.uzeyir.photoselector

import java.util.Locale

private val rawExtensions = setOf("CR3", "CR2", "NEF", "ARW", "DNG", "RAF", "RW2", "ORF")

internal fun exportableRawDocuments(documents: List<FolderDocumentData>): List<FolderDocumentData> =
    documents.filter { document -> document.isRawDocument() }

internal fun rawDocumentsByBaseName(documents: List<FolderDocumentData>): Map<String, List<FolderDocumentData>> =
    documents
        .filter { document -> document.isRawDocument() }
        .groupBy { document -> document.displayName.baseNameKey() }
        .mapValues { (_, rawDocuments) ->
            rawDocuments.sortedBy { it.displayName.lowercase(Locale.US) }
        }

internal fun matchingRawFilesFor(
    rawDocumentsByBaseName: Map<String, List<FolderDocumentData>>,
    photo: MediaItemData
): List<FolderDocumentData> {
    if (photo.mediaType != MediaType.Photo) return emptyList()
    return rawDocumentsByBaseName[photo.displayName.baseNameKey()].orEmpty()
}

private fun FolderDocumentData.isRawDocument(): Boolean =
    displayName.extension().uppercase(Locale.US) in rawExtensions
