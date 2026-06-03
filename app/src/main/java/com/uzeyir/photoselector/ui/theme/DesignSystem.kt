package com.uzeyir.photoselector.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppColors(
    val Background: Color,
    val BackgroundSoft: Color,
    val Surface: Color,
    val SurfaceElevated: Color,
    val SurfaceMuted: Color,
    val Accent: Color,
    val AccentSoft: Color,
    val AccentGlow: Color,
    val BorderSubtle: Color,
    val BorderAccent: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val Error: Color
)

data class AppTypography(
    val ScreenTitle: TextStyle,
    val SectionTitle: TextStyle,
    val CardTitle: TextStyle,
    val Body: TextStyle,
    val Caption: TextStyle,
    val ButtonText: TextStyle,
    val HelperText: TextStyle
)

data class AppShapes(
    val ScreenCard: Dp,
    val Button: Dp,
    val Thumbnail: Dp,
    val Input: Dp,
    val Chip: Dp,
    val Dialog: Dp
)

data class AppSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No AppTypography provided")
}

val LocalAppShapes = staticCompositionLocalOf<AppShapes> {
    error("No AppShapes provided")
}

val LocalAppSpacing = staticCompositionLocalOf<AppSpacing> {
    AppSpacing()
}

val DefaultAppTypography = AppTypography(
    ScreenTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    CardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    Caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    ButtonText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    HelperText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

val DefaultAppShapes = AppShapes(
    ScreenCard = 24.dp,
    Button = 16.dp,
    Thumbnail = 12.dp,
    Input = 16.dp,
    Chip = 999.dp,
    Dialog = 24.dp
)
