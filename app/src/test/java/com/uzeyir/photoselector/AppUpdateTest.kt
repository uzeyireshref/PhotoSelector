package com.uzeyir.photoselector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun lowerLocalVersionCodeRequiresUpdate() {
        val latest = AppUpdateInfo(
            versionCode = 2,
            versionName = "1.1.0",
            apkUrl = "https://example.com/app-release.apk",
            releasePageUrl = "https://example.com/releases/v1.1.0"
        )

        assertEquals(UpdateDecision.UpdateAvailable(latest), UpdatePolicy.decide(localVersionCode = 1, latest = latest))
    }

    @Test
    fun sameOrHigherLocalVersionCodeIsUpToDate() {
        val latest = AppUpdateInfo(
            versionCode = 2,
            versionName = "1.1.0",
            apkUrl = "https://example.com/app-release.apk",
            releasePageUrl = "https://example.com/releases/v1.1.0"
        )

        assertEquals(UpdateDecision.UpToDate, UpdatePolicy.decide(localVersionCode = 2, latest = latest))
        assertEquals(UpdateDecision.UpToDate, UpdatePolicy.decide(localVersionCode = 3, latest = latest))
    }

    @Test
    fun parsesReleaseMetadataAndApkAsset() {
        val json = """
            {
              "html_url": "https://github.com/uzeyireshref/PhotoSelector/releases/tag/v1.1.0",
              "body": "versionCode=2\nversionName=1.1.0\napkName=PhotoSelector-release.apk",
              "assets": [
                {
                  "name": "PhotoSelector-release.apk",
                  "browser_download_url": "https://github.com/uzeyireshref/PhotoSelector/releases/download/v1.1.0/PhotoSelector-release.apk"
                }
              ]
            }
        """.trimIndent()

        val updateInfo = GitHubReleaseParser.parse(json)

        assertEquals(2, updateInfo.versionCode)
        assertEquals("1.1.0", updateInfo.versionName)
        assertEquals("https://github.com/uzeyireshref/PhotoSelector/releases/download/v1.1.0/PhotoSelector-release.apk", updateInfo.apkUrl)
        assertEquals("https://github.com/uzeyireshref/PhotoSelector/releases/tag/v1.1.0", updateInfo.releasePageUrl)
    }

    @Test
    fun brokenReleaseMetadataFails() {
        val json = """
            {
              "html_url": "https://github.com/uzeyireshref/PhotoSelector/releases/tag/v1.1.0",
              "body": "versionName=1.1.0",
              "assets": []
            }
        """.trimIndent()

        val result = runCatching { GitHubReleaseParser.parse(json) }

        assertTrue(result.isFailure)
    }
}
