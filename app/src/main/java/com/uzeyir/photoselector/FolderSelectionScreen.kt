package com.uzeyir.photoselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    PremiumScreenBackground {
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
        PremiumIconButton(
            imageVector = Icons.Default.Settings,
            contentDescription = "Admin panel",
            onClick = onAdminClick,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 2.dp, end = 8.dp)
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
    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StartScreenMark()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            PremiumPrimaryButton(
                onClick = onFolderSelected,
                enabled = !isLoadingMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.selectFolder)
            }
            Spacer(modifier = Modifier.height(12.dp))
            PremiumOutlinedButton(
                onClick = onOpenSdCard,
                enabled = !isLoadingMedia,
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
                color = MaterialTheme.colorScheme.secondary
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
    PremiumIconBadge(
        imageVector = Icons.Default.PhotoLibrary,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.secondary
    )
}

@Composable
fun LanguageSelector(
    selectedLanguage: AppLanguage,
    strings: LocalizedStrings,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumSegmentedControl(
        options = listOf(strings.languageOptionTurkish, strings.languageOptionEnglish),
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

    PremiumPrimaryButton(
        onClick = onClick,
        enabled = !isBusy,
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

