package com.uzeyir.photoselector

import androidx.compose.ui.graphics.Color
import com.uzeyir.photoselector.ui.theme.appColorsForOption
import com.uzeyir.photoselector.ui.theme.TaksimDarkBackground
import com.uzeyir.photoselector.ui.theme.TaksimLightBackground
import com.uzeyir.photoselector.ui.theme.TaksimPrimaryRed
import com.uzeyir.photoselector.ui.theme.TaksimSurfaceDark
import com.uzeyir.photoselector.ui.theme.TaksimSurfaceLight
import com.uzeyir.photoselector.ui.theme.appColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemePaletteTest {

    @Test
    fun brandPaletteMatchesAppLogoColors() {
        assertEquals(Color(0xFFC62828), TaksimPrimaryRed)
        assertEquals(Color(0xFF120405), TaksimDarkBackground)
        assertEquals(Color(0xFFFDFCFB), TaksimLightBackground)
        assertEquals(Color(0xFF26080B), TaksimSurfaceDark)
        assertEquals(Color(0xFFF5EBEB), TaksimSurfaceLight)
    }

    @Test
    fun selectableThemesUseTheApprovedMatteDesignPalettes() {
        assertEquals(
            listOf(
                AppThemeOption.SignatureGold,
                AppThemeOption.RedBlackWhite,
                AppThemeOption.GraphiteCopper,
                AppThemeOption.MidnightRose
            ),
            AppThemeOption.entries
        )
    }

    @Test
    fun matteThemesUseOpaqueDarkSurfaces() {
        AppThemeOption.entries.forEach { theme ->
            val colors = appColorsForOption(theme)

            assertEquals(1f, colors.Surface.alpha, 0.0f)
            assertEquals(1f, colors.SurfaceElevated.alpha, 0.0f)
            assertEquals(1f, colors.SurfaceMuted.alpha, 0.0f)
        }
    }

    @Test
    fun redBlackWhiteThemeUsesRequestedPaletteFamily() {
        val colors = appColorsForOption(AppThemeOption.RedBlackWhite)

        assertEquals(Color(0xFF090909), colors.Background)
        assertEquals(Color(0xFF111111), colors.Surface)
        assertEquals(Color(0xFFC21A24), colors.Accent)
        assertEquals(Color(0xFFF7F3F0), colors.TextPrimary)
    }

    @Test
    fun graphiteCopperThemeUsesDarkGraphiteAndCopperAccent() {
        val colors = appColorsForOption(AppThemeOption.GraphiteCopper)

        assertEquals(Color(0xFF090806), colors.Background)
        assertEquals(Color(0xFF141311), colors.Surface)
        assertEquals(Color(0xFFD28A46), colors.Accent)
        assertEquals(Color(0xFFF4EFE7), colors.TextPrimary)
    }

    @Test
    fun midnightRoseThemeUsesDarkMidnightAndRoseAccent() {
        val colors = appColorsForOption(AppThemeOption.MidnightRose)

        assertEquals(Color(0xFF0A0710), colors.Background)
        assertEquals(Color(0xFF15101B), colors.Surface)
        assertEquals(Color(0xFFE06A8C), colors.Accent)
        assertEquals(Color(0xFFF5EEF3), colors.TextPrimary)
    }

    @Test
    fun selectableThemesUseDifferentBackgrounds() {
        val backgrounds = AppThemeOption.entries.map { appColorScheme(it).background }.toSet()

        assertEquals(AppThemeOption.entries.size, backgrounds.size)
    }
}
