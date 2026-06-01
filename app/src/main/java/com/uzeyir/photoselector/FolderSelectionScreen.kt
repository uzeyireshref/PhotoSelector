package com.uzeyir.photoselector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun FolderSelectionScreen(
    onFolderSelected: () -> Unit,
    onOpenSdCard: () -> Unit,
    updateStatus: AppUpdateStatus,
    isLoadingMedia: Boolean,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    strings: LocalizedStrings,
    onCheckUpdate: () -> Unit
) {
    val startSubtitle = when (language) {
        AppLanguage.Turkish -> "Fotoğraf ve videolarınızı seçmek için kaynak klasörü açın."
        AppLanguage.English -> "Open the source folder to choose your photos and videos."
    }
    val storageHint = when (language) {
        AppLanguage.Turkish -> "Telefon hafızası veya SD karttan devam edin."
        AppLanguage.English -> "Continue from phone storage or an SD card."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7FAF8),
                        Color(0xFFEAF3EF),
                        Color(0xFFFDF8EF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(76.dp))
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
                modifier = Modifier.widthIn(max = 440.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        LanguageSelector(
            selectedLanguage = language,
            strings = strings,
            onLanguageSelected = onLanguageSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StartScreenMark()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF14211F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF52615D),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onFolderSelected,
                enabled = !isLoadingMedia,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF17423C),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.selectFolder)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenSdCard,
                enabled = !isLoadingMedia,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF17423C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.SdStorage, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.openSdCard)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = storageHint,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6E7A76)
            )
            Spacer(modifier = Modifier.height(22.dp))
            if (isLoadingMedia) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
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
private fun StartScreenMark() {
    Surface(
        modifier = Modifier.size(72.dp),
        color = Color(0xFF17423C),
        shape = CircleShape,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {}
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = Color(0xFFFFD18A),
                modifier = Modifier.size(34.dp)
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
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = selectedLanguage == AppLanguage.Turkish,
            onClick = { onLanguageSelected(AppLanguage.Turkish) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text(strings.languageOptionTurkish)
        }
        SegmentedButton(
            selected = selectedLanguage == AppLanguage.English,
            onClick = { onLanguageSelected(AppLanguage.English) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text(strings.languageOptionEnglish)
        }
    }
}

@Composable
fun UpdateCheckButton(
    updateStatus: AppUpdateStatus,
    strings: LocalizedStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = updateStatus == AppUpdateStatus.Checking || updateStatus == AppUpdateStatus.Downloading
    val isUpToDate = updateStatus == AppUpdateStatus.UpToDate
    val label = updateStatus.label(strings)
    val colors = if (isUpToDate) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32),
            contentColor = Color.White
        )
    } else {
        ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        enabled = !isBusy,
        colors = colors,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.widthIn(min = 240.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        } else {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(label)
    }
}
