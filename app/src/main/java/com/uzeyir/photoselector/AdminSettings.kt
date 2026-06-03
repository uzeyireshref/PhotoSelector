package com.uzeyir.photoselector

import android.content.SharedPreferences

enum class AppThemeOption {
    SignatureGold,
    RedBlackWhite,
    DeepTeal
}

data class AdminSettings(
    val adminPassword: String = DEFAULT_ADMIN_PASSWORD,
    val pricing: PricingSettings = PricingSettings.Default,
    val theme: AppThemeOption = AppThemeOption.SignatureGold
) {
    fun canChangePassword(currentPassword: String, newPassword: String, repeatedPassword: String): Boolean =
        currentPassword == adminPassword &&
            newPassword.length >= MIN_ADMIN_PASSWORD_LENGTH &&
            newPassword == repeatedPassword

    companion object {
        const val DEFAULT_ADMIN_PASSWORD = "1234"
        const val MIN_ADMIN_PASSWORD_LENGTH = 4
        val Default = AdminSettings()
    }
}

interface AdminSettingsStore {
    fun load(): AdminSettings
    fun save(settings: AdminSettings)
}

class SharedPreferencesAdminSettingsStore(
    private val preferences: SharedPreferences
) : AdminSettingsStore {
    override fun load(): AdminSettings {
        val password = preferences.getString(KEY_ADMIN_PASSWORD, null)
            ?.takeIf { it.length >= AdminSettings.MIN_ADMIN_PASSWORD_LENGTH }
            ?: AdminSettings.DEFAULT_ADMIN_PASSWORD
        val pricing = PricingSettings(
            photoUnitPrice = preferences.getInt(KEY_PHOTO_UNIT_PRICE, PHOTO_UNIT_PRICE),
            videoUnitPrice = preferences.getInt(KEY_VIDEO_UNIT_PRICE, VIDEO_UNIT_PRICE),
            priceLimitCount = preferences.getInt(KEY_PRICE_LIMIT_COUNT, DEFAULT_PRICE_LIMIT_COUNT),
            discountTiers = parseDiscountTiers(
                preferences.getString(KEY_DISCOUNT_TIERS, null)
            )
        ).takeIf { it.isValid() } ?: PricingSettings.Default
        val theme = preferences.getString(KEY_THEME, null)
            ?.let { saved -> AppThemeOption.entries.firstOrNull { it.name == saved } }
            ?: AppThemeOption.SignatureGold
        return AdminSettings(
            adminPassword = password,
            pricing = pricing,
            theme = theme
        )
    }

    override fun save(settings: AdminSettings) {
        preferences.edit()
            .putString(KEY_ADMIN_PASSWORD, settings.adminPassword)
            .putInt(KEY_PHOTO_UNIT_PRICE, settings.pricing.photoUnitPrice)
            .putInt(KEY_VIDEO_UNIT_PRICE, settings.pricing.videoUnitPrice)
            .putInt(KEY_PRICE_LIMIT_COUNT, settings.pricing.priceLimitCount)
            .putString(KEY_DISCOUNT_TIERS, serializeDiscountTiers(settings.pricing.discountTiers))
            .putString(KEY_THEME, settings.theme.name)
            .apply()
    }

    companion object {
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_PHOTO_UNIT_PRICE = "photo_unit_price"
        private const val KEY_VIDEO_UNIT_PRICE = "video_unit_price"
        private const val KEY_PRICE_LIMIT_COUNT = "price_limit_count"
        private const val KEY_DISCOUNT_TIERS = "discount_tiers"
        private const val KEY_THEME = "app_theme"
    }
}

class InMemoryAdminSettingsStore(
    private var settings: AdminSettings = AdminSettings.Default
) : AdminSettingsStore {
    override fun load(): AdminSettings = settings

    override fun save(settings: AdminSettings) {
        this.settings = settings
    }
}

private fun serializeDiscountTiers(tiers: List<PricingDiscountTier>): String =
    tiers.joinToString(separator = ",") { "${it.itemCount}:${it.discountPercent}" }

private fun parseDiscountTiers(saved: String?): List<PricingDiscountTier> {
    if (saved.isNullOrBlank()) return defaultDiscountTiers
    return saved.split(",")
        .mapNotNull { token ->
            val count = token.substringBefore(":").toIntOrNull()
            val percent = token.substringAfter(":", missingDelimiterValue = "").toIntOrNull()
            if (count == null || percent == null) {
                null
            } else {
                PricingDiscountTier(itemCount = count, discountPercent = percent)
            }
        }
        .takeIf { it.isNotEmpty() }
        ?: defaultDiscountTiers
}
