package com.uzeyir.photoselector

import android.content.SharedPreferences

interface LastFolderStore {
    fun save(folderUri: String)
    fun clear()
    fun resolveAvailableFolder(persistedReadUris: Set<String>, persistedWriteUris: Set<String>): String?
}

class SharedPreferencesLastFolderStore(
    private val preferences: SharedPreferences
) : LastFolderStore {
    override fun save(folderUri: String) {
        preferences.edit().putString(KEY_LAST_FOLDER_URI, folderUri).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY_LAST_FOLDER_URI).apply()
    }

    override fun resolveAvailableFolder(persistedReadUris: Set<String>, persistedWriteUris: Set<String>): String? {
        val savedUri = preferences.getString(KEY_LAST_FOLDER_URI, null)
        return if (savedUri != null && savedUri in persistedReadUris && savedUri in persistedWriteUris) {
            savedUri
        } else {
            clear()
            null
        }
    }

    companion object {
        const val PREFERENCES_NAME = "photoselector_preferences"
        private const val KEY_LAST_FOLDER_URI = "last_folder_uri"
    }
}
