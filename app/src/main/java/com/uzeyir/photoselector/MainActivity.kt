package com.uzeyir.photoselector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uzeyir.photoselector.ui.theme.PhotoSelectorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

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

private val VideoControlsBottomInset = 96.dp

fun rotatedMediaSize(containerWidthPx: Int, containerHeightPx: Int, rotationDegrees: Int): Pair<Int, Int> {
    if (containerWidthPx <= 0 || containerHeightPx <= 0) return containerWidthPx to containerHeightPx
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    return if (normalizedRotation == 90 || normalizedRotation == 270) {
        containerHeightPx to containerWidthPx
    } else {
        containerWidthPx to containerHeightPx
    }
}

fun videoFullscreenRotationDegrees(videoWidth: Int, videoHeight: Int): Int =
    if (videoWidth > 0 && videoHeight > 0 && videoWidth > videoHeight) 90 else 0

fun fullscreenVideoSurfaceSize(
    containerWidthPx: Int,
    containerHeightPx: Int,
    videoWidth: Int,
    videoHeight: Int,
    rotationDegrees: Int
): Pair<Int, Int> {
    if (containerWidthPx <= 0 || containerHeightPx <= 0 || videoWidth <= 0 || videoHeight <= 0) {
        return rotatedMediaSize(containerWidthPx, containerHeightPx, rotationDegrees)
    }

    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    val isQuarterTurn = normalizedRotation == 90 || normalizedRotation == 270
    val visualVideoWidth = if (isQuarterTurn) videoHeight else videoWidth
    val visualVideoHeight = if (isQuarterTurn) videoWidth else videoHeight
    val containerAspect = containerWidthPx.toDouble() / containerHeightPx.toDouble()
    val visualVideoAspect = visualVideoWidth.toDouble() / visualVideoHeight.toDouble()
    val visualSize = if (visualVideoAspect > containerAspect) {
        ceil(containerHeightPx * visualVideoAspect).toInt() to containerHeightPx
    } else {
        containerWidthPx to ceil(containerWidthPx / visualVideoAspect).toInt()
    }

    return if (isQuarterTurn) {
        visualSize.second to visualSize.first
    } else {
        visualSize
    }
}

fun shouldRenderInlineVideoPlayer(mediaUri: Uri, fullscreenVideoUri: Uri?): Boolean =
    fullscreenVideoUri != mediaUri

fun galleryColumnCountForWidthDp(widthDp: Int): Int =
    if (widthDp >= 600) 4 else 3

fun thumbnailRequestSizePx(widthDp: Int, density: Float): Int {
    val columns = galleryColumnCountForWidthDp(widthDp)
    val contentPaddingDp = 16
    val cellWidthDp = (widthDp - contentPaddingDp).coerceAtLeast(columns) / columns.toFloat()
    return (cellWidthDp * density).roundToInt().coerceAtLeast(1)
}

