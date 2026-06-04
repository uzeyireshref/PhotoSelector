package com.uzeyir.photoselector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSettingsTest {

    @Test
    fun defaultAdminSettingsUsePlannedPasswordPricingAndTheme() {
        val settings = AdminSettings.Default

        assertEquals("1234", settings.adminPassword)
        assertEquals(300, settings.pricing.photoUnitPrice)
        assertEquals(1000, settings.pricing.videoUnitPrice)
        assertEquals(10, settings.pricing.priceLimitCount)
        assertEquals(AppThemeOption.SignatureGold, settings.theme)
        assertEquals(null, settings.preferredSdCardFolderUri)
        assertEquals(emptyList<AuthorizedSdCardFolder>(), settings.authorizedSdCardFolders)
    }

    @Test
    fun passwordChangeRequiresCurrentPasswordMinimumLengthAndConfirmation() {
        val settings = AdminSettings.Default

        assertFalse(settings.canChangePassword(currentPassword = "0000", newPassword = "4567", repeatedPassword = "4567"))
        assertFalse(settings.canChangePassword(currentPassword = "1234", newPassword = "456", repeatedPassword = "456"))
        assertFalse(settings.canChangePassword(currentPassword = "1234", newPassword = "4567", repeatedPassword = "4568"))
        assertTrue(settings.canChangePassword(currentPassword = "1234", newPassword = "4567", repeatedPassword = "4567"))
    }

    @Test
    fun pricingSettingsRejectInvalidValues() {
        assertFalse(AdminSettings.Default.pricing.copy(photoUnitPrice = -1).isValid())
        assertFalse(AdminSettings.Default.pricing.copy(videoUnitPrice = -1).isValid())
        assertFalse(AdminSettings.Default.pricing.copy(priceLimitCount = 0).isValid())
        assertFalse(AdminSettings.Default.pricing.copy(discountTiers = listOf(PricingDiscountTier(itemCount = 4, discountPercent = 101))).isValid())
        assertFalse(
            AdminSettings.Default.pricing.copy(
                discountTiers = listOf(
                    PricingDiscountTier(itemCount = 4, discountPercent = 5),
                    PricingDiscountTier(itemCount = 4, discountPercent = 10)
                )
            ).isValid()
        )
        assertTrue(AdminSettings.Default.pricing.isValid())
    }

    @Test
    fun inMemoryAdminSettingsStorePersistsChanges() {
        val store = InMemoryAdminSettingsStore()
        val updated = AdminSettings.Default.copy(
            adminPassword = "9876",
            pricing = AdminSettings.Default.pricing.copy(photoUnitPrice = 500),
            theme = AppThemeOption.GraphiteCopper,
            preferredSdCardFolderUri = "content://com.android.externalstorage.documents/tree/1234-5678%3ADCIM",
            authorizedSdCardFolders = listOf(
                AuthorizedSdCardFolder(
                    volumeId = "1234-5678",
                    folderUri = "content://com.android.externalstorage.documents/tree/1234-5678%3ADCIM",
                    relativePath = "DCIM"
                )
            )
        )

        store.save(updated)

        assertEquals(updated, store.load())
    }

    @Test
    fun preferredSdCardFolderResolvesOnlyWithReadAndWritePermissions() {
        val settings = AdminSettings.Default.copy(
            preferredSdCardFolderUri = "content://tree/custom-folder"
        )

        assertEquals(
            "content://tree/custom-folder",
            resolvePreferredSdCardFolder(
                settings = settings,
                persistedReadUris = setOf("content://tree/custom-folder"),
                persistedWriteUris = setOf("content://tree/custom-folder")
            )
        )
        assertEquals(
            null,
            resolvePreferredSdCardFolder(
                settings = settings,
                persistedReadUris = setOf("content://tree/custom-folder"),
                persistedWriteUris = emptySet()
            )
        )
        assertEquals(
            null,
            resolvePreferredSdCardFolder(
                settings = AdminSettings.Default,
                persistedReadUris = setOf("content://tree/custom-folder"),
                persistedWriteUris = setOf("content://tree/custom-folder")
            )
        )
    }

    @Test
    fun treeUriIsParsedIntoAuthorizedSdCardFolder() {
        assertEquals(
            AuthorizedSdCardFolder(
                volumeId = "1234-5678",
                folderUri = "content://com.android.externalstorage.documents/tree/1234-5678%3ADCIM%2FSECILENLER",
                relativePath = "DCIM/SECILENLER"
            ),
            authorizedSdCardFolderFromTreeUri(
                "content://com.android.externalstorage.documents/tree/1234-5678%3ADCIM%2FSECILENLER"
            )
        )
    }

    @Test
    fun authorizedSdCardFolderUpsertReplacesOnlySameVolume() {
        val first = AuthorizedSdCardFolder(
            volumeId = "1111-2222",
            folderUri = "content://tree/1111-2222%3ADCIM",
            relativePath = "DCIM"
        )
        val second = AuthorizedSdCardFolder(
            volumeId = "3333-4444",
            folderUri = "content://tree/3333-4444%3ADCIM",
            relativePath = "DCIM"
        )
        val replacement = AuthorizedSdCardFolder(
            volumeId = "1111-2222",
            folderUri = "content://tree/1111-2222%3ADCIM%2FSELECTED",
            relativePath = "DCIM/SELECTED"
        )

        assertEquals(
            listOf(replacement, second),
            upsertAuthorizedSdCardFolder(listOf(first, second), replacement)
        )
    }

    @Test
    fun authorizedSdCardFolderResolvesByVolumeOnlyWithReadAndWritePermissions() {
        val folder = AuthorizedSdCardFolder(
            volumeId = "1234-5678",
            folderUri = "content://tree/1234-5678%3ADCIM",
            relativePath = "DCIM"
        )
        val settings = AdminSettings.Default.copy(authorizedSdCardFolders = listOf(folder))

        assertEquals(
            folder,
            resolveAuthorizedSdCardFolder(
                settings = settings,
                volumeId = "1234-5678",
                persistedReadUris = setOf(folder.folderUri),
                persistedWriteUris = setOf(folder.folderUri)
            )
        )
        assertEquals(
            null,
            resolveAuthorizedSdCardFolder(
                settings = settings,
                volumeId = "9999-0000",
                persistedReadUris = setOf(folder.folderUri),
                persistedWriteUris = setOf(folder.folderUri)
            )
        )
        assertEquals(
            null,
            resolveAuthorizedSdCardFolder(
                settings = settings,
                volumeId = "1234-5678",
                persistedReadUris = setOf(folder.folderUri),
                persistedWriteUris = emptySet()
            )
        )
    }

    @Test
    fun missingPermissionDoesNotMeanPreferredSdCardFolderShouldBeForgotten() {
        val settings = AdminSettings.Default.copy(
            preferredSdCardFolderUri = "content://tree/custom-folder"
        )

        val resolution = preferredSdCardFolderResolution(
            settings = settings,
            persistedReadUris = emptySet(),
            persistedWriteUris = emptySet()
        )

        assertEquals(null, resolution.folderUri)
        assertEquals(true, resolution.hasSavedPreference)
    }
}
