package com.uzeyir.photoselector

import android.net.Uri
import androidx.core.content.FileProvider

class DocumentShareFileProvider : FileProvider() {
    override fun getType(uri: Uri): String = DOCUMENT_SHARE_MIME_TYPE
}
