package com.uzeyir.photoselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.StudioFavoriteOnDark
import com.uzeyir.photoselector.ui.theme.StudioSuccess
import com.uzeyir.photoselector.ui.theme.StudioSuccessOnDark

@Composable
fun FullscreenTopBar(
    photo: MediaItemData,
    currentIndex: Int,
    totalCount: Int,
    strings: LocalizedStrings,
    isLiked: Boolean,
    onRotate: (() -> Unit)?,
    onVideoFullscreen: (() -> Unit)?,
    onLikeToggle: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.56f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = photo.displayName,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            when {
                onRotate != null -> IconButton(onClick = onRotate) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = strings.rotate, tint = Color.White)
                }
                onVideoFullscreen != null -> IconButton(onClick = onVideoFullscreen) {
                    Icon(Icons.Default.Fullscreen, contentDescription = strings.fullscreen, tint = Color.White)
                }
            }
            IconButton(onClick = onLikeToggle) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = strings.like,
                    tint = if (isLiked) StudioFavoriteOnDark else Color.White
                )
            }
        }
    }
}

@Composable
fun FullscreenBottomBar(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.62f),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 600.dp) {
                TabletFullscreenBottomBarContent(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    photoOriginalPrice = photoOriginalPrice,
                    photoPayablePrice = photoPayablePrice,
                    videoOriginalPrice = videoOriginalPrice,
                    videoPayablePrice = videoPayablePrice,
                    totalPayablePrice = totalPayablePrice,
                    strings = strings,
                    onReviewClick = onReviewClick
                )
            } else {
                PhoneFullscreenBottomBarContent(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    photoOriginalPrice = photoOriginalPrice,
                    photoPayablePrice = photoPayablePrice,
                    videoOriginalPrice = videoOriginalPrice,
                    videoPayablePrice = videoPayablePrice,
                    totalPayablePrice = totalPayablePrice,
                    strings = strings,
                    onReviewClick = onReviewClick
                )
            }
        }
    }
}

@Composable
private fun PhoneFullscreenBottomBarContent(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val discount = (photoOriginalPrice + videoOriginalPrice) - (photoPayablePrice + videoPayablePrice)
            Text("${strings.selected}: ${photoCount + videoCount}", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("${strings.photoShort}: $photoCount   ${strings.videoShort}: $videoCount", color = Color.White.copy(alpha = 0.72f))
            if (discount > 0) {
                Text("${strings.discount}: ${strings.price(discount)}", color = StudioSuccessOnDark, style = MaterialTheme.typography.bodyMedium)
            }
            Text("${strings.total}: ${strings.price(totalPayablePrice)}", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Button(onClick = onReviewClick) {
            Text(strings.review)
        }
    }
}

@Composable
private fun TabletFullscreenBottomBarContent(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPriceSummary(
            photoCount = photoCount,
            videoCount = videoCount,
            discount = (photoOriginalPrice + videoOriginalPrice) - (photoPayablePrice + videoPayablePrice),
            totalPayablePrice = totalPayablePrice,
            strings = strings,
            modifier = Modifier.weight(1.5f),
            colors = PriceSummaryColors(
                selected = Color.White,
                supporting = Color.White.copy(alpha = 0.74f),
                discount = StudioSuccessOnDark,
                totalLabel = Color.White.copy(alpha = 0.72f),
                totalPrice = Color.White
            ),
            containerColor = Color.White.copy(alpha = 0.10f),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onReviewClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(strings.review, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun VideoCompactBottomBar(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.68f),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 600.dp) {
                TabletFullscreenBottomBarContent(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    photoOriginalPrice = photoOriginalPrice,
                    photoPayablePrice = photoPayablePrice,
                    videoOriginalPrice = videoOriginalPrice,
                    videoPayablePrice = videoPayablePrice,
                    totalPayablePrice = totalPayablePrice,
                    strings = strings,
                    onReviewClick = onReviewClick
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.photo}: $photoCount  ${strings.video}: $videoCount",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = strings.price(totalPayablePrice),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(onClick = onReviewClick) {
                        Text(strings.review)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionPriceSummary(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    textColor: Color = LocalContentColor.current,
    discountedColor: Color = StudioSuccess,
    modifier: Modifier = Modifier
) {
    Surface(
        color = textColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(strings.total, color = textColor.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
            if (photoCount > 0) {
                PriceLine(
                    label = "${strings.photo}: $photoCount",
                    originalPrice = photoOriginalPrice,
                    payablePrice = photoPayablePrice,
                    strings = strings,
                    textColor = textColor,
                    discountedColor = discountedColor
                )
            }
            if (videoCount > 0) {
                PriceLine(
                    label = "${strings.video}: $videoCount",
                    originalPrice = videoOriginalPrice,
                    payablePrice = videoPayablePrice,
                    strings = strings,
                    textColor = textColor,
                    discountedColor = discountedColor
                )
            }
            Text(
                text = strings.price(totalPayablePrice),
                color = if (photoOriginalPrice > photoPayablePrice || videoOriginalPrice > videoPayablePrice) discountedColor else textColor,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun PriceLine(
    label: String,
    originalPrice: Int,
    payablePrice: Int,
    strings: LocalizedStrings,
    textColor: Color,
    discountedColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textColor.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
        if (originalPrice > payablePrice) {
            Text(
                text = strings.price(originalPrice),
                color = textColor.copy(alpha = 0.62f),
                textDecoration = TextDecoration.LineThrough,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = strings.price(payablePrice),
                color = discountedColor,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                text = strings.price(payablePrice),
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
