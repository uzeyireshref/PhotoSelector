package com.uzeyir.photoselector

import androidx.compose.ui.graphics.Color
import com.uzeyir.photoselector.ui.theme.StudioDarkBackground
import com.uzeyir.photoselector.ui.theme.StudioFavorite
import com.uzeyir.photoselector.ui.theme.StudioGreen
import com.uzeyir.photoselector.ui.theme.StudioInk
import com.uzeyir.photoselector.ui.theme.StudioPaper
import com.uzeyir.photoselector.ui.theme.StudioSage
import com.uzeyir.photoselector.ui.theme.StudioSuccess
import com.uzeyir.photoselector.ui.theme.StudioSurface
import com.uzeyir.photoselector.ui.theme.StudioSurfaceVariant
import com.uzeyir.photoselector.ui.theme.StudioWarmAccent
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePaletteTest {

    @Test
    fun brandPaletteMatchesAppLogoColors() {
        assertEquals(Color(0xFF1A0B0D), StudioInk)
        assertEquals(Color(0xFF7A0712), StudioGreen)
        assertEquals(Color(0xFF7B5B5D), StudioSage)
        assertEquals(Color(0xFFF7F1EA), StudioPaper)
        assertEquals(Color(0xFFFFFBF6), StudioSurface)
        assertEquals(Color(0xFFF0E2DD), StudioSurfaceVariant)
        assertEquals(Color(0xFF140608), StudioDarkBackground)
        assertEquals(Color(0xFFC71824), StudioFavorite)
        assertEquals(Color(0xFFB88A4A), StudioSuccess)
        assertEquals(Color(0xFFFFE0A8), StudioWarmAccent)
    }
}