@Composable
fun PhotoSelectorApp(viewModel: PhotoViewModel = viewModel()) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen
    val likedPhotoUriSet = viewModel.likedPhotoUriSet
    val exportStatus = viewModel.exportStatus
    val exportSummary = viewModel.exportSummary
    val photos = viewModel.photos
    val isLoadingMedia = viewModel.isLoadingMedia
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
    val galleryGridState = rememberLazyGridState()
    val galleryContentKey = viewModel.mediaLoadVersion

    BackHandler(enabled = currentScreen != Screen.FolderSelection) {
        viewModel.handleBack()
    }

    LaunchedEffect(selectionWarningMessage) {
        val message = selectionWarningMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(strings.message(message))
        viewModel.clearSelectionWarning()
    }

    LaunchedEffect(galleryContentKey) {
        if (photos.isNotEmpty()) {
            galleryGridState.scrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        val persistedReadUris = persistedPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet()
        val persistedWriteUris = persistedPermissions
            .filter { it.isWritePermission }
            .map { it.uri.toString() }
            .toSet()
        lastFolderStore.resolveAvailableFolder(
            persistedReadUris = persistedReadUris,
            persistedWriteUris = persistedWriteUris
        )?.let { savedFolder ->
            runCatching {
                viewModel.loadMediaFromFolder(Uri.parse(savedFolder), context.contentResolver)
                viewModel.navigateTo(Screen.Gallery)
            }.onFailure {
                lastFolderStore.clear()
                viewModel.reset()
                viewModel.warn(UiMessage.FolderLoadFailed)
            }
        }
    }

    fun handleSelectedFolder(uri: Uri?) {
        uri?.let { selectedUri ->
            val permissionGranted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.onFailure {
                viewModel.warn(UiMessage.FolderLoadFailed)
            }.isSuccess
            if (!permissionGranted) return

            coroutineScope.launch {
                runCatching {
                    viewModel.loadMediaFromFolder(selectedUri, context.contentResolver)
                    lastFolderStore.save(selectedUri.toString())
                    viewModel.navigateTo(Screen.Gallery)
                }.onFailure {
                    lastFolderStore.clear()
                    viewModel.reset()
                    viewModel.warn(UiMessage.FolderLoadFailed)
                }
            }
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
        openDocumentTree(volume.createOpenDocumentTreeIntent().withSdCardDcimInitialUri())
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
                Screen.FolderSelection -> FolderSelectionScreen(
                    onFolderSelected = { openDocumentTree() },
                    onOpenSdCard = { openSdCardPicker() },
                    updateStatus = updateStatus,
                    isLoadingMedia = isLoadingMedia,
                    language = language,
                    onLanguageSelected = { selectedLanguage -> language = selectedLanguage },
                    strings = strings,
                    onCheckUpdate = {
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
                    }
                )
                Screen.Gallery -> GalleryScreen(
                    photos = photos,
                    likedPhotoUris = likedPhotoUriSet,
                    strings = strings,
                    gridState = galleryGridState,
                    onPhotoClick = { uri -> viewModel.openPhoto(uri) },
                    onLikeToggle = { uri -> viewModel.toggleLike(uri) }
                )
                Screen.PhotoDetail -> PhotoDetailScreen(
                    photos = viewerPhotos,
                    selectedPhotoIndex = selectedPhotoIndex,
                    likedPhotoUris = likedPhotoUriSet,
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

private fun Intent.withSdCardDcimInitialUri(): Intent {
    val rootUri = getParcelableExtraCompat(DocumentsContract.EXTRA_INITIAL_URI) ?: return this
    val dcimUri = sdCardDcimInitialUriStringFromRootUri(rootUri.toString()) ?: return this
    return putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(dcimUri))
}

@Suppress("DEPRECATION")
private fun Intent.getParcelableExtraCompat(name: String): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, Uri::class.java)
    } else {
        getParcelableExtra(name)
    }

internal fun sdCardDcimInitialUriStringFromRootUri(rootUri: String): String? {
    val rootPrefix = "content://com.android.externalstorage.documents/root/"
    if (!rootUri.startsWith(rootPrefix)) return null
    val volumeId = rootUri
        .removePrefix(rootPrefix)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    if (volumeId.isBlank()) return null
    return "content://com.android.externalstorage.documents/document/${volumeId}%3ADCIM"
}

@Composable
fun FolderSelectionScreen(
    onFolderSelected: () -> Unit,
    onOpenSdCard: () -> Unit,
    updateStatus: AppUpdateStatus,
    isLoadingMedia: Boolean,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    strings: LocalizedStrings,
    onCheckUpdate: () -> Unit
) {
    val startSubtitle = when (language) {
        AppLanguage.Turkish -> "Fotoğraf ve videolarınızı seçmek için kaynak klasörü açın."
        AppLanguage.English -> "Open the source folder to choose your photos and videos."
    }
    val storageHint = when (language) {
        AppLanguage.Turkish -> "Telefon hafızası veya SD karttan devam edin."
        AppLanguage.English -> "Continue from phone storage or an SD card."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7FAF8),
                        Color(0xFFEAF3EF),
                        Color(0xFFFDF8EF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(76.dp))
            StartScreenPanel(
                title = strings.folderTitle,
                subtitle = startSubtitle,
                storageHint = storageHint,
                onFolderSelected = onFolderSelected,
                onOpenSdCard = onOpenSdCard,
                updateStatus = updateStatus,
                isLoadingMedia = isLoadingMedia,
                strings = strings,
                onCheckUpdate = onCheckUpdate,
                modifier = Modifier.widthIn(max = 440.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        LanguageSelector(
            selectedLanguage = language,
            strings = strings,
            onLanguageSelected = onLanguageSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }
}

@Composable
private fun StartScreenPanel(
    title: String,
    subtitle: String,
    storageHint: String,
    onFolderSelected: () -> Unit,
    onOpenSdCard: () -> Unit,
    updateStatus: AppUpdateStatus,
    isLoadingMedia: Boolean,
    strings: LocalizedStrings,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StartScreenMark()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF14211F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF52615D),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onFolderSelected,
                enabled = !isLoadingMedia,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF17423C),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.selectFolder)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenSdCard,
                enabled = !isLoadingMedia,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF17423C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.SdStorage, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.openSdCard)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = storageHint,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6E7A76)
            )
            Spacer(modifier = Modifier.height(22.dp))
            if (isLoadingMedia) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
            }
            UpdateCheckButton(
                updateStatus = updateStatus,
                strings = strings,
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StartScreenMark() {
    Surface(
        modifier = Modifier.size(72.dp),
        color = Color(0xFF17423C),
        shape = CircleShape,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {}
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = Color(0xFFFFD18A),
                modifier = Modifier.size(34.dp)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.widthIn(min = 240.dp)
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
    likedPhotoUris: Set<Uri>,
    strings: LocalizedStrings,
    gridState: LazyGridState,
    onPhotoClick: (Uri) -> Unit,
    onLikeToggle: (Uri) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthDp = maxWidth.value.roundToInt()
        val columnCount = galleryColumnCountForWidthDp(widthDp)
        val thumbnailSizePx = thumbnailRequestSizePx(widthDp, density.density)
        val gridPadding = if (columnCount >= 4) 10.dp else 8.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = gridState,
            contentPadding = PaddingValues(gridPadding)
        ) {
            items(photos, key = { it.uri }) { photo ->
                PhotoItem(
                    media = photo,
                    isLiked = likedPhotoUris.contains(photo.uri),
                    strings = strings,
                    thumbnailSizePx = thumbnailSizePx,
                    onClick = { onPhotoClick(photo.uri) },
                    onLikeToggle = { onLikeToggle(photo.uri) }
                )
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
    var fullscreenVideo by remember { mutableStateOf<MediaItemData?>(null) }
    val currentPhoto = photos[pagerState.currentPage]
    val isLiked = likedPhotoUris.contains(currentPhoto.uri)

    BackHandler(enabled = fullscreenVideo != null) {
        fullscreenVideo = null
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
        fullscreenVideo = null
        controlsVisible = true
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
            val rotationDegrees = if (media.mediaType == MediaType.Photo) rotationFor(media.uri) else 0
            if (media.mediaType == MediaType.Video) {
                if (shouldRenderInlineVideoPlayer(media.uri, fullscreenVideo?.uri)) {
                    VideoPlayer(
                        media = media,
                        isActive = page == pagerState.currentPage && fullscreenVideo == null,
                        rotationDegrees = rotationDegrees,
                        controlsBottomInset = VideoControlsBottomInset,
                        onSingleTap = { toggleControls() }
                    )
                }
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
                        showControls()
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
                onRotate = if (currentPhoto.mediaType == MediaType.Photo) onRotate else null,
                onVideoFullscreen = if (currentPhoto.mediaType == MediaType.Video) {
                    { fullscreenVideo = currentPhoto }
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

        fullscreenVideo?.let { media ->
            VideoFullscreenPlayer(
                media = media,
                strings = strings,
                onExit = { fullscreenVideo = null },
                modifier = Modifier.matchParentSize()
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

    BoxWithConstraints(
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
        val (mediaWidthPx, mediaHeightPx) = rotatedMediaSize(
            constraints.maxWidth,
            constraints.maxHeight,
            rotationDegrees
        )
        val mediaWidth = if (mediaWidthPx == constraints.maxWidth) maxWidth else maxHeight
        val mediaHeight = if (mediaHeightPx == constraints.maxHeight) maxHeight else maxWidth
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            modifier = Modifier
                .requiredSize(width = mediaWidth, height = mediaHeight)
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
@OptIn(UnstableApi::class)
fun VideoPlayer(
    media: MediaItemData,
    isActive: Boolean,
    rotationDegrees: Int,
    controlsBottomInset: Dp,
    onSingleTap: () -> Unit
) {
    val context = LocalContext.current
    var currentPositionMs by remember(media.uri) { mutableLongStateOf(0L) }
    var durationMs by remember(media.uri) { mutableLongStateOf(0L) }
    var isVideoPlaying by remember(media.uri) { mutableStateOf(false) }
    val player = remember(media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(media.uri))
            prepare()
        }
    }

    fun updatePlaybackState() {
        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it > 0L } ?: 0L
        isVideoPlaying = player.isPlaying
    }

    LaunchedEffect(isActive) {
        player.playWhenReady = isActive
        if (isActive) {
            player.play()
        } else {
            player.pause()
        }
        updatePlaybackState()
        while (isActive) {
            delay(500)
            updatePlaybackState()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = media.displayName }
            .pointerInput(media.uri) {
                detectTapGestures(onTap = { onSingleTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        val (mediaWidthPx, mediaHeightPx) = rotatedMediaSize(
            constraints.maxWidth,
            constraints.maxHeight,
            rotationDegrees
        )
        val mediaWidth = if (mediaWidthPx == constraints.maxWidth) maxWidth else maxHeight
        val mediaHeight = if (mediaHeightPx == constraints.maxHeight) maxHeight else maxWidth
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.useController = false
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier
                .requiredSize(width = mediaWidth, height = mediaHeight)
                .graphicsLayer(
                    rotationZ = rotationDegrees.toFloat()
                )
        )
        VideoPlaybackControls(
            isPlaying = isVideoPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onPlayPause = {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                updatePlaybackState()
            },
            onSeekTo = { positionMs ->
                player.seekTo(positionMs)
                updatePlaybackState()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp, end = 18.dp, bottom = controlsBottomInset + 12.dp)
        )
    }
}

@Composable
@OptIn(UnstableApi::class)
fun VideoFullscreenPlayer(
    media: MediaItemData,
    strings: LocalizedStrings,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    BackHandler(onBack = onExit)

    var currentPositionMs by remember(media.uri) { mutableLongStateOf(0L) }
    var durationMs by remember(media.uri) { mutableLongStateOf(0L) }
    var isVideoPlaying by remember(media.uri) { mutableStateOf(false) }
    var videoWidth by remember(media.uri) { mutableIntStateOf(0) }
    var videoHeight by remember(media.uri) { mutableIntStateOf(0) }
    var textureView by remember(media.uri) { mutableStateOf<TextureView?>(null) }
    val player = remember(media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(media.uri))
            playWhenReady = true
            prepare()
            play()
        }
    }

    fun updatePlaybackState() {
        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it > 0L } ?: 0L
        isVideoPlaying = player.isPlaying
    }

    LaunchedEffect(player) {
        while (true) {
            updatePlaybackState()
            delay(500)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(player, textureView) {
        val currentTextureView = textureView
        if (currentTextureView != null) {
            player.setVideoTextureView(currentTextureView)
        }
        onDispose {
            if (currentTextureView != null) {
                player.clearVideoTextureView(currentTextureView)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val rotationDegrees = videoFullscreenRotationDegrees(videoWidth, videoHeight)
        val (mediaWidthPx, mediaHeightPx) = fullscreenVideoSurfaceSize(
            containerWidthPx = constraints.maxWidth,
            containerHeightPx = constraints.maxHeight,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            rotationDegrees = rotationDegrees
        )
        val mediaWidth = with(density) { mediaWidthPx.toDp() }
        val mediaHeight = with(density) { mediaHeightPx.toDp() }

        AndroidView(
            factory = { viewContext ->
                TextureView(viewContext).also { view ->
                    textureView = view
                    player.setVideoTextureView(view)
                }
            },
            update = { view ->
                if (textureView !== view) {
                    textureView = view
                }
                player.setVideoTextureView(view)
            },
            modifier = Modifier
                .requiredSize(width = mediaWidth, height = mediaHeight)
                .graphicsLayer(rotationZ = rotationDegrees.toFloat())
        )

        FilledIconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.48f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.FullscreenExit, contentDescription = strings.exitFullscreen)
        }

        VideoPlaybackControls(
            isPlaying = isVideoPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onPlayPause = {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                updatePlaybackState()
            },
            onSeekTo = { positionMs ->
                player.seekTo(positionMs)
                updatePlaybackState()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 20.dp)
        )
    }
}

@Composable
fun VideoPlaybackControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text(
                text = formatPlaybackTime(currentPositionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            val sliderMax = durationMs.takeIf { it > 0L } ?: 1L
            Slider(
                value = currentPositionMs.coerceIn(0L, sliderMax).toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..sliderMax.toFloat(),
                enabled = durationMs > 0L,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatPlaybackTime(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1_000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun FullscreenTopBar(
    photo: PhotoItemData,
    currentIndex: Int,
    totalCount: Int,
    strings: LocalizedStrings,
    onRotate: (() -> Unit)?,
    onVideoFullscreen: (() -> Unit)?,
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
    discountedColor: Color = Color(0xFF2E7D32),
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

            is ExportStatus.Copying -> {
                val progressFraction = exportStatus.progressFraction
                if (progressFraction != null) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.widthIn(min = 320.dp, max = 520.dp)
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.widthIn(min = 320.dp, max = 520.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(strings.copyingSelectedFiles)
                exportProgressCountLabel(exportStatus)?.let { progressLabel ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(progressLabel, style = MaterialTheme.typography.bodyMedium)
                }
                exportStatus.currentFileName?.let { fileName ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    val selectedCount = photoCount + videoCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionMetricCard(
            label = strings.selected,
            value = selectedCount.toString(),
            supportingText = "${strings.photo}: $photoCount   ${strings.video}: $videoCount",
            modifier = Modifier.weight(0.9f)
        )
        TabletPriceBreakdownCard(
            photoCount = photoCount,
            videoCount = videoCount,
            photoOriginalPrice = photoOriginalPrice,
            photoPayablePrice = photoPayablePrice,
            videoOriginalPrice = videoOriginalPrice,
            videoPayablePrice = videoPayablePrice,
            strings = strings,
            modifier = Modifier.weight(1.2f)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.total,
                style = MaterialTheme.typography.labelLarge,
                color = LocalContentColor.current.copy(alpha = 0.68f)
            )
            Text(
                text = strings.price(totalPayablePrice),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = onReviewClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.widthIn(min = 220.dp)
            ) {
                Text(buttonText, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TabletPriceBreakdownCard(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    photoPayablePrice: Int,
    videoOriginalPrice: Int,
    videoPayablePrice: Int,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.priceBreakdown,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
            )
            TabletPriceLine(
                label = "${strings.photo}: $photoCount",
                originalPrice = photoOriginalPrice,
                payablePrice = photoPayablePrice,
                strings = strings
            )
            TabletPriceLine(
                label = "${strings.video}: $videoCount",
                originalPrice = videoOriginalPrice,
                payablePrice = videoPayablePrice,
                strings = strings
            )
        }
    }
}

@Composable
private fun TabletPriceLine(
    label: String,
    originalPrice: Int,
    payablePrice: Int,
    strings: LocalizedStrings
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (originalPrice > payablePrice) {
                Text(
                    text = strings.price(originalPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Text(
                text = strings.price(payablePrice),
                style = MaterialTheme.typography.titleMedium,
                color = if (originalPrice > payablePrice) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SelectionMetricCard(
    label: String,
    value: String,
    supportingText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
