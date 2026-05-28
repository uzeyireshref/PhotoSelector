package com.uzeyir.photoselector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releasePageUrl: String
)

sealed class UpdateDecision {
    data object UpToDate : UpdateDecision()
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateDecision()
}

sealed class AppUpdateStatus {
    data object Idle : AppUpdateStatus()
    data object Checking : AppUpdateStatus()
    data object UpToDate : AppUpdateStatus()
    data class Available(val versionName: String) : AppUpdateStatus()
    data object Downloading : AppUpdateStatus()
    data object ReadyToInstall : AppUpdateStatus()
    data object Error : AppUpdateStatus()
}

object UpdatePolicy {
    fun decide(localVersionCode: Int, latest: AppUpdateInfo): UpdateDecision =
        if (localVersionCode < latest.versionCode) {
            UpdateDecision.UpdateAvailable(latest)
        } else {
            UpdateDecision.UpToDate
        }
}

object GitHubReleaseParser {
    fun parse(json: String): AppUpdateInfo {
        val release = JSONObject(json)
        val body = release.getString("body")
        val releasePageUrl = release.getString("html_url")
        val metadata = body.lineSequence()
            .mapNotNull { line ->
                val key = line.substringBefore('=', "").trim()
                val value = line.substringAfter('=', "").trim()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .toMap()

        val versionCode = metadata["versionCode"]?.toIntOrNull()
            ?: error("Release metadata does not contain a valid versionCode.")
        val versionName = metadata["versionName"]
            ?: error("Release metadata does not contain versionName.")
        val apkUrl = metadata["apkName"]?.let { apkName ->
            release.findAssetDownloadUrl(apkName)
                ?: error("Release asset not found: $apkName")
        } ?: release.findFirstApkDownloadUrl()
            ?: error("Release metadata does not contain apkName and no APK asset was found.")

        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releasePageUrl = releasePageUrl
        )
    }

    private fun JSONObject.findAssetDownloadUrl(apkName: String): String? {
        val assets = getJSONArray("assets")
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            if (asset.optString("name") == apkName) {
                return asset.optString("browser_download_url").takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun JSONObject.findFirstApkDownloadUrl(): String? {
        val assets = getJSONArray("assets")
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.optString("browser_download_url").takeIf { it.isNotBlank() }
            }
        }
        return null
    }
}

class GitHubUpdateRepository(
    private val latestReleaseUrl: String = LATEST_RELEASE_URL
) {
    suspend fun fetchLatestRelease(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val connection = URL(latestReleaseUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "PhotoSelector/${BuildConfig.VERSION_NAME}")
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                error("GitHub yanıtı başarısız: $responseCode")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            GitHubReleaseParser.parse(json)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadApk(context: Context, updateInfo: AppUpdateInfo): File = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outputFile = File(updateDir, "PhotoSelector-update-${updateInfo.versionCode}.apk")
        val connection = URL(updateInfo.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("User-Agent", "PhotoSelector/${BuildConfig.VERSION_NAME}")
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                error("APK indirilemedi: $responseCode")
            }
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/uzeyireshref/PhotoSelector/releases/latest"
    }
}

object ApkInstaller {
    fun openInstaller(context: Context, apkFile: File) {
        when (
            installerActionFor(
                sdkInt = Build.VERSION.SDK_INT,
                canRequestPackageInstalls = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
            )
        ) {
            InstallerAction.OpenUnknownSourcesSettings -> {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                return
            }

            InstallerAction.OpenPackageInstaller -> Unit
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(installIntent)
    }
}

internal enum class InstallerAction {
    OpenUnknownSourcesSettings,
    OpenPackageInstaller
}

internal fun installerActionFor(sdkInt: Int, canRequestPackageInstalls: Boolean): InstallerAction =
    if (sdkInt >= Build.VERSION_CODES.O && !canRequestPackageInstalls) {
        InstallerAction.OpenUnknownSourcesSettings
    } else {
        InstallerAction.OpenPackageInstaller
    }
