package com.uzeyir.photoselector

import androidx.compose.ui.graphics.Color
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
    fun selectableThemesKeepExistingThemesAndAddThreeGalleryFriendlyPalettes() {
        assertEquals(
            listOf(
                AppThemeOption.TaksimLight,
                AppThemeOption.Dark,
                AppThemeOption.RedWhite,
                AppThemeOption.HighContrastDark,
                AppThemeOption.MonoLight,
                AppThemeOption.GallerySage,
                AppThemeOption.MidnightTeal
            ),
            AppThemeOption.entries
        )
    }

    @Test
    fun darkThemesUseSoftDarkSurfacesInsteadOfPureBlack() {
        val midnight = appColorScheme(AppThemeOption.MidnightTeal)

        assertNotEquals(Color(0xFF000000), midnight.background)
        assertNotEquals(Color(0xFF000000), midnight.surface)
    }

    @Test
    fun selectableThemesUseDifferentBackgrounds() {
        val backgrounds = AppThemeOption.entries.map { appColorScheme(it).background }.toSet()

        assertEquals(AppThemeOption.entries.size, backgrounds.size)
    }
}
