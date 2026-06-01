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

@Composable
fun PhotoSelectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
