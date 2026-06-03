package com.uzeyir.photoselector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.AppTheme

@Composable
fun AdminLoginScreen(
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onLogin: (String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AppScreenBackground(contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppCard(
                modifier = Modifier.widthIn(min = 320.dp, max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIconBadge(Icons.Default.Lock, contentDescription = null)
                    Text(strings.adminPanel, style = AppTheme.typography.SectionTitle, fontWeight = FontWeight.SemiBold)
                    AppTextField(
                        value = password,
                        onValueChange = {
                            password = it.filter(Char::isDigit)
                            error = null
                        },
                        label = strings.adminPassword,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = error != null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AppTheme.colors.Accent
                            )
                        },
                        supportingText = { error?.let { Text(it, color = AppTheme.colors.Error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppPrimaryButton(
                        onClick = {
                            error = when {
                                password.length < AdminSettings.MIN_ADMIN_PASSWORD_LENGTH -> strings.adminPasswordMinError
                                onLogin(password) -> null
                                else -> strings.adminPasswordWrong
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.adminLogin, style = AppTheme.typography.ButtonText)
                    }
                    AppOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.returnHome, style = AppTheme.typography.ButtonText)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    settings: AdminSettings,
    selectedSection: AdminPanelSection,
    strings: LocalizedStrings,
    onBackHome: () -> Unit,
    onSectionSelected: (AdminPanelSection) -> Unit,
    onChangePassword: (String, String, String) -> Boolean,
    onSavePricing: (PricingSettings) -> Boolean,
    onResetPricing: () -> Unit,
    onThemeSelected: (AppThemeOption) -> Unit
) {
    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.adminPanel,
                    style = AppTheme.typography.ScreenTitle.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AppTheme.colors.Accent,
                                AppTheme.colors.AccentSoft
                            )
                        )
                    ),
                    fontWeight = FontWeight.Bold
                )
                AppOutlinedButton(onClick = onBackHome) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = AppTheme.colors.Accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.returnHome, style = AppTheme.typography.ButtonText)
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavRail {
                    AdminRailItem(
                        selected = selectedSection == AdminPanelSection.Password,
                        label = strings.adminPassword,
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        onClick = { onSectionSelected(AdminPanelSection.Password) }
                    )
                    AdminRailItem(
                        selected = selectedSection == AdminPanelSection.Pricing,
                        label = strings.adminPricing,
                        icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        onClick = { onSectionSelected(AdminPanelSection.Pricing) }
                    )
                    AdminRailItem(
                        selected = selectedSection == AdminPanelSection.Themes,
                        label = strings.adminThemes,
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        onClick = { onSectionSelected(AdminPanelSection.Themes) }
                    )
                }
                AppCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 18.dp, bottom = 18.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    when (selectedSection) {
                        AdminPanelSection.Password -> PasswordAdminSection(strings = strings, onChangePassword = onChangePassword)
                        AdminPanelSection.Pricing -> PricingAdminSection(
                            pricing = settings.pricing,
                            strings = strings,
                            onSavePricing = onSavePricing,
                            onResetPricing = onResetPricing
                        )
                        AdminPanelSection.Themes -> ThemesAdminSection(
                            selectedTheme = settings.theme,
                            strings = strings,
                            onThemeSelected = onThemeSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavRail(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.shapes.ScreenCard)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(116.dp)
            .padding(start = 18.dp, bottom = 18.dp)
            .clip(shape)
            .background(premiumGlassBrush(AppTheme.colors.Surface))
            .border(BorderStroke(1.2.dp, AppTheme.colors.BorderAccent.copy(alpha = 0.52f)), shape)
            .padding(horizontal = 10.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

@Composable
private fun AdminRailItem(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    AppNavRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label
    )
}

@Composable
private fun PasswordAdminSection(
    strings: LocalizedStrings,
    onChangePassword: (String, String, String) -> Boolean
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AdminSectionColumn(title = strings.adminPasswordChange, icon = Icons.Default.Lock) {
        PasswordField(strings.adminCurrentPassword, currentPassword) {
            currentPassword = it.filter(Char::isDigit)
            message = null
        }
        PasswordField(strings.adminNewPassword, newPassword) {
            newPassword = it.filter(Char::isDigit)
            message = null
        }
        PasswordField(strings.adminRepeatNewPassword, repeatedPassword) {
            repeatedPassword = it.filter(Char::isDigit)
            message = null
        }
        AppPrimaryButton(
            onClick = {
                val changed = onChangePassword(currentPassword, newPassword, repeatedPassword)
                message = if (changed) {
                    currentPassword = ""
                    newPassword = ""
                    repeatedPassword = ""
                    strings.adminPasswordChanged
                } else {
                    strings.adminPasswordChangeFailed
                }
            },
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            Text(strings.adminChange, style = AppTheme.typography.ButtonText)
        }
        message?.let { Text(it, color = AppTheme.colors.TextPrimary, style = AppTheme.typography.Body) }
    }
}

@Composable
private fun PricingAdminSection(
    pricing: PricingSettings,
    strings: LocalizedStrings,
    onSavePricing: (PricingSettings) -> Boolean,
    onResetPricing: () -> Unit
) {
    var photoPrice by remember { mutableStateOf(pricing.photoUnitPrice.toString()) }
    var videoPrice by remember { mutableStateOf(pricing.videoUnitPrice.toString()) }
    var limitCount by remember { mutableStateOf(pricing.priceLimitCount.toString()) }
    val tiers = remember { mutableStateListOf<Pair<String, String>>() }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pricing) {
        photoPrice = pricing.photoUnitPrice.toString()
        videoPrice = pricing.videoUnitPrice.toString()
        limitCount = pricing.priceLimitCount.toString()
        tiers.clear()
        tiers.addAll(pricing.discountTiers.sortedBy { it.itemCount }.map { it.itemCount.toString() to it.discountPercent.toString() })
        message = null
    }

    AdminSectionColumn(title = strings.adminPricing, icon = Icons.Default.Payments) {
        NumberField(label = strings.adminPhotoPrice, value = photoPrice, onValueChange = {
            photoPrice = it.filter(Char::isDigit)
            message = null
        })
        NumberField(label = strings.adminVideoPrice, value = videoPrice, onValueChange = {
            videoPrice = it.filter(Char::isDigit)
            message = null
        })
        NumberField(label = strings.adminLimitCount, value = limitCount, onValueChange = {
            limitCount = it.filter(Char::isDigit)
            message = null
        })
        Text(strings.adminDiscountTiers, style = AppTheme.typography.CardTitle, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.TextPrimary)
        tiers.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(
                    label = strings.adminTierItemCount,
                    value = row.first,
                    onValueChange = { value -> tiers[index] = value.filter(Char::isDigit) to row.second },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = strings.adminTierPercent,
                    value = row.second,
                    onValueChange = { value -> tiers[index] = row.first to value.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { tiers.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = strings.adminDelete, tint = AppTheme.colors.Accent)
                }
            }
        }
        AppOutlinedButton(
            onClick = { tiers.add("" to "") },
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.adminAddTier, style = AppTheme.typography.ButtonText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppPrimaryButton(
                onClick = {
                    val draft = PricingSettings(
                        photoUnitPrice = photoPrice.toIntOrNull() ?: -1,
                        videoUnitPrice = videoPrice.toIntOrNull() ?: -1,
                        priceLimitCount = limitCount.toIntOrNull() ?: 0,
                        discountTiers = tiers.map {
                            PricingDiscountTier(
                                itemCount = it.first.toIntOrNull() ?: 0,
                                discountPercent = it.second.toIntOrNull() ?: -1
                            )
                        }
                    )
                    message = if (onSavePricing(draft)) strings.adminPricingSaved else strings.adminPricingInvalid
                },
            ) {
                Text(strings.adminSave, style = AppTheme.typography.ButtonText)
            }
            AppOutlinedButton(
                onClick = {
                    onResetPricing()
                    message = strings.adminPricingReset
                },
            ) {
                Text(strings.adminResetDefaults, style = AppTheme.typography.ButtonText)
            }
        }
        message?.let { Text(it, color = AppTheme.colors.TextPrimary, style = AppTheme.typography.Body) }
    }
}

@Composable
private fun ThemesAdminSection(
    selectedTheme: AppThemeOption,
    strings: LocalizedStrings,
    onThemeSelected: (AppThemeOption) -> Unit
) {
    AdminSectionColumn(title = strings.adminThemes, icon = Icons.Default.Palette) {
        AppThemeOption.entries.forEach { theme ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
                Column {
                    Text(theme.adminLabel(strings), style = AppTheme.typography.CardTitle, color = AppTheme.colors.TextPrimary)
                    Text(theme.adminDescription(strings), style = AppTheme.typography.Caption, color = AppTheme.colors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AdminSectionColumn(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            icon?.let {
                AppIconBadge(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = AppTheme.colors.Accent
                )
            }
            Text(
                text = title,
                style = AppTheme.typography.SectionTitle,
                color = AppTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        AppDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = AppTheme.colors.Accent
            )
        },
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

private fun AppThemeOption.adminLabel(strings: LocalizedStrings): String = when (this) {
    AppThemeOption.SignatureGold -> strings.themeSignatureGold
    AppThemeOption.RedBlackWhite -> strings.themeRedBlackWhite
    AppThemeOption.DeepTeal -> strings.themeDeepTeal
}

private fun AppThemeOption.adminDescription(strings: LocalizedStrings): String = when (this) {
    AppThemeOption.SignatureGold -> strings.themeSignatureGoldDescription
    AppThemeOption.RedBlackWhite -> strings.themeRedBlackWhiteDescription
    AppThemeOption.DeepTeal -> strings.themeDeepTealDescription
}
