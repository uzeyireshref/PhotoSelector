package com.uzeyir.photoselector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.uzeyir.photoselector.AppThemeOption

private data class MattePalette(
    val background: Color,
    val backgroundSoft: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val borderSubtle: Color,
    val borderAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnAccent: Color,
    val error: Color
)

private val SignatureGoldPalette = MattePalette(
    background = Color(0xFF080A0D),
    backgroundSoft = Color(0xFF030405),
    surface = Color(0xFF111316),
    surfaceElevated = Color(0xFF1A1D21),
    surfaceMuted = Color(0xFF15181C),
    accent = Color(0xFFE8B84E),
    accentSoft = Color(0xFFF9D072),
    borderSubtle = Color(0xFF35302A),
    borderAccent = Color(0xFF5A4727),
    textPrimary = Color(0xFFF5F1EA),
    textSecondary = Color(0xFF9E9990),
    textOnAccent = Color(0xFF16110A),
    error = Color(0xFFFF8A91)
)

private val RedBlackWhitePalette = MattePalette(
    background = Color(0xFF090909),
    backgroundSoft = Color(0xFF050505),
    surface = Color(0xFF111111),
    surfaceElevated = Color(0xFF181818),
    surfaceMuted = Color(0xFF101010),
    accent = Color(0xFFC21A24),
    accentSoft = Color(0xFFD12B31),
    borderSubtle = Color(0xFF2C2C2C),
    borderAccent = Color(0xFF7A1B22),
    textPrimary = Color(0xFFF7F3F0),
    textSecondary = Color(0xFFF7F3F0),
    textOnAccent = Color(0xFFFFFFFF),
    error = Color(0xFFFF8A91)
)

private val GraphiteCopperPalette = MattePalette(
    background = Color(0xFF090806),
    backgroundSoft = Color(0xFF040403),
    surface = Color(0xFF141311),
    surfaceElevated = Color(0xFF1E1B18),
    surfaceMuted = Color(0xFF171512),
    accent = Color(0xFFD28A46),
    accentSoft = Color(0xFFF0B76B),
    borderSubtle = Color(0xFF38302A),
    borderAccent = Color(0xFF7C4F2C),
    textPrimary = Color(0xFFF4EFE7),
    textSecondary = Color(0xFFB3A99C),
    textOnAccent = Color(0xFF160D07),
    error = Color(0xFFFF8A91)
)

private val MidnightRosePalette = MattePalette(
    background = Color(0xFF0A0710),
    backgroundSoft = Color(0xFF040307),
    surface = Color(0xFF15101B),
    surfaceElevated = Color(0xFF201827),
    surfaceMuted = Color(0xFF18121F),
    accent = Color(0xFFE06A8C),
    accentSoft = Color(0xFFF4A7B9),
    borderSubtle = Color(0xFF33293B),
    borderAccent = Color(0xFF7D3A52),
    textPrimary = Color(0xFFF5EEF3),
    textSecondary = Color(0xFFB6A8B4),
    textOnAccent = Color(0xFF17070E),
    error = Color(0xFFFF8A91)
)

private fun paletteForOption(themeOption: AppThemeOption): MattePalette = when (themeOption) {
    AppThemeOption.SignatureGold -> SignatureGoldPalette
    AppThemeOption.RedBlackWhite -> RedBlackWhitePalette
    AppThemeOption.GraphiteCopper -> GraphiteCopperPalette
    AppThemeOption.MidnightRose -> MidnightRosePalette
}

private fun matteScheme(
    palette: MattePalette
) = darkColorScheme(
    primary = palette.accent,
    onPrimary = palette.textOnAccent,
    secondary = palette.accentSoft,
    onSecondary = palette.background,
    tertiary = palette.accentSoft,
    onTertiary = palette.background,
    background = palette.background,
    surface = palette.surface,
    surfaceVariant = palette.surfaceElevated,
    onBackground = palette.textPrimary,
    onSurface = palette.textPrimary,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.borderAccent,
    error = palette.error
)

fun appColorScheme(themeOption: AppThemeOption) = matteScheme(paletteForOption(themeOption))

fun appColorsForOption(themeOption: AppThemeOption): AppColors {
    val palette = paletteForOption(themeOption)
    return AppColors(
        Background = palette.background,
        BackgroundSoft = palette.backgroundSoft,
        Surface = palette.surface,
        SurfaceElevated = palette.surfaceElevated,
        SurfaceMuted = palette.surfaceMuted,
        Accent = palette.accent,
        AccentSoft = palette.accentSoft,
        AccentGlow = palette.accent.copy(alpha = 0.06f),
        BorderSubtle = palette.borderSubtle,
        BorderAccent = palette.borderAccent,
        TextPrimary = palette.textPrimary,
        TextSecondary = palette.textSecondary,
        TextMuted = palette.textSecondary,
        Error = palette.error
    )
}

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable
        get() = LocalAppShapes.current

    val spacing: AppSpacing
        @Composable
        get() = LocalAppSpacing.current
}

@Composable
fun PhotoSelectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeOption: AppThemeOption = AppThemeOption.SignatureGold,
    content: @Composable () -> Unit
) {
    val colorScheme = appColorScheme(themeOption)
    val appColors = appColorsForOption(themeOption)

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides DefaultAppTypography,
        LocalAppShapes provides DefaultAppShapes,
        LocalAppSpacing provides LocalAppSpacing.current
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
