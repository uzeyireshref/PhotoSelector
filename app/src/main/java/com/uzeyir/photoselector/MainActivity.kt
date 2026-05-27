package com.uzeyir.photoselector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.uzeyir.photoselector.ui.theme.PhotoSelectorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoSelectorTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoSelectorApp()
                }
            }
        }
    }
}

enum class Screen {
    FolderSelection,
    Gallery,
    PhotoDetail,
    Review,
    Confirmation
}

@Composable
fun PhotoSelectorApp(viewModel: PhotoViewModel = viewModel()) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen
    val likedPhotos = viewModel.likedPhotos
    val exportStatus = viewModel.exportStatus
    val exportSummary = viewModel.exportSummary
    val photos = viewModel.photos
    val viewerPhotos = viewModel.viewerPhotos
    val likedMediaItems = viewModel.likedMediaItems
    val viewerSource = viewModel.viewerSource
    val selectedPhotoIndex = viewModel.selectedPhotoIndex
    val selectionWarningMessage = viewModel.selectionWarningMessage
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val updateRepository = remember { GitHubUpdateRepository() }
    val lastFolderStore = remember(context) {
        SharedPreferencesLastFolderStore(
            context.getSharedPreferences(
                SharedPreferencesLastFolderStore.PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
        )
    }
    var updateStatus by remember { mutableStateOf<AppUpdateStatus>(AppUpdateStatus.Idle) }
    var language by remember { mutableStateOf(UiText.defaultLanguage) }
    val strings = UiText.strings(language)
    var sdCardOptions by remember { mutableStateOf<List<StorageVolume>>(emptyList()) }

    BackHandler(enabled = currentScreen != Screen.FolderSelection) {
        viewModel.handleBack()
    }

    LaunchedEffect(selectionWarningMessage) {
        val message = selectionWarningMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(strings.message(message))
        viewModel.clearSelectionWarning()
    }

    LaunchedEffect(Unit) {
        val persistedReadUris = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet()
        lastFolderStore.resolveAvailableFolder(persistedReadUris)?.let { savedFolder ->
            viewModel.loadMediaFromFolder(Uri.parse(savedFolder), context.contentResolver)
            viewModel.navigateTo(Screen.Gallery)
        }
    }

    fun handleSelectedFolder(uri: Uri?) {
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            lastFolderStore.save(it.toString())
            viewModel.loadMediaFromFolder(it, context.contentResolver)
            viewModel.navigateTo(Screen.Gallery)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handleSelectedFolder(result.data?.data)
    }

    fun openDocumentTree(intent: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)) {
        folderLauncher.launch(
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        )
    }

    fun openSdCardVolume(volume: StorageVolume) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            viewModel.warn(UiMessage.SdCardPickerUnsupported)
            return
        }
        openDocumentTree(volume.createOpenDocumentTreeIntent())
    }

    fun openSdCardPicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            viewModel.warn(UiMessage.SdCardPickerUnsupported)
            return
        }
        val storageManager = context.getSystemService(StorageManager::class.java)
        val removableVolumes = storageManager.storageVolumes.filter { it.isRemovable }
        when (removableVolumes.size) {
            0 -> viewModel.warn(UiMessage.SdCardNotInserted)
            1 -> openSdCardVolume(removableVolumes.first())
            else -> sdCardOptions = removableVolumes
        }
    }

    if (sdCardOptions.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { sdCardOptions = emptyList() },
            title = { Text(strings.chooseSdCard) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sdCardOptions.forEachIndexed { index, volume ->
                        TextButton(
                            onClick = {
                                sdCardOptions = emptyList()
                                openSdCardVolume(volume)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(volume.getDescription(context) ?: "${strings.chooseSdCard} ${index + 1}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sdCardOptions = emptyList() }) {
                    Text(strings.back)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen == Screen.Gallery || currentScreen == Screen.Review) {
                BottomPriceBar(
                    photoCount = viewModel.selectedPhotoCount,
                    videoCount = viewModel.selectedVideoCount,
                    photoOriginalPrice = viewModel.photoBasePrice,
                    photoPayablePrice = viewModel.photoDisplayPrice,
                    videoOriginalPrice = viewModel.videoBasePrice,
                    videoPayablePrice = viewModel.videoDisplayPrice,
                    totalPayablePrice = viewModel.totalDisplayPrice,
                    onReviewClick = {
                        if (currentScreen == Screen.Gallery) viewModel.goToReviewOrWarn()
                        else if (currentScreen == Screen.Review) viewModel.goToConfirmationOrWarn()
                    },
                    buttonText = if (currentScreen == Screen.Gallery) strings.reviewSelection else strings.confirmSelection,
                    strings = strings
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                Screen.FolderSelection -> FolderSelectionScreen(onFolderSelected = {
                    openDocumentTree()
                }, onOpenSdCard = {
                    openSdCardPicker()
                }, updateStatus = updateStatus, language = language, onLanguageSelected = { selectedLanguage ->
                    language = selectedLanguage
                }, strings = strings, onCheckUpdate = {
                    coroutineScope.launch {
                        updateStatus = AppUpdateStatus.Checking
                        runCatching {
                            val latestRelease = updateRepository.fetchLatestRelease()
                            when (val decision = UpdatePolicy.decide(BuildConfig.VERSION_CODE, latestRelease)) {
                                UpdateDecision.UpToDate -> {
                                    updateStatus = AppUpdateStatus.UpToDate
                                }
                                is UpdateDecision.UpdateAvailable -> {
                                    updateStatus = AppUpdateStatus.Available(decision.updateInfo.versionName)
                                    delay(450)
                                    updateStatus = AppUpdateStatus.Downloading
                                    val apkFile = updateRepository.downloadApk(context, decision.updateInfo)
                                    updateStatus = AppUpdateStatus.ReadyToInstall
                                    ApkInstaller.openInstaller(context, apkFile)
                                }
                            }
                        }.onFailure {
                            updateStatus = AppUpdateStatus.Error
                        }
                    }
                })
                Screen.Gallery -> GalleryScreen(
                    photos = photos,
                    likedPhotos = likedPhotos,
                    strings = strings,
                    onPhotoClick = { uri -> viewModel.openPhoto(uri) },
                    onLikeToggle = { uri -> viewModel.toggleLike(uri) }
                )
                Screen.PhotoDetail -> PhotoDetailScreen(
                    photos = viewerPhotos,
                    selectedPhotoIndex = selectedPhotoIndex,
                    likedPhotos = likedPhotos,
                    photoCount = viewModel.selectedPhotoCount,
                    videoCount = viewModel.selectedVideoCount,
                    photoOriginalPrice = viewModel.photoBasePrice,
                    photoPayablePrice = viewModel.photoDisplayPrice,
                    videoOriginalPrice = viewModel.videoBasePrice,
                    videoPayablePrice = viewModel.videoDisplayPrice,
                    totalPayablePrice = viewModel.totalDisplayPrice,
                    strings = strings,
                    onBack = {
                        viewModel.navigateTo(
                            if (viewerSource == PhotoViewerSource.Review) Screen.Review else Screen.Gallery
                        )
                    },
                    onPhotoSelected = { index -> viewModel.openPhotoAt(index) },
                    rotationFor = { uri -> viewModel.rotationFor(uri) },
                    onRotate = { viewModel.rotateSelectedMedia() },
                    onLikeToggle = { uri -> viewModel.toggleLike(uri) },
                    onReviewClick = { viewModel.goToReviewOrWarn() }
                )
                Screen.Review -> ReviewScreen(
                    likedPhotos = likedMediaItems,
                    strings = strings,
                    onBack = { viewModel.navigateTo(Screen.Gallery) },
                    onPhotoClick = { uri -> viewModel.openLikedPhoto(uri) },
                    onRemoveLike = { uri -> viewModel.toggleLike(uri) }
                )
                Screen.Confirmation -> ConfirmationScreen(
                    summary = exportSummary,
                    exportStatus = exportStatus,
                    strings = strings,
                    onBack = { viewModel.navigateTo(Screen.Review) },
                    onConfirmExport = {
                        coroutineScope.launch {
                            viewModel.exportSelection(context.contentResolver)
                        }
                    },
                    onFinished = { viewModel.reset() }
                )
            }
        }
    }
}

@Composable
fun FolderSelectionScreen(
    onFolderSelected: () -> Unit,
    onOpenSdCard: () -> Unit,
    updateStatus: AppUpdateStatus,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    strings: LocalizedStrings,
    onCheckUpdate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        LanguageSelector(
            selectedLanguage = language,
            strings = strings,
            onLanguageSelected = onLanguageSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(strings.folderTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onFolderSelected) {
                Text(strings.selectFolder)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenSdCard,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.SdStorage, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.openSdCard)
            }
            Spacer(modifier = Modifier.height(24.dp))
            UpdateCheckButton(
                updateStatus = updateStatus,
                strings = strings,
                onClick = onCheckUpdate
            )
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLanguage: AppLanguage,
    strings: LocalizedStrings,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = selectedLanguage == AppLanguage.Turkish,
            onClick = { onLanguageSelected(AppLanguage.Turkish) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text(strings.languageOptionTurkish)
        }
        SegmentedButton(
            selected = selectedLanguage == AppLanguage.English,
            onClick = { onLanguageSelected(AppLanguage.English) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text(strings.languageOptionEnglish)
        }
    }
}

@Composable
fun UpdateCheckButton(
    updateStatus: AppUpdateStatus,
    strings: LocalizedStrings,
    onClick: () -> Unit
) {
    val isBusy = updateStatus == AppUpdateStatus.Checking || updateStatus == AppUpdateStatus.Downloading
    val isUpToDate = updateStatus == AppUpdateStatus.UpToDate
    val label = updateStatus.label(strings)
    val colors = if (isUpToDate) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32),
            contentColor = Color.White
        )
    } else {
        ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        enabled = !isBusy,
        colors = colors,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.widthIn(min = 240.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        } else {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(label)
    }
}

@Composable
fun GalleryScreen(
    photos: List<MediaItemData>,
    likedPhotos: List<Uri>,
    strings: LocalizedStrings,
    onPhotoClick: (Uri) -> Unit,
    onLikeToggle: (Uri) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(photos, key = { it.uri }) { photo ->
            PhotoItem(
                media = photo,
                isLiked = likedPhotos.contains(photo.uri),
                strings = strings,
                onClick = { onPhotoClick(photo.uri) },
                onLikeToggle = { onLikeToggle(photo.uri) }
            )
        }
    }
}

