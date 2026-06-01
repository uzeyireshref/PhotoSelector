package com.uzeyir.photoselector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ConfirmationScreen(
    summary: ExportSummary,
    exportStatus: ExportStatus,
    includeRawFiles: Boolean,
    photoOriginalPrice: Int = summary.selectedJpgCount * 300,
    photoPayablePrice: Int = discountedPayablePrice(summary.selectedJpgCount, unitPrice = 300),
    videoOriginalPrice: Int = summary.selectedVideoCount * 1000,
    videoPayablePrice: Int = discountedPayablePrice(summary.selectedVideoCount, unitPrice = 1000),
    totalDiscount: Int = (photoOriginalPrice + videoOriginalPrice) - (photoPayablePrice + videoPayablePrice),
    totalPayablePrice: Int = photoPayablePrice + videoPayablePrice,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onIncludeRawFilesChange: (Boolean) -> Unit,
    onShareWhatsApp: () -> Unit,
    onConfirmExport: () -> Unit,
    onReturnHome: () -> Unit,
    onReturnToGallery: () -> Unit
) {
    val rawFilesToTransfer = if (includeRawFiles) summary.matchedRawCount else 0
    val totalTransferFiles = summary.selectedJpgCount + summary.selectedVideoCount + rawFilesToTransfer
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(strings.finalConfirmation, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        ConfirmationSummaryCard(
            summary = summary,
            includeRawFiles = includeRawFiles,
            totalTransferFiles = totalTransferFiles,
            totalDiscount = totalDiscount,
            totalPayablePrice = totalPayablePrice,
            strings = strings,
            modifier = Modifier.widthIn(min = 320.dp, max = 560.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))

        when (exportStatus) {
            ExportStatus.Idle -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.widthIn(min = 320.dp, max = 560.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeRawFiles,
                            onCheckedChange = onIncludeRawFilesChange,
                            enabled = summary.matchedRawCount > 0
                        )
                        Text(
                            text = strings.includeRawFiles,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable(
                                enabled = summary.matchedRawCount > 0,
                                onClick = { onIncludeRawFilesChange(!includeRawFiles) }
                            )
                        )
                    }
                    Button(
                        onClick = onShareWhatsApp,
                        enabled = totalTransferFiles > 0,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.widthIn(min = 320.dp, max = 420.dp)
                    ) {
                        Text(strings.whatsAppDocumentShare)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onBack) {
                            Text(strings.back)
                        }
                        Button(
                            onClick = onConfirmExport,
                            enabled = totalTransferFiles > 0
                        ) {
                            Text(if (includeRawFiles) strings.copyJpgAndRaw else strings.copySelectedFiles)
                        }
                    }
                }
            }

            is ExportStatus.Copying -> {
                CopyProgressPanel(
                    exportStatus = exportStatus,
                    strings = strings,
                    modifier = Modifier.widthIn(min = 320.dp, max = 620.dp)
                )
            }

            is ExportStatus.Success -> {
                Text(strings.exportComplete, color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.folderName(exportStatus.folderName), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(strings.copiedFileCount(exportStatus.copiedFiles))
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onReturnHome) {
                        Text(strings.returnHome)
                    }
                    Button(onClick = onReturnToGallery) {
                        Text(strings.returnToGallery)
                    }
                }
            }

            is ExportStatus.Error -> {
                Text(strings.exportFailed, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.message(exportStatus.message, exportStatus.argument), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) {
                        Text(strings.back)
                    }
                    Button(onClick = onConfirmExport) {
                        Text(strings.retry)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationSummaryCard(
    summary: ExportSummary,
    includeRawFiles: Boolean,
    totalTransferFiles: Int,
    totalDiscount: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryRow("${strings.photoShort}: ${summary.selectedJpgCount}", "${strings.matchingRaw}: ${summary.matchedRawCount}")
            SummaryRow(strings.includeRawFiles, strings.yesNo(includeRawFiles))
            SummaryRow("${strings.videoShort}: ${summary.selectedVideoCount}", strings.filesToCopyCount(totalTransferFiles))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            if (totalDiscount > 0) {
                Text(
                    text = "${strings.discount}: ${strings.price(totalDiscount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${strings.total}: ${strings.price(totalPayablePrice)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CopyProgressPanel(
    exportStatus: ExportStatus.Copying,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.copyProgress, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = exportStatus.progressFraction?.let { formatPercent(it) }
                        ?: exportProgressCountLabel(exportStatus).orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            exportStatus.progressFraction?.let { progress ->
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            exportStatus.currentFileName?.let { fileName ->
                CurrentFileProgress(
                    fileName = fileName,
                    progress = exportStatus.currentFileProgressFraction
                )
            }
        }
    }
}

@Composable
private fun CurrentFileProgress(
    fileName: String,
    progress: Float?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = progress?.let { formatPercent(it) } ?: "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            progress?.let { value ->
                LinearProgressIndicator(
                    progress = { value },
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun formatPercent(progress: Float): String =
    "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%"
