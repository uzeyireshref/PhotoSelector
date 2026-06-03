package com.uzeyir.photoselector

sealed class ShareStatus {
    data object Idle : ShareStatus()

    data class Preparing(
        val preparedFiles: Int = 0,
        val totalFiles: Int = 0,
        val preparedBytes: Long = 0L,
        val totalBytes: Long? = null,
        val currentFileName: String? = null,
        val currentFileBytes: Long = 0L,
        val currentFileTotalBytes: Long? = null
    ) : ShareStatus() {
        val progressFraction: Float?
            get() = totalBytes
                ?.takeIf { it > 0L }
                ?.let { total -> (preparedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) }

        val currentFileProgressFraction: Float?
            get() = currentFileTotalBytes
                ?.takeIf { it > 0L }
                ?.let { total -> (currentFileBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) }
    }
}

internal fun shareProgressCountLabel(status: ShareStatus.Preparing): String? =
    if (status.totalFiles > 0) "${status.preparedFiles.coerceIn(0, status.totalFiles)}/${status.totalFiles}" else null