@Composable
fun PhotoItem(
    media: MediaItemData,
    isLiked: Boolean,
    strings: LocalizedStrings,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box {
            if (media.mediaType == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF181818)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = media.uri,
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
                    model = media.uri,
                    contentDescription = media.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = strings.like,
                    tint = if (isLiked) Color.Red else Color.White
                )
            }
        }
    }
}

@Composable
fun PhotoDetailScreen(
    photos: List<MediaItemData>,
    selectedPhotoIndex: Int,
    likedPhotos: List<Uri>,
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
    var controlsWakeKey by remember { mutableIntStateOf(0) }
    val currentPhoto = photos[pagerState.currentPage]
    val isCurrentVideo = currentPhoto.mediaType == MediaType.Video
    val isLiked = likedPhotos.contains(currentPhoto.uri)

    fun showControlsAndResetTimer() {
        controlsVisible = true
        controlsWakeKey += 1
    }

    fun toggleControls() {
        controlsVisible = !controlsVisible
        if (controlsVisible) {
            controlsWakeKey += 1
        }
    }

    fun keepVisibleControlsAlive() {
        if (controlsVisible) {
            controlsWakeKey += 1
        }
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

    LaunchedEffect(controlsVisible, controlsWakeKey, pagerState.currentPage, isCurrentVideo) {
        if (controlsVisible && !isCurrentVideo) {
            delay(2_500)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = photos[page]
            val rotationDegrees = rotationFor(media.uri)
            if (media.mediaType == MediaType.Video) {
                VideoPlayer(
                    media = media,
                    isActive = page == pagerState.currentPage,
                    rotationDegrees = rotationDegrees,
                    onSingleTap = { toggleControls() }
                )
            } else {
                ZoomablePhoto(
                    photo = media,
                    rotationDegrees = rotationDegrees,
                    onSingleTap = { toggleControls() },
                    onDoubleTapOrTransform = { keepVisibleControlsAlive() }
                )
            }
        }

        if (controlsVisible || isCurrentVideo) {
            if (pagerState.currentPage > 0) {
                FilledIconButton(
                    onClick = {
                        showControlsAndResetTimer()
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.42f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = strings.previousPhoto)
                }
            }

            if (pagerState.currentPage < photos.lastIndex) {
                FilledIconButton(
                    onClick = {
                        showControlsAndResetTimer()
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.42f),
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
                onRotate = onRotate,
                onBack = onBack
            )
        }

        if (isCurrentVideo) {
            VideoCompactBottomBar(
                photoCount = photoCount,
                videoCount = videoCount,
                totalPayablePrice = totalPayablePrice,
                isLiked = isLiked,
                strings = strings,
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
                isLiked = isLiked,
                strings = strings,
                onLikeToggle = { onLikeToggle(currentPhoto.uri) },
                onReviewClick = onReviewClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun ZoomablePhoto(
    photo: PhotoItemData,
    rotationDegrees: Int,
    onSingleTap: () -> Unit,
    onDoubleTapOrTransform: () -> Unit
) {
    var scale by remember(photo.uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(photo.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(photo.uri) { mutableFloatStateOf(0f) }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photo.uri) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        onDoubleTapOrTransform()
                        if (scale > 1f) {
                            resetZoom()
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(photo.uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pressed: Boolean
                    do {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                        val pointerCount = event.changes.count { it.pressed }
                        if (!shouldHandlePhotoTransform(pointerCount = pointerCount, scale = scale)) {
                            continue
                        }

                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (zoom != 1f || pan != Offset.Zero) {
                            onDoubleTapOrTransform()
                        }

                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }

                        event.changes.forEach { change ->
                            if (change.positionChanged()) {
                                change.consume()
                            }
                        }
                    } while (pressed)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotationDegrees.toFloat()
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun VideoPlayer(
    media: MediaItemData,
    isActive: Boolean,
    rotationDegrees: Int,
    onSingleTap: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(media.uri))
            prepare()
        }
    }

    LaunchedEffect(isActive) {
        player.playWhenReady = isActive
        if (isActive) {
            player.play()
        } else {
            player.pause()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(media.uri) {
                detectTapGestures(onTap = { onSingleTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = rotationDegrees.toFloat())
        )
    }
}

@Composable
fun FullscreenTopBar(
    photo: PhotoItemData,
    currentIndex: Int,
    totalCount: Int,
    strings: LocalizedStrings,
    onRotate: () -> Unit,
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
            IconButton(onClick = onRotate) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = strings.rotate, tint = Color.White)
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
    isLiked: Boolean,
    strings: LocalizedStrings,
    onLikeToggle: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.62f),
        modifier = modifier.fillMaxWidth()
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.selectedCount(photoCount + videoCount), color = Color.White)
                SelectionPriceSummary(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    photoOriginalPrice = photoOriginalPrice,
                    photoPayablePrice = photoPayablePrice,
                    videoOriginalPrice = videoOriginalPrice,
                    videoPayablePrice = videoPayablePrice,
                    totalPayablePrice = totalPayablePrice,
                    strings = strings,
                    textColor = Color.White,
                    discountedColor = Color(0xFF81C784)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledIconButton(
                    onClick = onLikeToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isLiked) Color.White else Color.White.copy(alpha = 0.14f),
                        contentColor = if (isLiked) Color.Red else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = strings.like
                    )
                }
                Button(onClick = onReviewClick) {
                    Text(strings.review)
                }
            }
        }
    }
}

@Composable
fun VideoCompactBottomBar(
    photoCount: Int,
    videoCount: Int,
    totalPayablePrice: Int,
    isLiked: Boolean,
    strings: LocalizedStrings,
    onLikeToggle: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.68f),
        modifier = modifier.fillMaxWidth()
    ) {
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
            FilledIconButton(
                onClick = onLikeToggle,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isLiked) Color.White else Color.White.copy(alpha = 0.14f),
                    contentColor = if (isLiked) Color.Red else Color.White
                )
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = strings.like
                )
            }
            Button(onClick = onReviewClick) {
                Text(strings.review)
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
    discountedColor: Color = Color(0xFF2E7D32)
) {
    Surface(
        color = textColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
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

@Composable
fun ReviewScreen(
    likedPhotos: List<MediaItemData>,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onPhotoClick: (Uri) -> Unit,
    onRemoveLike: (Uri) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Button(onClick = onBack) { Text(strings.backToGallery) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(strings.reviewLikedPhotos, style = MaterialTheme.typography.titleLarge)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(likedPhotos, key = { it.uri }) { photo ->
                PhotoItem(
                    media = photo,
                    isLiked = true,
                    strings = strings,
                    onClick = { onPhotoClick(photo.uri) },
                    onLikeToggle = { onRemoveLike(photo.uri) }
                )
            }
        }
    }
}

@Composable
fun ConfirmationScreen(
    summary: ExportSummary,
    exportStatus: ExportStatus,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onConfirmExport: () -> Unit,
    onFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(strings.finalConfirmation, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        Text(strings.selectedJpgCount(summary.selectedJpgCount), style = MaterialTheme.typography.titleMedium)
        Text(strings.matchingRawCount(summary.matchedRawCount), style = MaterialTheme.typography.titleMedium)
        Text(strings.selectedVideoCount(summary.selectedVideoCount), style = MaterialTheme.typography.titleMedium)
        Text(strings.totalFileCount(summary.totalFileCount), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(28.dp))

        when (exportStatus) {
            ExportStatus.Idle -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) {
                        Text(strings.back)
                    }
                    Button(
                        onClick = onConfirmExport,
                        enabled = summary.totalFileCount > 0
                    ) {
                        Text(strings.copyJpgAndRaw)
                    }
                }
            }

            ExportStatus.Copying -> {
                LinearProgressIndicator(modifier = Modifier.widthIn(min = 320.dp, max = 520.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(strings.copyingSelectedFiles)
            }

            is ExportStatus.Success -> {
                Text(strings.exportComplete, color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.folderName(exportStatus.folderName), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(strings.copiedFileCount(exportStatus.copiedFiles))
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onFinished) {
                    Text(strings.finish)
                }
            }

            is ExportStatus.Error -> {
                Text(strings.exportFailed, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.message(exportStatus.message, exportStatus.argument), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) {
                        Text(strings.back)
                    }
                    Button(onClick = onConfirmExport) {
                        Text(strings.retry)
                    }
                }
            }
        }
    }
}

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
    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(strings.selectedCount(photoCount + videoCount))
                SelectionPriceSummary(
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
            Button(onClick = onReviewClick) {
                Text(buttonText)
            }
        }
    }
}
