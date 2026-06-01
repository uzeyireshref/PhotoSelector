package com.uzeyir.photoselector.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.uzeyir.photoselector.AppThemeOption

private val DarkColorScheme = darkColorScheme(
    primary = TaksimPrimaryRed,
    onPrimary = TaksimTextWhite,
    secondary = TaksimBurgundy,
    onSecondary = TaksimTextWhite,
    tertiary = TaksimWarmAccent,
    onTertiary = TaksimTextDark,
    background = TaksimDarkBackground,
    surface = TaksimSurfaceDark,
    surfaceVariant = TaksimBurgundy,
    onBackground = TaksimTextWhite,
    onSurface = TaksimTextWhite,
    onSurfaceVariant = TaksimTextWhite,
    outline = TaksimOutlineDark,
    error = TaksimError
)

private val LightColorScheme = lightColorScheme(
    primary = TaksimPrimaryRed,
    onPrimary = TaksimTextWhite,
    secondary = TaksimBurgundy,
    onSecondary = TaksimTextWhite,
    tertiary = TaksimSuccess,
    onTertiary = TaksimTextWhite,
    background = TaksimLightBackground,
    onBackground = TaksimTextDark,
    surface = TaksimSurfaceLight,
    onSurface = TaksimTextDark,
    surfaceVariant = TaksimWarmAccent,
    onSurfaceVariant = TaksimTextDark,
    outline = TaksimOutlineLight,
    error = TaksimError
)

private val RedWhiteColorScheme = lightColorScheme(
    primary = TaksimBurgundy,
    onPrimary = TaksimTextWhite,
    secondary = TaksimPrimaryRed,
    onSecondary = TaksimTextWhite,
    tertiary = TaksimWarmAccent,
    onTertiary = TaksimTextDark,
    background = Color(0xFFFFFFFF),
    onBackground = TaksimTextDark,
    surface = Color(0xFFFFF7F7),
    onSurface = TaksimTextDark,
    surfaceVariant = Color(0xFFFFE5E5),
    onSurfaceVariant = TaksimBurgundy,
    outline = TaksimOutlineLight,
    error = TaksimError
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFFF5252),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFF69F0AE),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF),
    error = Color(0xFFFF8A80)
)

private val MonoLightColorScheme = lightColorScheme(
    primary = Color(0xFF242424),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF626262),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF8A735E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF3C4043),
    outline = Color(0xFFDADCE0),
    error = TaksimError
)

private val GallerySageColorScheme = lightColorScheme(
    primary = Color(0xFF3D5A4B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF69756D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF8A6F45),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F6F1),
    onBackground = Color(0xFF1F241F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F241F),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF3F4A41),
    outline = Color(0xFFB8C4B7),
    error = TaksimError
)

private val MidnightTealColorScheme = darkColorScheme(
    primary = Color(0xFF8FCFC3),
    onPrimary = Color(0xFF0B2A25),
    secondary = Color(0xFFB3C8C2),
    onSecondary = Color(0xFF1F2D2A),
    tertiary = Color(0xFFE2C48B),
    onTertiary = Color(0xFF382B10),
    background = Color(0xFF10191B),
    onBackground = Color(0xFFE3ECEB),
    surface = Color(0xFF192326),
    onSurface = Color(0xFFE3ECEB),
    surfaceVariant = Color(0xFF263336),
    onSurfaceVariant = Color(0xFFC3D0CE),
    outline = Color(0xFF4F6265),
    error = Color(0xFFFFB4AB)
)

fun appColorScheme(themeOption: AppThemeOption) = when (themeOption) {
    AppThemeOption.TaksimLight -> LightColorScheme
    AppThemeOption.Dark -> DarkColorScheme
    AppThemeOption.RedWhite -> RedWhiteColorScheme
    AppThemeOption.HighContrastDark -> HighContrastDarkColorScheme
    AppThemeOption.MonoLight -> MonoLightColorScheme
    AppThemeOption.GallerySage -> GallerySageColorScheme
    AppThemeOption.MidnightTeal -> MidnightTealColorScheme
}

@Composable
fun PhotoSelectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    themeOption: AppThemeOption = if (darkTheme) AppThemeOption.Dark else AppThemeOption.TaksimLight,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> appColorScheme(themeOption)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
