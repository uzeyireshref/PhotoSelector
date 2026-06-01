package com.uzeyir.photoselector

import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uzeyir.photoselector.ui.theme.StudioDarkBackground
import com.uzeyir.photoselector.ui.theme.StudioFavorite
import kotlin.math.roundToInt

@Composable
fun GalleryScreen(
    photos: List<MediaItemData>,
    likedPhotoUris: Set<Uri>,
    likedMediaItems: List<MediaItemData> = photos.filter { likedPhotoUris.contains(it.uri) },
    selectedTab: GalleryTab? = null,
    onTabSelected: (GalleryTab) -> Unit = {},
    strings: LocalizedStrings,
    gridState: LazyGridState,
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
    onClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    val context = LocalContext.current
    val thumbnailRequest = remember(media.uri, thumbnailSizePx) {
        ImageRequest.Builder(context)
            .data(media.uri)
            .size(thumbnailSizePx)
            .crossfade(true)
            .build()
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
                AsyncImage(
                    model = thumbnailRequest,
                    contentDescription = media.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.34f), CircleShape)
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
