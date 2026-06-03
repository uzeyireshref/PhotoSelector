package com.uzeyir.photoselector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.AppTheme
import com.uzeyir.photoselector.ui.theme.LocalAppColors
import com.uzeyir.photoselector.ui.theme.LocalAppTypography
import com.uzeyir.photoselector.ui.theme.LocalAppShapes
import com.uzeyir.photoselector.ui.theme.LocalAppSpacing

private fun Color.lift(alpha: Float = 0.38f): Color =
    Color(
        red = red + (1f - red) * alpha,
        green = green + (1f - green) * alpha,
        blue = blue + (1f - blue) * alpha,
        alpha = this.alpha
    )

private fun Color.sink(alpha: Float = 0.34f): Color =
    Color(red = red * (1f - alpha), green = green * (1f - alpha), blue = blue * (1f - alpha), alpha = this.alpha)

private fun premiumAccentBrush(accent: Color): Brush =
    Brush.verticalGradient(
        listOf(
            accent,
            accent.sink(0.08f),
            accent.sink(0.20f)
        )
    )

internal fun premiumGlassBrush(surface: Color): Brush =
    Brush.linearGradient(
        listOf(
            surface,
            surface
        )
    )

@Composable
fun AppScreenBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppTheme.colors.BackgroundSoft,
                        AppTheme.colors.Background,
                        AppTheme.colors.BackgroundSoft
                    )
                )
            ),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.Surface,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    radius: Dp = AppTheme.shapes.ScreenCard,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .shadow(16.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.24f))
            .clip(shape)
            .background(premiumGlassBrush(containerColor))
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = AppTheme.colors.BorderSubtle
                ),
                shape
            )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            CompositionLocalProvider(LocalContentColor provides AppTheme.colors.TextPrimary) {
                content()
            }
        }
    }
}

@Composable
fun AppPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.shapes.Button)
    val accent = AppTheme.colors.Accent
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 54.dp)
            .shadow(
                if (enabled) 14.dp else 0.dp,
                shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.34f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .border(BorderStroke(1.2.dp, accent.copy(alpha = if (enabled) 0.95f else 0.25f)), shape),
        color = Color.Transparent,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else AppTheme.colors.TextPrimary.copy(alpha = 0.4f),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .background(if (enabled) premiumAccentBrush(accent) else premiumGlassBrush(AppTheme.colors.SurfaceElevated))
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun AppSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    AppOutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.shapes.Button)
    val accent = AppTheme.colors.Accent
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .shadow(
                if (enabled) 5.dp else 0.dp,
                shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .border(BorderStroke(1.15.dp, accent.copy(alpha = if (enabled) 0.75f else 0.22f)), shape),
        color = Color.Transparent,
        contentColor = if (enabled) AppTheme.colors.TextPrimary else AppTheme.colors.TextPrimary.copy(alpha = 0.4f),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .background(premiumGlassBrush(AppTheme.colors.Surface))
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun AppActionRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.End),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    isError: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = AppTheme.typography.HelperText) },
        leadingIcon = leadingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        shape = RoundedCornerShape(AppTheme.shapes.Input),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.Accent,
            unfocusedBorderColor = AppTheme.colors.Accent.copy(alpha = 0.50f),
            focusedContainerColor = AppTheme.colors.SurfaceElevated,
            unfocusedContainerColor = AppTheme.colors.Surface,
            focusedLabelColor = AppTheme.colors.Accent,
            unfocusedLabelColor = AppTheme.colors.TextSecondary,
            focusedLeadingIconColor = AppTheme.colors.Accent,
            unfocusedLeadingIconColor = AppTheme.colors.TextSecondary
        ),
        modifier = modifier
    )
}

@Composable
fun AppIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null
) {
    val accent = AppTheme.colors.Accent
    val contentColor = if (selected) AppTheme.colors.Background else (tint ?: AppTheme.colors.TextPrimary)
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) premiumAccentBrush(accent) else premiumGlassBrush(AppTheme.colors.SurfaceMuted))
            .border(BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.85f else 0.45f)), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor
        )
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}

