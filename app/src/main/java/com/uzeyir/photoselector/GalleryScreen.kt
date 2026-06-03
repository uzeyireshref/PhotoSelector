package com.uzeyir.photoselector

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uzeyir.photoselector.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GalleryScreen(
    photos: List<MediaItemData>,
    likedPhotoUris: Set<Uri>,
    likedMediaItems: List<MediaItemData> = photos.filter { likedPhotoUris.contains(it.uri) },
    selectedTab: GalleryTab? = null,
    onTabSelected: (GalleryTab) -> Unit = {},
    strings: LocalizedStrings,
    allGridState: LazyGridState,
    favoritesGridState: LazyGridState,
    onPhotoClick: (Uri) -> Unit,
    onFavoritePhotoClick: (Uri) -> Unit = onPhotoClick,
    onLikeToggle: (Uri) -> Unit
) {
    var internalSelectedTab by remember { mutableStateOf(GalleryTab.All) }
    val activeTab = selectedTab ?: internalSelectedTab
    val selectTab: (GalleryTab) -> Unit = { tab ->
        internalSelectedTab = tab
        onTabSelected(tab)
    }
    AppScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryHeader(
                selectedTab = activeTab,
                totalCount = photos.size,
                favoriteCount = likedMediaItems.size,
                strings = strings,
                onTabSelected = selectTab
            )

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val direction = if (targetState == GalleryTab.Favorites) 1 else -1
                    (fadeIn(tween(120)) + slideInHorizontally(tween(140)) { direction * it / 10 })
                        .togetherWith(fadeOut(tween(90)) + slideOutHorizontally(tween(120)) { -direction * it / 10 })
                },
                label = "GalleryTabContent",
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { tab ->
                val visibleMedia = if (tab == GalleryTab.All) photos else likedMediaItems
                val gridState = if (tab == GalleryTab.All) allGridState else favoritesGridState
                if (tab == GalleryTab.Favorites && visibleMedia.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyFavoritesState(
                            title = strings.noFavoritesYet,
                            actionText = strings.goToAllPhotos,
                            onActionClick = { selectTab(GalleryTab.All) },
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val density = LocalDensity.current
                        val widthDp = maxWidth.value.roundToInt()
                        val columnCount = galleryColumnCountForWidthDp(widthDp)
                        val thumbnailSizePx = thumbnailRequestSizePx(widthDp, density.density)
                        val gridPadding = if (columnCount >= 4) 18.dp else 12.dp
                        val gridSpacing = if (columnCount >= 4) 10.dp else 8.dp

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            state = gridState,
                            contentPadding = PaddingValues(gridPadding),
                            verticalArrangement = Arrangement.spacedBy(gridSpacing),
                            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                        ) {
                            items(
                                items = visibleMedia,
                                key = { it.uri },
                                contentType = { it.mediaType }
                            ) { photo ->
                                PhotoItem(
                                    media = photo,
                                    isLiked = likedPhotoUris.contains(photo.uri),
                                    strings = strings,
                                    thumbnailSizePx = thumbnailSizePx,
                                    isScrollInProgress = gridState.isScrollInProgress,
                                    onClick = {
                                        if (tab == GalleryTab.Favorites) {
                                            onFavoritePhotoClick(photo.uri)
                                        } else {
                                            onPhotoClick(photo.uri)
                                        }
                                    },
                                    onLikeToggle = { onLikeToggle(photo.uri) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState(
    title: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .widthIn(min = 280.dp, max = 420.dp)
            .background(AppTheme.colors.SurfaceElevated, shape)
            .border(1.dp, AppTheme.colors.BorderSubtle.copy(alpha = 0.72f), shape)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(AppTheme.colors.SurfaceMuted, CircleShape)
                    .border(1.dp, AppTheme.colors.BorderSubtle.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = AppTheme.colors.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = AppTheme.typography.CardTitle,
                color = AppTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        AppOutlinedButton(onClick = onActionClick, modifier = Modifier.fillMaxWidth()) {
            Text(actionText, style = AppTheme.typography.ButtonText)
        }
    }
}

@Composable
private fun GalleryHeader(
    selectedTab: GalleryTab,
    totalCount: Int,
    favoriteCount: Int,
    strings: LocalizedStrings,
    onTabSelected: (GalleryTab) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        val compact = maxWidth < 620.dp
        val titleBlock: @Composable () -> Unit = {
            Column {
                Text(
                    text = strings.galleryTitle,
                    style = (if (compact) AppTheme.typography.SectionTitle else AppTheme.typography.ScreenTitle).copy(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                AppTheme.colors.Accent,
                                AppTheme.colors.AccentSoft
                            )
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${strings.photo}: $totalCount    ${strings.selected}: $favoriteCount",
                    style = AppTheme.typography.Body,
                    color = AppTheme.colors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val tabs: @Composable (Modifier) -> Unit = { tabModifier ->
            AppSegmentedControl(
                options = listOf(strings.allPhotos, "${strings.favorites} ($favoriteCount)"),
                selectedIndex = if (selectedTab == GalleryTab.All) 0 else 1,
                onSelected = { index ->
                    onTabSelected(if (index == 0) GalleryTab.All else GalleryTab.Favorites)
                },
                modifier = tabModifier,
                icons = listOf(Icons.Default.PhotoLibrary, Icons.Default.Favorite)
            )
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                titleBlock()
                tabs(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)) {
                    titleBlock()
                }
                tabs(Modifier.widthIn(min = 420.dp, max = 560.dp))
            }
        }
    }
}

@Composable
fun PhotoItem(
    media: MediaItemData,
    isLiked: Boolean,
    strings: LocalizedStrings,
    thumbnailSizePx: Int = 512,
    isScrollInProgress: Boolean = false,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val highQualityCacheKey = remember(media) { highQualityGalleryThumbnailCacheKey(media) }
    var fastThumbnail by remember(media.uri) { mutableStateOf<Bitmap?>(null) }
    var highQualityThumbnailPainter by remember(highQualityCacheKey) {
        mutableStateOf<Painter?>(cachedHighQualityGalleryThumbnailPainter(highQualityCacheKey))
    }
    val thumbnailRequest = remember(media.uri, thumbnailSizePx, highQualityCacheKey) {
        ImageRequest.Builder(context)
            .data(media.uri)
            .size(GALLERY_HIGH_QUALITY_IMAGE_SIZE_PX)
            .memoryCacheKey(highQualityCacheKey)
            .diskCacheKey(highQualityCacheKey)
            .crossfade(true)
            .build()
    }
    val fastThumbnailPainter = fastThumbnail?.let { BitmapPainter(it.asImageBitmap()) }
    val highQualityAlpha by animateFloatAsState(
        targetValue = if (highQualityThumbnailPainter != null) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "HighQualityThumbnailAlpha"
    )

    LaunchedEffect(media.uri, media.mediaType, thumbnailSizePx) {
        fastThumbnail = null
        if (media.mediaType == MediaType.Photo) {
            if (highQualityThumbnailPainter == null) {
                loadCachedHighQualityGalleryThumbnailBitmap(context, highQualityCacheKey)?.let { cachedBitmap ->
                    val cachedPainter = BitmapPainter(cachedBitmap.asImageBitmap())
                    highQualityThumbnailPainter = cachedPainter
                    cacheHighQualityGalleryThumbnailPainter(highQualityCacheKey, cachedPainter)
                }
            }
            if (highQualityThumbnailPainter == null) {
                fastThumbnail = loadFastSafThumbnail(
                    contentResolver = context.contentResolver,
                    uri = media.uri,
                    sizePx = thumbnailSizePx
                )
            }
        }
    }

    AppGalleryItem(
        selected = isLiked,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box {
            if (media.mediaType == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = media.displayName,
                        placeholder = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = strings.video,
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(52.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.SurfaceElevated)
                ) {
                    if (fastThumbnailPainter != null) {
                        Image(
                            painter = fastThumbnailPainter,
                            contentDescription = media.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (highQualityThumbnailPainter != null) {
                        Image(
                            painter = highQualityThumbnailPainter!!,
                            contentDescription = media.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = highQualityAlpha },
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (
                        shouldRequestHighQualityGalleryThumbnail(
                            isScrollInProgress = isScrollInProgress,
                            hasCachedHighQualityThumbnail = highQualityThumbnailPainter != null
                        )
                    ) {
                        AsyncImage(
                            model = thumbnailRequest,
                            contentDescription = media.displayName,
                            placeholder = fastThumbnailPainter,
                            onSuccess = { state ->
                                highQualityThumbnailPainter = state.painter
                                cacheHighQualityGalleryThumbnailPainter(highQualityCacheKey, state.painter)
                                val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                                if (bitmap != null) {
                                    coroutineScope.launch {
                                        cacheHighQualityGalleryThumbnailBitmap(context, highQualityCacheKey, bitmap)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0f },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(48.dp)
                    .clip(CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.32f), CircleShape)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isLiked) 0.38f else 0.16f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedFavoriteIcon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = strings.like,
                        isLiked = isLiked,
                        tint = if (isLiked) AppTheme.colors.Error else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
