package com.uzeyir.photoselector

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.uzeyir.photoselector.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun PhotoDetailScreen(
    photos: List<MediaItemData>,
    selectedPhotoIndex: Int,
    likedPhotoUris: Set<Uri>,
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    totalPayablePrice: Int,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onPhotoSelected: (Int) -> Unit,
    rotationFor: (Uri) -> Int,
    onRotate: () -> Unit,
    onLikeToggle: (Uri) -> Unit,
    onReviewClick: () -> Unit
) {
    if (photos.isEmpty() || selectedPhotoIndex !in photos.indices) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = selectedPhotoIndex,
        pageCount = { photos.size }
    )
    val coroutineScope = rememberCoroutineScope()
    var controlsVisible by remember { mutableStateOf(true) }
    val videoController = rememberVideoPlaybackController(photos)
    val currentPhoto = photos[pagerState.currentPage]
    val isLiked = likedPhotoUris.contains(currentPhoto.uri)
    val fullscreenVideo = videoController.fullscreenMedia
    val sharedVideoPlayer = videoController.player

    BackHandler(enabled = videoController.isFullscreen) {
        videoController.exitFullscreen()
    }

    fun showControls() {
        controlsVisible = true
    }

    fun toggleControls() {
        controlsVisible = !controlsVisible
    }

    LaunchedEffect(selectedPhotoIndex, photos.size) {
        if (selectedPhotoIndex in photos.indices && pagerState.currentPage != selectedPhotoIndex) {
            pagerState.scrollToPage(selectedPhotoIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedPhotoIndex) {
            onPhotoSelected(pagerState.currentPage)
        }
    }

    LaunchedEffect(currentPhoto.uri) {
        videoController.clearIfDifferentPage(currentPhoto.uri)
        controlsVisible = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = photos[page]
            val rotationDegrees = if (media.mediaType == MediaType.Photo) rotationFor(media.uri) else 0
            if (media.mediaType == MediaType.Video) {
                VideoPoster(
                    media = media,
                    strings = strings,
                    onPlay = {
                        videoController.playFullscreen(media.uri)
                        controlsVisible = false
                    },
                    onSingleTap = { toggleControls() }
                )
            } else {
                ZoomablePhoto(
                    photo = media,
                    rotationDegrees = rotationDegrees,
                    onSingleTap = { toggleControls() },
                    onDoubleTapOrTransform = { showControls() }
                )
            }
        }

        if (controlsVisible) {
            if (pagerState.currentPage > 0) {
                FilledIconButton(
                    onClick = {
                        showControls()
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AppTheme.colors.SurfaceMuted,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = strings.previousPhoto)
                }
            }

            if (pagerState.currentPage < photos.lastIndex) {
                FilledIconButton(
                    onClick = {
                        showControls()
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AppTheme.colors.SurfaceMuted,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = strings.nextPhoto)
                }
            }

            FullscreenTopBar(
                photo = currentPhoto,
                currentIndex = pagerState.currentPage,
                totalCount = photos.size,
                strings = strings,
                onRotate = if (currentPhoto.mediaType == MediaType.Photo) onRotate else null,
                onVideoFullscreen = if (currentPhoto.mediaType == MediaType.Video) {
                    {
                        videoController.playFullscreen(currentPhoto.uri)
                        controlsVisible = false
                    }
                } else {
                    null
                },
                onBack = onBack
            )
        }

        if (currentPhoto.mediaType == MediaType.Video && controlsVisible) {
            VideoCompactBottomBar(
                photoCount = photoCount,
                videoCount = videoCount,
                photoOriginalPrice = photoOriginalPrice,
                photoPayablePrice = photoPayablePrice,
                videoOriginalPrice = videoOriginalPrice,
                videoPayablePrice = videoPayablePrice,
                totalPayablePrice = totalPayablePrice,
                strings = strings,
                isLiked = isLiked,
                onLikeToggle = { onLikeToggle(currentPhoto.uri) },
                onReviewClick = onReviewClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else if (controlsVisible) {
            FullscreenBottomBar(
                photoCount = photoCount,
                videoCount = videoCount,
                photoOriginalPrice = photoOriginalPrice,
                photoPayablePrice = photoPayablePrice,
                videoOriginalPrice = videoOriginalPrice,
                videoPayablePrice = videoPayablePrice,
                totalPayablePrice = totalPayablePrice,
                strings = strings,
                isLiked = isLiked,
                onLikeToggle = { onLikeToggle(currentPhoto.uri) },
                onReviewClick = onReviewClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        fullscreenVideo?.let { media ->
            sharedVideoPlayer?.let { player ->
                VideoFullscreenPlayer(
                    player = player,
                    media = media,
                    strings = strings,
                    onExit = { videoController.exitFullscreen() },
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

