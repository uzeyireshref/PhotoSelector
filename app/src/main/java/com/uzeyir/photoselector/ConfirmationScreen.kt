package com.uzeyir.photoselector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.AppTheme
import com.uzeyir.photoselector.ui.theme.TaksimSuccess
import kotlin.math.roundToInt

@Composable
fun ConfirmationScreen(
    summary: ExportSummary,
    exportStatus: ExportStatus,
    shareStatus: ShareStatus = ShareStatus.Idle,
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
    AppScreenBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val compact = maxWidth < 700.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (compact) 24.dp else 64.dp,
                        vertical = if (compact) 38.dp else 78.dp
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    strings.finalConfirmation,
                    style = AppTheme.typography.ScreenTitle,
                    color = AppTheme.colors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    strings.finalConfirmationSubtitle,
                    style = AppTheme.typography.CardTitle,
                    color = AppTheme.colors.TextSecondary
                )
                Spacer(modifier = Modifier.height(34.dp))
                ConfirmationSummaryCard(
                    summary = summary,
                    includeRawFiles = includeRawFiles,
                    totalTransferFiles = totalTransferFiles,
                    totalOriginalPrice = photoOriginalPrice + videoOriginalPrice,
                    totalDiscount = totalDiscount,
                    totalPayablePrice = totalPayablePrice,
                    strings = strings,
                    compact = compact,
                    modifier = Modifier.widthIn(min = 320.dp, max = 820.dp)
                )
                Spacer(modifier = Modifier.height(28.dp))

                when (exportStatus) {
                    ExportStatus.Idle -> {
                        val isSharePreparing = shareStatus is ShareStatus.Preparing
                        Column(
                            modifier = Modifier.widthIn(min = 320.dp, max = 820.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            RawIncludeActionRow(
                                checked = includeRawFiles,
                                enabled = summary.matchedRawCount > 0 && !isSharePreparing,
                                text = strings.includeRawFiles,
                                onClick = { onIncludeRawFilesChange(!includeRawFiles) }
                            )
                            WhatsAppDocumentActionRow(
                                onClick = onShareWhatsApp,
                                enabled = totalTransferFiles > 0 && !isSharePreparing,
                                text = strings.whatsAppDocumentShare
                            )
                            if (shareStatus is ShareStatus.Preparing) {
                                ShareProgressPanel(
                                    shareStatus = shareStatus,
                                    strings = strings,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ConfirmationBackButton(
                                    onClick = onBack,
                                    modifier = Modifier.weight(0.82f),
                                    text = strings.back
                                )
                                ConfirmationCopyButton(
                                    onClick = onConfirmExport,
                                    enabled = totalTransferFiles > 0 && !isSharePreparing,
                                    modifier = Modifier.weight(1.62f),
                                    text = if (includeRawFiles) strings.copyJpgAndRaw else strings.copySelectedFiles
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AppTheme.colors.TextSecondary.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    strings.secureProcessingNotice,
                                    style = AppTheme.typography.Body,
                                    color = AppTheme.colors.TextSecondary
                                )
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
                        var successVisible by remember(exportStatus) { mutableStateOf(false) }
                        LaunchedEffect(exportStatus) {
                            successVisible = true
                        }
                        val successScale by animateFloatAsState(
                            targetValue = if (successVisible) 1f else 0.72f,
                            animationSpec = tween(durationMillis = 180),
                            label = "CopySuccessCheckScale"
                        )
                        val successAlpha by animateFloatAsState(
                            targetValue = if (successVisible) 1f else 0f,
                            animationSpec = tween(durationMillis = 140),
                            label = "CopySuccessCheckAlpha"
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TaksimSuccess,
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer {
                                    scaleX = successScale
                                    scaleY = successScale
                                    alpha = successAlpha
                                }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(strings.exportComplete, color = TaksimSuccess, style = AppTheme.typography.SectionTitle)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            strings.folderName(exportStatus.folderName),
                            color = AppTheme.colors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppTheme.typography.Body
                        )
                        Text(
                            strings.copiedFileCount(exportStatus.copiedFiles),
                            color = AppTheme.colors.TextSecondary,
                            style = AppTheme.typography.Body
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppOutlinedButton(onClick = onReturnHome) {
                                Text(strings.returnHome, style = AppTheme.typography.ButtonText)
                            }
                            AppPrimaryButton(onClick = onReturnToGallery) {
                                Text(strings.returnToGallery, style = AppTheme.typography.ButtonText)
                            }
                        }
                    }

                    is ExportStatus.Error -> {
                        Text(strings.exportFailed, color = AppTheme.colors.Error, style = AppTheme.typography.SectionTitle)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            strings.message(exportStatus.message, exportStatus.argument),
                            color = AppTheme.colors.Error,
                            style = AppTheme.typography.Body
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppOutlinedButton(onClick = onBack) {
                                Text(strings.back, style = AppTheme.typography.ButtonText)
                            }
                            AppPrimaryButton(onClick = onConfirmExport) {
                                Text(strings.retry, style = AppTheme.typography.ButtonText)
                            }
                        }
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
    totalOriginalPrice: Int,
    totalDiscount: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        containerColor = AppTheme.colors.Surface.copy(alpha = 0.92f),
        contentPadding = PaddingValues(0.dp),
        radius = 34.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 38.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SummaryMetric(Icons.Default.Image, strings.photoShort, summary.selectedJpgCount.toString())
                    SummaryMetric(Icons.Default.GpsFixed, strings.matchingRaw, summary.matchedRawCount.toString())
                    SummaryMetric(Icons.Default.Videocam, strings.videoShort, summary.selectedVideoCount.toString())
                    SummaryMetric(null, strings.rawIncluded, strings.yesNo(includeRawFiles), rawBadge = true)
                    SummaryMetric(Icons.Default.Description, strings.filesToCopy, "$totalTransferFiles ${strings.fileUnit}")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        SummaryMetric(Icons.Default.Image, strings.photoShort, summary.selectedJpgCount.toString())
                        ThinDivider()
                        SummaryMetric(Icons.Default.Videocam, strings.videoShort, summary.selectedVideoCount.toString())
                        ThinDivider()
                        SummaryMetric(null, strings.rawIncluded, strings.yesNo(includeRawFiles), rawBadge = true)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(250.dp)
                            .background(AppTheme.colors.BorderSubtle.copy(alpha = 0.70f))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        SummaryMetric(Icons.Default.GpsFixed, strings.matchingRaw, summary.matchedRawCount.toString())
                        ThinDivider()
                        SummaryMetric(Icons.Default.Description, strings.filesToCopy, "$totalTransferFiles ${strings.fileUnit}")
                    }
                }
            }
            ThinDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconFrame {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = AppTheme.colors.Accent,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.total,
                        style = AppTheme.typography.CardTitle,
                        color = AppTheme.colors.TextSecondary
                    )
                    if (totalDiscount > 0) {
                        Text(
                            text = strings.price(totalOriginalPrice),
                            style = AppTheme.typography.CardTitle,
                            color = AppTheme.colors.TextSecondary.copy(alpha = 0.72f),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = strings.price(totalPayablePrice),
                        style = AppTheme.typography.ScreenTitle,
                        color = if (totalDiscount > 0) TaksimSuccess else AppTheme.colors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (totalDiscount > 0) {
                Text(
                    text = "${strings.discount}: ${strings.price(totalDiscount)}",
                    style = AppTheme.typography.CardTitle,
                    color = TaksimSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    icon: ImageVector?,
    label: String,
    value: String,
    rawBadge: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconFrame {
            if (rawBadge) {
                Text(
                    "RAW",
                    style = AppTheme.typography.HelperText,
                    color = AppTheme.colors.Accent,
                    fontWeight = FontWeight.Bold
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppTheme.colors.Accent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = AppTheme.typography.CardTitle, color = AppTheme.colors.TextSecondary)
            Text(value, style = AppTheme.typography.SectionTitle, color = AppTheme.colors.TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IconFrame(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(shape)
            .background(AppTheme.colors.SurfaceElevated.copy(alpha = 0.80f))
            .border(BorderStroke(1.dp, AppTheme.colors.BorderSubtle.copy(alpha = 0.90f)), shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppTheme.colors.BorderSubtle.copy(alpha = 0.60f))
    )
}

@Composable
private fun RawIncludeActionRow(
    checked: Boolean,
    enabled: Boolean,
    text: String,
    onClick: () -> Unit
) {
    ActionSurface(enabled = enabled, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (checked) AppTheme.colors.Accent else AppTheme.colors.SurfaceElevated)
                    .border(BorderStroke(1.dp, AppTheme.colors.Accent.copy(alpha = 0.82f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
            Text(text, style = AppTheme.typography.CardTitle, color = AppTheme.colors.TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WhatsAppDocumentActionRow(
    onClick: () -> Unit,
    enabled: Boolean,
    text: String
) {
    ActionSurface(enabled = enabled, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_whatsapp_logo),
                contentDescription = text,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text,
                style = AppTheme.typography.CardTitle,
                color = AppTheme.colors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppTheme.colors.TextSecondary,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun ConfirmationBackButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppTheme.shapes.Button)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 74.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .border(BorderStroke(1.4.dp, AppTheme.colors.Accent), shape),
        color = Color.Transparent,
        contentColor = AppTheme.colors.Accent,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = AppTheme.typography.SectionTitle, color = AppTheme.colors.Accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ConfirmationCopyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppTheme.shapes.Button)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 74.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .border(BorderStroke(1.2.dp, AppTheme.colors.Accent.copy(alpha = if (enabled) 0.95f else 0.25f)), shape),
        color = Color.Transparent,
        contentColor = Color.White,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .background(AppTheme.colors.Accent.copy(alpha = if (enabled) 1f else 0.34f))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text, style = AppTheme.typography.SectionTitle, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionSurface(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 98.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .border(BorderStroke(1.dp, AppTheme.colors.BorderSubtle.copy(alpha = if (enabled) 0.86f else 0.34f)), shape),
        color = Color.Transparent,
        contentColor = AppTheme.colors.TextPrimary,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(AppTheme.colors.Surface.copy(alpha = if (enabled) 0.92f else 0.50f))
                .padding(horizontal = 30.dp, vertical = 22.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle? = null,
    valueColor: Color? = null
) {
    val defaultStyle = AppTheme.typography.CardTitle
    val defaultColor = AppTheme.colors.TextPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTheme.typography.Body,
            color = AppTheme.colors.TextSecondary
        )
        Text(
            text = value,
            style = valueStyle ?: defaultStyle,
            color = valueColor ?: defaultColor,
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
    AppCard(
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
                Text(strings.copyProgress, style = AppTheme.typography.CardTitle, fontWeight = FontWeight.SemiBold)
                Text(
                    text = exportStatus.progressFraction?.let { formatPercent(it) }
                        ?: exportProgressCountLabel(exportStatus).orEmpty(),
                    style = AppTheme.typography.CardTitle,
                    color = AppTheme.colors.Accent
                )
            }
            exportStatus.progressFraction?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.Accent,
                    trackColor = AppTheme.colors.SurfaceMuted
                )
            } ?: LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AppTheme.colors.Accent,
                trackColor = AppTheme.colors.SurfaceMuted
            )
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
    AppCard(
        containerColor = AppTheme.colors.SurfaceElevated,
        modifier = Modifier.fillMaxWidth(),
        radius = AppTheme.shapes.Button,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileName,
                    style = AppTheme.typography.Caption,
                    color = AppTheme.colors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = progress?.let { formatPercent(it) } ?: "-",
                    style = AppTheme.typography.HelperText,
                    color = AppTheme.colors.Accent
                )
            }
            progress?.let { value ->
                LinearProgressIndicator(
                    progress = { value },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.Accent,
                    trackColor = AppTheme.colors.SurfaceMuted
                )
            } ?: LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AppTheme.colors.Accent,
                trackColor = AppTheme.colors.SurfaceMuted
            )
        }
    }
}

@Composable
private fun ShareProgressPanel(
    shareStatus: ShareStatus.Preparing,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    AppCard(
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
                Text(strings.shareProgress, style = AppTheme.typography.CardTitle, fontWeight = FontWeight.SemiBold)
                Text(
                    text = shareStatus.progressFraction?.let { formatPercent(it) }
                        ?: shareProgressCountLabel(shareStatus).orEmpty(),
                    style = AppTheme.typography.CardTitle,
                    color = AppTheme.colors.Accent
                )
            }
            shareStatus.progressFraction?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.Accent,
                    trackColor = AppTheme.colors.SurfaceMuted
                )
            } ?: LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AppTheme.colors.Accent,
                trackColor = AppTheme.colors.SurfaceMuted
            )
            shareStatus.currentFileName?.let { fileName ->
                CurrentFileProgress(
                    fileName = fileName,
                    progress = shareStatus.currentFileProgressFraction
                )
            }
        }
    }
}

private fun formatPercent(progress: Float): String =
    "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%"
