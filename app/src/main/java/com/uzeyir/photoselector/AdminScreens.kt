package com.uzeyir.photoselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AdminLoginScreen(
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onLogin: (String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    PremiumScreenBackground(contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        PremiumCard(
            modifier = Modifier.widthIn(min = 320.dp, max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PremiumIconBadge(Icons.Default.Lock, contentDescription = null)
                Text("Admin panel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                PremiumTextField(
                    value = password,
                    onValueChange = {
                        password = it.filter(Char::isDigit)
                        error = null
                    },
                    label = "Şifre",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                PremiumPrimaryButton(
                    onClick = {
                        error = when {
                            password.length < AdminSettings.MIN_ADMIN_PASSWORD_LENGTH -> "Şifre en az 4 haneli olmalı."
                            onLogin(password) -> null
                            else -> "Şifre hatalı."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Giriş")
                }
                PremiumOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.returnHome)
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
    PremiumScreenBackground {
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
            Text("Admin panel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            PremiumOutlinedButton(onClick = onBackHome) {
                Text(strings.returnHome)
            }
        }
        Row(modifier = Modifier.fillMaxSize()) {
            PremiumNavRail {
                AdminRailItem(
                    selected = selectedSection == AdminPanelSection.Password,
                    label = "Şifre",
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    onClick = { onSectionSelected(AdminPanelSection.Password) }
                )
                AdminRailItem(
                    selected = selectedSection == AdminPanelSection.Pricing,
                    label = "Fiyatlar",
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    onClick = { onSectionSelected(AdminPanelSection.Pricing) }
                )
                AdminRailItem(
                    selected = selectedSection == AdminPanelSection.Themes,
                    label = "Theme-lar",
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    onClick = { onSectionSelected(AdminPanelSection.Themes) }
                )
            }
            PremiumCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 18.dp, bottom = 18.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                when (selectedSection) {
                    AdminPanelSection.Password -> PasswordAdminSection(onChangePassword = onChangePassword)
                    AdminPanelSection.Pricing -> PricingAdminSection(
                        pricing = settings.pricing,
                        onSavePricing = onSavePricing,
                        onResetPricing = onResetPricing
                    )
                    AdminPanelSection.Themes -> ThemesAdminSection(
                        selectedTheme = settings.theme,
                        onThemeSelected = onThemeSelected
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun AdminRailItem(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    PremiumNavRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label
    )
}

@Composable
private fun PasswordAdminSection(
    onChangePassword: (String, String, String) -> Boolean
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AdminSectionColumn(title = "Şifre değiştir") {
        PasswordField("Eski şifre", currentPassword) {
            currentPassword = it.filter(Char::isDigit)
            message = null
        }
        PasswordField("Yeni şifre", newPassword) {
            newPassword = it.filter(Char::isDigit)
            message = null
        }
        PasswordField("Yeni şifre tekrar", repeatedPassword) {
            repeatedPassword = it.filter(Char::isDigit)
            message = null
        }
        PremiumPrimaryButton(
            onClick = {
                val changed = onChangePassword(currentPassword, newPassword, repeatedPassword)
                message = if (changed) {
                    currentPassword = ""
                    newPassword = ""
                    repeatedPassword = ""
                    "Şifre değiştirildi."
                } else {
                    "Eski şifre, yeni şifre veya tekrar alanı hatalı."
                }
            },
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            Text("Değiştir")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun PricingAdminSection(
    pricing: PricingSettings,
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

    AdminSectionColumn(title = "Fiyatlar") {
        NumberField(label = "Foto fiyatı", value = photoPrice, onValueChange = {
            photoPrice = it.filter(Char::isDigit)
            message = null
        })
        NumberField(label = "Video fiyatı", value = videoPrice, onValueChange = {
            videoPrice = it.filter(Char::isDigit)
            message = null
        })
        NumberField(label = "Limit adedi", value = limitCount, onValueChange = {
            limitCount = it.filter(Char::isDigit)
            message = null
        })
        Text("İndirim basamakları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        tiers.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(
                    label = "Adet",
                    value = row.first,
                    onValueChange = { value -> tiers[index] = value.filter(Char::isDigit) to row.second },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "Yüzde",
                    value = row.second,
                    onValueChange = { value -> tiers[index] = row.first to value.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { tiers.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                }
            }
        }
        PremiumOutlinedButton(
            onClick = { tiers.add("" to "") },
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Basamak ekle")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumPrimaryButton(
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
                    message = if (onSavePricing(draft)) "Fiyatlar kaydedildi." else "Fiyat değerlerini kontrol edin."
                },
            ) {
                Text("Kaydet")
            }
            PremiumOutlinedButton(
                onClick = {
                    onResetPricing()
                    message = "Varsayılan fiyatlara dönüldü."
                },
            ) {
                Text("Varsayılana dön")
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ThemesAdminSection(
    selectedTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit
) {
    AdminSectionColumn(title = "Theme-lar") {
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
                    Text(theme.adminLabel(), style = MaterialTheme.typography.titleMedium)
                    Text(theme.adminDescription(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AdminSectionColumn(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    PremiumTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
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
    PremiumTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

private fun AppThemeOption.adminLabel(): String = when (this) {
    AppThemeOption.TaksimLight -> "Mevcut kırmızı/açık"
    AppThemeOption.Dark -> "Premium koyu"
    AppThemeOption.RedWhite -> "Koyu kırmızı-beyaz"
    AppThemeOption.HighContrastDark -> "Yüksek kontrast koyu"
    AppThemeOption.MonoLight -> "Sade siyah-beyaz"
    AppThemeOption.GallerySage -> "Galeri adaçayı"
    AppThemeOption.MidnightTeal -> "Gece petrol"
}

private fun AppThemeOption.adminDescription(): String = when (this) {
    AppThemeOption.TaksimLight -> "Mevcut Taksim paleti."
    AppThemeOption.Dark -> "Matte charcoal yüzeyler, düşük kontrast ve rafine kırmızı vurgu."
    AppThemeOption.RedWhite -> "Beyaz zemin, koyu kırmızı aksan."
    AppThemeOption.HighContrastDark -> "Siyah zemin, yüksek okunabilirlik."
    AppThemeOption.MonoLight -> "Temiz beyaz yüzeyler, siyah vurgu ve nötr metin."
    AppThemeOption.GallerySage -> "Fotoğrafları öne çıkaran düşük doygunluklu yeşil tonlar."
    AppThemeOption.MidnightTeal -> "Göz yormayan koyu gri-petrol yüzeyler ve yumuşak vurgu."
}