@Composable
fun AppIconBadge(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.Accent
) {
    Surface(
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
            .shadow(12.dp, CircleShape, clip = false, ambientColor = tint.copy(alpha = 0.20f), spotColor = Color.Black),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, tint.copy(alpha = 0.70f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(premiumGlassBrush(AppTheme.colors.SurfaceElevated))
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList()
) {
    val accent = AppTheme.colors.Accent
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(AppTheme.shapes.Chip),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .background(premiumGlassBrush(AppTheme.colors.SurfaceElevated))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, text ->
                val selected = selectedIndex == index
                val icon = icons.getOrNull(index)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(AppTheme.shapes.Chip))
                        .clickable { onSelected(index) }
                        .background(if (selected) premiumGlassBrush(AppTheme.colors.SurfaceMuted) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .border(
                            BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.85f) else Color.Transparent),
                            RoundedCornerShape(AppTheme.shapes.Chip)
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AppTheme.colors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text,
                            style = AppTheme.typography.ButtonText,
                            color = AppTheme.colors.TextPrimary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.xxl, vertical = AppTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            navigationIcon?.invoke()
            Column {
                Text(
                    text = title,
                    style = AppTheme.typography.ScreenTitle,
                    color = AppTheme.colors.TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTheme.typography.Caption,
                        color = AppTheme.colors.TextSecondary
                    )
                }
            }
        }
        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                content = actions
            )
        }
    }
}

@Composable
fun AppNavRailItem(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.shapes.Button)
    val accent = AppTheme.colors.Accent
    val contentColor = AppTheme.colors.TextPrimary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .background(if (selected) premiumGlassBrush(AppTheme.colors.SurfaceMuted) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .border(
                BorderStroke(
                    width = 1.2.dp,
                    brush = if (selected) {
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.4f), accent))
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                ),
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
        Text(
            label,
            style = AppTheme.typography.HelperText,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun AppGalleryItem(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.shapes.Thumbnail)
    Box(
        modifier = modifier
            .shadow(if (selected) 8.dp else 3.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(shape)
            .background(AppTheme.colors.Surface)
            .border(
                BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) AppTheme.colors.Accent else AppTheme.colors.BorderSubtle.copy(alpha = 0.32f)
                ),
                shape
            )
    ) {
        content()
    }
}

@Composable
fun AppBottomSummaryBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.shapes.ScreenCard)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .shadow(10.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.14f)),
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(1.dp, AppTheme.colors.BorderSubtle.copy(alpha = 0.72f))
    ) {
        Box(modifier = Modifier.background(premiumGlassBrush(AppTheme.colors.Surface))) {
            content()
        }
    }
}

@Composable
fun AppDialog(
    title: String,
    text: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit = {}
) {
    val currentColorScheme = MaterialTheme.colorScheme
    val currentAppColors = AppTheme.colors
    val currentTypography = MaterialTheme.typography
    val currentAppTypography = AppTheme.typography
    val currentAppShapes = AppTheme.shapes
    val currentAppSpacing = AppTheme.spacing
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(
            LocalAppColors provides currentAppColors,
            LocalAppTypography provides currentAppTypography,
            LocalAppShapes provides currentAppShapes,
            LocalAppSpacing provides currentAppSpacing
        ) {
            MaterialTheme(colorScheme = currentColorScheme, typography = currentTypography) {
                AppCard(
                    modifier = Modifier.widthIn(min = 320.dp, max = 640.dp),
                    containerColor = AppTheme.colors.SurfaceElevated,
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 24.dp),
                    radius = AppTheme.shapes.Dialog
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xl)) {
                        Text(
                            title,
                            style = AppTheme.typography.SectionTitle,
                            color = AppTheme.colors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box {
                            text()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            dismissButton()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    AppCard(
        modifier = modifier.widthIn(min = 260.dp, max = 420.dp),
        containerColor = AppTheme.colors.Surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            icon?.let {
                AppIconBadge(imageVector = it, contentDescription = null)
            }
            Text(
                text = title,
                style = AppTheme.typography.CardTitle,
                color = AppTheme.colors.TextPrimary.copy(alpha = 0.80f)
            )
        }
    }
}

@Composable
fun AppDivider(
    modifier: Modifier = Modifier
) {
    val primaryColor = AppTheme.colors.Accent
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.15f),
                        primaryColor.copy(alpha = 0.42f),
                        primaryColor.copy(alpha = 0.68f),
                        primaryColor.copy(alpha = 0.42f),
                        primaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
    )
}
