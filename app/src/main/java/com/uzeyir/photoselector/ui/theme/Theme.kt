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
    primary = StudioSurface,
    onPrimary = StudioInk,
    secondary = StudioSage,
    tertiary = StudioGreen,
    background = Color(0xFF111614),
    surface = Color(0xFF1A211E),
    surfaceVariant = Color(0xFF26302B),
    onBackground = Color(0xFFF2F4F0),
    onSurface = Color(0xFFF2F4F0),
    onSurfaceVariant = Color(0xFFD5DDD5)
)

private val LightColorScheme = lightColorScheme(
    primary = StudioInk,
    onPrimary = Color.White,
    secondary = StudioGreen,
    onSecondary = Color.White,
    tertiary = StudioSage,
    background = StudioPaper,
    onBackground = StudioInk,
    surface = StudioSurface,
    onSurface = StudioInk,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioInk,
    outline = StudioLine
)

@Composable
fun PhotoSelectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
