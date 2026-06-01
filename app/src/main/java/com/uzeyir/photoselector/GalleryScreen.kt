package com.uzeyir.photoselector

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uzeyir.photoselector.ui.theme.StudioDarkBackground
import com.uzeyir.photoselector.ui.theme.StudioDarkSurface
import com.uzeyir.photoselector.ui.theme.StudioFavorite
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
    val visibleMedia = if (activeTab == GalleryTab.All) photos else likedMediaItems
    val gridState = if (activeTab == GalleryTab.All) allGridState else favoritesGridState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GalleryHeader(
            selectedTab = activeTab,
            totalCount = photos.size,
            favoriteCount = likedMediaItems.size,
            strings = strings,
            onTabSelected = selectTab
        )

        if (activeTab == GalleryTab.Favorites && visibleMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = strings.noFavoritesYet,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp)
                    )
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val density = LocalDensity.current
                val widthDp = maxWidth.value.roundToInt()
                val columnCount = galleryColumnCountForWidthDp(widthDp)
                val thumbnailSizePx = thumbnailRequestSizePx(widthDp, density.density)
                val gridPadding = if (columnCount >= 4) 14.dp else 10.dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    contentPadding = PaddingValues(gridPadding),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                if (activeTab == GalleryTab.Favorites) {
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

@Composable
private fun GalleryHeader(
    selectedTab: GalleryTab,
    totalCount: Int,
    favoriteCount: Int,
    strings: LocalizedStrings,
    onTabSelected: (GalleryTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.galleryTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${strings.photo}: $totalCount   ${strings.selected}: $favoriteCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            GalleryTabButton(
                text = strings.allPhotos,
                selected = selectedTab == GalleryTab.All,
                onClick = { onTabSelected(GalleryTab.All) }
            )
            GalleryTabButton(
                text = "${strings.favorites} ($favoriteCount)",
                selected = selectedTab == GalleryTab.Favorites,
                onClick = { onTabSelected(GalleryTab.Favorites) }
            )
        }
    }
}

@Composable
private fun GalleryTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        contentPadding = PaddingValues(horizontal = 30.dp, vertical = 18.dp),
        modifier = Modifier.heightIn(min = 62.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
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

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .padding(3.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box {
            if (media.mediaType == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioDarkBackground),
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(StudioDarkSurface.copy(alpha = 0.64f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = strings.like,
                    tint = if (isLiked) StudioFavorite else Color.White
                )
            }
        }
    }
}
