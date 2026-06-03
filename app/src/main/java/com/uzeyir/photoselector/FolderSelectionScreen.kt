package com.uzeyir.photoselector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzeyir.photoselector.ui.theme.AppTheme

@Composable
fun FolderSelectionScreen(
    onFolderSelected: () -> Unit,
    onOpenSdCard: () -> Unit,
    updateStatus: AppUpdateStatus,
    isLoadingMedia: Boolean,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    strings: LocalizedStrings,
    onCheckUpdate: () -> Unit,
    onAdminClick: () -> Unit = {}
) {
    val startSubtitle = when (language) {
        AppLanguage.Turkish -> "Fotoğraf ve videolarınızı seçmek için kaynak klasörü açın."
        AppLanguage.English -> "Open the source folder to choose your photos and videos."
    }
    val storageHint = when (language) {
        AppLanguage.Turkish -> "Telefon hafızası veya SD karttan devam edin."
        AppLanguage.English -> "Continue from phone storage or an SD card."
    }

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(34.dp))
            StartScreenPanel(
                title = strings.folderTitle,
                subtitle = startSubtitle,
                storageHint = storageHint,
                onFolderSelected = onFolderSelected,
                onOpenSdCard = onOpenSdCard,
                updateStatus = updateStatus,
                isLoadingMedia = isLoadingMedia,
                strings = strings,
                onCheckUpdate = onCheckUpdate,
                modifier = Modifier.widthIn(max = 660.dp)
            )
            Spacer(modifier = Modifier.height(34.dp))
        }

        LanguageSelector(
            selectedLanguage = language,
            strings = strings,
            onLanguageSelected = onLanguageSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 30.dp)
                .width(388.dp)
        )
        HomeSettingsButton(
            onClick = onAdminClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 28.dp, end = 34.dp)
        )
    }
}

