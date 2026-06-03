package com.uzeyir.photoselector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.AppTheme
import com.uzeyir.photoselector.ui.theme.TaksimError
import com.uzeyir.photoselector.ui.theme.TaksimSuccess

@Composable
fun FullscreenTopBar(
    photo: MediaItemData,
    currentIndex: Int,
    totalCount: Int,
    strings: LocalizedStrings,
    onRotate: (() -> Unit)?,
    onVideoFullscreen: (() -> Unit)?,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = AppTheme.colors.SurfaceMuted
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = photo.displayName,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            when {
                onRotate != null -> IconButton(
                    onClick = onRotate,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppTheme.colors.SurfaceMuted
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = strings.rotate, tint = Color.White)
                }
                onVideoFullscreen != null -> IconButton(
                    onClick = onVideoFullscreen,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppTheme.colors.SurfaceMuted
                    )
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = strings.fullscreen, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun FullscreenBottomBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.70f)
                    )
                )
            )
    ) {
        content()
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
    isLiked: Boolean,
    onLikeToggle: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FullscreenBottomBarContainer(modifier = modifier) {
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
                    isLiked = isLiked,
                    onLikeToggle = onLikeToggle,
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
                    isLiked = isLiked,
                    onLikeToggle = onLikeToggle,
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
    isLiked: Boolean,
    onLikeToggle: () -> Unit,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FullscreenPriceSummaryCard(
            modifier = Modifier.weight(1f)
        ) {
            FullscreenReadablePriceSummary(
                photoCount = photoCount,
                videoCount = videoCount,
                photoOriginalPrice = photoOriginalPrice,
                photoPayablePrice = photoPayablePrice,
                videoOriginalPrice = videoOriginalPrice,
                videoPayablePrice = videoPayablePrice,
                totalPayablePrice = totalPayablePrice,
                strings = strings
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        FullscreenLikeButton(isLiked = isLiked, strings = strings, onLikeToggle = onLikeToggle)
        AppPrimaryButton(onClick = onReviewClick) {
            Text(strings.review, style = AppTheme.typography.ButtonText)
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
    isLiked: Boolean,
    onLikeToggle: () -> Unit,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FullscreenPriceSummaryCard(
            modifier = Modifier.weight(1.9f)
        ) {
            FullscreenReadablePriceSummary(
                photoCount = photoCount,
                videoCount = videoCount,
                photoOriginalPrice = photoOriginalPrice,
                photoPayablePrice = photoPayablePrice,
                videoOriginalPrice = videoOriginalPrice,
                videoPayablePrice = videoPayablePrice,
                totalPayablePrice = totalPayablePrice,
                strings = strings
            )
        }
        Row(
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FullscreenLikeButton(isLiked = isLiked, strings = strings, onLikeToggle = onLikeToggle)
            AppPrimaryButton(
                onClick = onReviewClick,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(strings.review, style = AppTheme.typography.ButtonText)
            }
        }
    }
}

@Composable
private fun FullscreenReadablePriceSummary(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings
) {
    val discount = (photoOriginalPrice + videoOriginalPrice) - (photoPayablePrice + videoPayablePrice)
    CompactPriceSummary(
        photoCount = photoCount,
        videoCount = videoCount,
        photoOriginalPrice = photoOriginalPrice,
        videoOriginalPrice = videoOriginalPrice,
        originalTotalPrice = photoOriginalPrice + videoOriginalPrice,
        discount = discount,
        totalPayablePrice = totalPayablePrice,
        strings = strings,
        colors = PriceSummaryColors(
            selected = Color.White,
            supporting = Color.White.copy(alpha = 0.76f),
            discount = TaksimSuccess,
            totalLabel = Color.White.copy(alpha = 0.70f),
            totalPrice = Color.White
        ),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    )
}

@Composable
private fun FullscreenLikeButton(
    isLiked: Boolean,
    strings: LocalizedStrings,
    onLikeToggle: () -> Unit
) {
    FilledIconButton(
        onClick = onLikeToggle,
        modifier = Modifier.size(58.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = AppTheme.colors.SurfaceMuted,
            contentColor = if (isLiked) TaksimError else Color.White
        )
    ) {
        AnimatedFavoriteIcon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = strings.like,
            isLiked = isLiked,
            tint = if (isLiked) TaksimError else Color.White,
            modifier = Modifier.size(32.dp)
        )
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
    isLiked: Boolean,
    onLikeToggle: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FullscreenBottomBarContainer(modifier = modifier) {
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
                    isLiked = isLiked,
                    onLikeToggle = onLikeToggle,
                    onReviewClick = onReviewClick
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FullscreenPriceSummaryCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        FullscreenReadablePriceSummary(
                            photoCount = photoCount,
                            videoCount = videoCount,
                            photoOriginalPrice = photoOriginalPrice,
                            photoPayablePrice = photoPayablePrice,
                            videoOriginalPrice = videoOriginalPrice,
                            videoPayablePrice = videoPayablePrice,
                            totalPayablePrice = totalPayablePrice,
                            strings = strings
                        )
                    }
                    FullscreenLikeButton(isLiked = isLiked, strings = strings, onLikeToggle = onLikeToggle)
                    AppPrimaryButton(onClick = onReviewClick) {
                        Text(strings.review, style = AppTheme.typography.ButtonText)
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
    discountedColor: Color = TaksimSuccess,
    modifier: Modifier = Modifier
) {
    AppCard(
        containerColor = AppTheme.colors.SurfaceElevated,
        modifier = modifier,
        radius = AppTheme.shapes.Button,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(strings.total, color = textColor, style = MaterialTheme.typography.labelMedium)
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
        Text(label, color = textColor, style = MaterialTheme.typography.bodySmall)
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

@Composable
fun FullscreenPriceSummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(AppTheme.colors.Surface)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            AppTheme.colors.BorderAccent.copy(alpha = 0.52f),
                            AppTheme.colors.BorderSubtle.copy(alpha = 0.40f),
                            AppTheme.colors.BorderAccent.copy(alpha = 0.52f)
                        )
                    )
                ),
                shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        content()
    }
}
