package com.uzeyir.photoselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.AppTheme
import com.uzeyir.photoselector.ui.theme.TaksimSuccess

@Composable
fun BottomPriceBar(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    onReviewClick: () -> Unit,
    buttonText: String,
    strings: LocalizedStrings
) {
    AppBottomSummaryBar {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            val isTablet = maxWidth >= 600.dp
            if (isTablet) {
                TabletBottomPriceBarContent(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    photoOriginalPrice = photoOriginalPrice,
                    photoPayablePrice = photoPayablePrice,
                    videoOriginalPrice = videoOriginalPrice,
                    videoPayablePrice = videoPayablePrice,
                    totalPayablePrice = totalPayablePrice,
                    onReviewClick = onReviewClick,
                    buttonText = buttonText,
                    strings = strings
                )
            } else {
                PhoneBottomPriceBarContent(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    photoOriginalPrice = photoOriginalPrice,
                    photoPayablePrice = photoPayablePrice,
                    videoOriginalPrice = videoOriginalPrice,
                    videoPayablePrice = videoPayablePrice,
                    totalPayablePrice = totalPayablePrice,
                    onReviewClick = onReviewClick,
                    buttonText = buttonText,
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun PhoneBottomPriceBarContent(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    onReviewClick: () -> Unit,
    buttonText: String,
    strings: LocalizedStrings
) {
    val discount = (photoOriginalPrice + videoOriginalPrice) - (photoPayablePrice + videoPayablePrice)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPriceSummary(
            photoCount = photoCount,
            videoCount = videoCount,
            originalTotalPrice = photoOriginalPrice + videoOriginalPrice,
            discount = discount,
            totalPayablePrice = totalPayablePrice,
            strings = strings,
            modifier = Modifier.weight(1f)
        )
        AppPrimaryButton(
            onClick = onReviewClick,
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(buttonText, style = AppTheme.typography.ButtonText, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TabletBottomPriceBarContent(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    onReviewClick: () -> Unit,
    buttonText: String,
    strings: LocalizedStrings
) {
    val discount = (photoOriginalPrice + videoOriginalPrice) - (photoPayablePrice + videoPayablePrice)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPriceSummary(
            photoCount = photoCount,
            videoCount = videoCount,
            originalTotalPrice = photoOriginalPrice + videoOriginalPrice,
            discount = discount,
            totalPayablePrice = totalPayablePrice,
            strings = strings,
            modifier = Modifier.weight(1f)
        )
        AppPrimaryButton(
            onClick = onReviewClick,
            contentPadding = PaddingValues(horizontal = 34.dp, vertical = 15.dp),
            modifier = Modifier
                .widthIn(min = 260.dp, max = 320.dp)
                .heightIn(min = 56.dp)
        ) {
            Text(buttonText, style = AppTheme.typography.SectionTitle)
        }
    }
}

internal data class PriceSummaryColors(
    val selected: Color,
    val supporting: Color,
    val discount: Color,
    val totalLabel: Color,
    val totalPrice: Color
)

@Composable
internal fun CompactPriceSummary(
    photoCount: Int,
    videoCount: Int,
    originalTotalPrice: Int,
    discount: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier,
    colors: PriceSummaryColors = PriceSummaryColors(
        selected = AppTheme.colors.TextPrimary,
        supporting = AppTheme.colors.TextSecondary,
        discount = TaksimSuccess,
        totalLabel = AppTheme.colors.TextSecondary,
        totalPrice = AppTheme.colors.TextPrimary
    ),
    containerColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(28.dp)
) {
    val summaryContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.widthIn(min = 160.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${strings.selected}: ${photoCount + videoCount}",
                    style = AppTheme.typography.CardTitle,
                    color = colors.selected,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.photoShort}: $photoCount   ${strings.videoShort}: $videoCount",
                    style = AppTheme.typography.Body,
                    color = colors.supporting
                )
                if (discount > 0) {
                    Text(
                        text = "${strings.discount}: ${strings.price(discount)}",
                        style = AppTheme.typography.Body,
                        color = colors.discount
                    )
                }
            }
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = strings.total,
                    style = AppTheme.typography.HelperText,
                    color = colors.totalLabel
                )
                Text(
                    text = strings.price(totalPayablePrice),
                    style = AppTheme.typography.SectionTitle,
                    color = if (discount > 0) colors.discount else colors.totalPrice,
                    fontWeight = FontWeight.Bold
                )
                if (discount > 0) {
                    Text(
                        text = strings.price(originalTotalPrice),
                        style = AppTheme.typography.Body,
                        color = colors.supporting.copy(alpha = 0.72f),
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
    }
    if (containerColor == Color.Transparent) {
        Box(
            modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            summaryContent()
        }
    } else {
        AppCard(
            modifier = modifier,
            containerColor = containerColor,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            radius = AppTheme.shapes.Button
        ) {
            summaryContent()
        }
    }
}