@Composable
private fun HomeSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(66.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                AppTheme.colors.SurfaceElevated
            )
            .border(BorderStroke(1.1.dp, AppTheme.colors.BorderSubtle), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Admin panel",
            tint = AppTheme.colors.Accent,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun StartScreenPanel(
    title: String,
    subtitle: String,
    storageHint: String,
    onFolderSelected: () -> Unit,
    onOpenSdCard: () -> Unit,
    updateStatus: AppUpdateStatus,
    isLoadingMedia: Boolean,
    strings: LocalizedStrings,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeThemeCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 34.dp, vertical = 34.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StartScreenMark()
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = title,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                color = AppTheme.colors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = AppTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            AppPrimaryButton(
                onClick = onFolderSelected,
                enabled = !isLoadingMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(strings.selectFolder, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            AppOutlinedButton(
                onClick = onOpenSdCard,
                enabled = !isLoadingMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                Icon(Icons.Default.SdStorage, contentDescription = null, modifier = Modifier.size(27.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(strings.openSdCard, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                StorageHintGlyph(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = storageHint,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = AppTheme.colors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(26.dp))
            HomeThemeDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(26.dp))
            if (isLoadingMedia) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.Accent,
                    trackColor = AppTheme.colors.SurfaceMuted
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
            }
            UpdateCheckButton(
                updateStatus = updateStatus,
                strings = strings,
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HomeThemeCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(34.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(34.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.44f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .clip(shape)
            .background(
                AppTheme.colors.SurfaceElevated
            )
            .border(BorderStroke(1.15.dp, AppTheme.colors.BorderSubtle), shape)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun StartScreenMark() {
    val accent = AppTheme.colors.Accent
    val accentSoft = AppTheme.colors.AccentSoft
    val surface = AppTheme.colors.Surface
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(146.dp)) {
            drawArc(
                color = accent.copy(alpha = 0.58f),
                startAngle = 210f,
                sweepAngle = 285f,
                useCenter = false,
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = accent.copy(alpha = 0.20f),
                startAngle = 290f,
                sweepAngle = 170f,
                useCenter = false,
                style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(108.dp)
                .border(
                    BorderStroke(
                        1.25.dp,
                        Brush.linearGradient(
                            listOf(
                                AppTheme.colors.Accent.copy(alpha = 0.95f),
                                accentSoft.copy(alpha = 0.25f),
                                AppTheme.colors.Accent.copy(alpha = 0.85f)
                            )
                        )
                    ),
                    CircleShape
                )
                .background(
                    Brush.radialGradient(
                        listOf(
                            AppTheme.colors.Accent.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        radius = 260f
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(94.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            surface,
                            surface
                        ),
                        radius = 180f
                    )
                )
        )
        PhotoFolderGlyph(modifier = Modifier.size(64.dp))
        GlowDot(modifier = Modifier.offset(x = (-62).dp, y = 5.dp))
        GlowDot(modifier = Modifier.offset(x = 62.dp, y = 0.dp))
    }
}

@Composable
private fun PhotoFolderGlyph(modifier: Modifier = Modifier) {
    val accent = AppTheme.colors.Accent
    val accentSoft = AppTheme.colors.AccentSoft
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val folder = Path().apply {
            moveTo(w * 0.08f, h * 0.30f)
            quadraticTo(w * 0.08f, h * 0.18f, w * 0.20f, h * 0.18f)
            lineTo(w * 0.42f, h * 0.18f)
            quadraticTo(w * 0.50f, h * 0.18f, w * 0.56f, h * 0.28f)
            lineTo(w * 0.86f, h * 0.28f)
            quadraticTo(w * 0.94f, h * 0.28f, w * 0.94f, h * 0.38f)
            lineTo(w * 0.94f, h * 0.82f)
            quadraticTo(w * 0.94f, h * 0.92f, w * 0.84f, h * 0.92f)
            lineTo(w * 0.16f, h * 0.92f)
            quadraticTo(w * 0.08f, h * 0.92f, w * 0.08f, h * 0.82f)
            close()
        }
        drawPath(
            path = folder,
            brush = Brush.verticalGradient(
                listOf(
                    accent.copy(alpha = 0.98f),
                    accentSoft.copy(alpha = 0.92f),
                    accent.copy(alpha = 0.86f)
                )
            )
        )
        drawRoundRect(
            color = Color(0xFF14120F),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.48f),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.30f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        val mountain = Path().apply {
            moveTo(w * 0.30f, h * 0.74f)
            lineTo(w * 0.43f, h * 0.60f)
            lineTo(w * 0.54f, h * 0.72f)
            lineTo(w * 0.64f, h * 0.62f)
            lineTo(w * 0.76f, h * 0.74f)
            close()
        }
        drawPath(path = mountain, color = accent.copy(alpha = 0.98f))
        drawCircle(color = accent.copy(alpha = 0.98f), radius = 3.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.57f))
    }
}

@Composable
private fun GlowDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.Accent)
    )
}

@Composable
private fun HomeThemeDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AppTheme.colors.BorderSubtle.copy(alpha = 0.20f),
                            AppTheme.colors.Accent.copy(alpha = 0.42f),
                            AppTheme.colors.BorderSubtle.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .width(78.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AppTheme.colors.Accent.copy(alpha = 0.70f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun StorageHintGlyph(modifier: Modifier = Modifier) {
    val muted = AppTheme.colors.TextSecondary.copy(alpha = 0.58f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = muted,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.76f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF14120F).copy(alpha = 0.72f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.27f, h * 0.17f),
            size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.05f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        val sd = Path().apply {
            moveTo(w * 0.54f, h * 0.34f)
            lineTo(w * 0.72f, h * 0.22f)
            lineTo(w * 0.86f, h * 0.36f)
            lineTo(w * 0.86f, h * 0.86f)
            lineTo(w * 0.54f, h * 0.86f)
            close()
        }
        drawPath(sd, muted)
        repeat(3) { index ->
            drawRect(
                color = Color(0xFF14120F).copy(alpha = 0.72f),
                topLeft = androidx.compose.ui.geometry.Offset(w * (0.60f + index * 0.07f), h * 0.34f),
                size = androidx.compose.ui.geometry.Size(w * 0.035f, h * 0.12f)
            )
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLanguage: AppLanguage,
    strings: LocalizedStrings,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val optionTurkish = if (selectedLanguage == AppLanguage.Turkish) "✓ ${strings.languageOptionTurkish}" else strings.languageOptionTurkish
    val optionEnglish = if (selectedLanguage == AppLanguage.English) "✓ ${strings.languageOptionEnglish}" else strings.languageOptionEnglish

    AppSegmentedControl(
        options = listOf(optionTurkish, optionEnglish),
        selectedIndex = if (selectedLanguage == AppLanguage.Turkish) 0 else 1,
        onSelected = { index ->
            onLanguageSelected(if (index == 0) AppLanguage.Turkish else AppLanguage.English)
        },
        modifier = modifier
    )
}

@Composable
fun UpdateCheckButton(
    updateStatus: AppUpdateStatus,
    strings: LocalizedStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = updateStatus == AppUpdateStatus.Checking || updateStatus == AppUpdateStatus.Downloading
    val label = updateStatus.label(strings)
    val accent = AppTheme.colors.Accent

    val shape = RoundedCornerShape(AppTheme.shapes.Button)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .clickable(enabled = !isBusy, onClick = onClick)
                .background(
                    AppTheme.colors.Surface
                )
            .border(BorderStroke(1.dp, AppTheme.colors.BorderSubtle), shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.13f))
                        .border(BorderStroke(1.dp, accent.copy(alpha = 0.6f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = label,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    color = AppTheme.colors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
