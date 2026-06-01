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

fun appColorScheme(themeOption: AppThemeOption) = when (themeOption) {
    AppThemeOption.TaksimLight -> LightColorScheme
    AppThemeOption.Dark -> DarkColorScheme
    AppThemeOption.RedWhite -> RedWhiteColorScheme
    AppThemeOption.HighContrastDark -> HighContrastDarkColorScheme
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
