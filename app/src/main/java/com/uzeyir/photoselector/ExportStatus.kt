package com.uzeyir.photoselector

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
