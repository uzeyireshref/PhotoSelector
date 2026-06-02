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
        assertEquals(AppThemeOption.Dark, settings.theme)
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
            theme = AppThemeOption.MidnightTeal
        )

        store.save(updated)

        assertEquals(updated, store.load())
    }
}
