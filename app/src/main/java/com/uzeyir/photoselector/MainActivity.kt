package com.uzeyir.photoselector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uzeyir.photoselector.ui.theme.PhotoSelectorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val galleryTab = viewModel.galleryTab
    val pendingReturnToFolderConfirmation = viewModel.pendingReturnToFolderConfirmation
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
    var includeRawFiles by remember { mutableStateOf(true) }
    val strings = UiText.strings(language)
    var sdCardOptions by remember { mutableStateOf<List<StorageVolume>>(emptyList()) }
    val galleryGridState = rememberLazyGridState()
    val galleryContentKey = viewModel.mediaLoadVersion

    BackHandler(enabled = currentScreen != Screen.FolderSelection) {
        when (currentScreen) {
            Screen.PhotoDetail, Screen.Confirmation -> viewModel.handleBack()
            Screen.Gallery -> viewModel.requestReturnToFolderSelection()
            Screen.FolderSelection -> Unit
        }
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

    if (pendingReturnToFolderConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelReturnToFolderSelection() },
            title = { Text(strings.returnHomeWarningTitle) },
            text = { Text(strings.returnHomeWarningMessage) },
            confirmButton = {
                Button(onClick = { viewModel.confirmReturnToFolderSelection() }) {
                    Text(strings.returnHome)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelReturnToFolderSelection() }) {
                    Text(strings.stayHere)
                }
            }
        )
    }

    Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentScreen == Screen.Gallery) {
                    BottomPriceBar(
                        photoCount = viewModel.selectedPhotoCount,
                        videoCount = viewModel.selectedVideoCount,
                        photoOriginalPrice = viewModel.photoBasePrice,
                        photoPayablePrice = viewModel.photoDisplayPrice,
                        videoOriginalPrice = viewModel.videoBasePrice,
                        videoPayablePrice = viewModel.videoDisplayPrice,
                        totalPayablePrice = viewModel.totalDisplayPrice,
                        onReviewClick = {
                            includeRawFiles = true
                            viewModel.goToConfirmationOrWarn()
                        },
                        buttonText = strings.confirmSelection,
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
                    likedMediaItems = likedMediaItems,
                    likedPhotoUris = likedPhotoUriSet,
                    selectedTab = galleryTab,
                    onTabSelected = { tab -> viewModel.selectGalleryTab(tab) },
                    strings = strings,
                    gridState = galleryGridState,
                    onPhotoClick = { uri -> viewModel.openPhoto(uri) },
                    onFavoritePhotoClick = { uri -> viewModel.openPhoto(uri) },
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
                    onBack = { viewModel.navigateTo(Screen.Gallery) },
                    onPhotoSelected = { index -> viewModel.openPhotoAt(index) },
                    rotationFor = { uri -> viewModel.rotationFor(uri) },
                    onRotate = { viewModel.rotateSelectedMedia() },
                    onLikeToggle = { uri -> viewModel.toggleLike(uri) },
                    onReviewClick = { viewModel.showFavoritesFromViewerOrWarn() }
                )
                Screen.Confirmation -> ConfirmationScreen(
                    summary = exportSummary,
                    exportStatus = exportStatus,
                    includeRawFiles = includeRawFiles,
                    photoOriginalPrice = viewModel.photoBasePrice,
                    photoPayablePrice = viewModel.photoDisplayPrice,
                    videoOriginalPrice = viewModel.videoBasePrice,
                    videoPayablePrice = viewModel.videoDisplayPrice,
                    totalDiscount = viewModel.discount,
                    totalPayablePrice = viewModel.totalDisplayPrice,
                    strings = strings,
                    onBack = { viewModel.navigateTo(Screen.Gallery) },
                    onIncludeRawFilesChange = { includeRawFiles = it },
                    onShareWhatsApp = {
                        val files = viewModel.selectedTransferFiles(includeRawFiles = includeRawFiles)
                        if (files.isNotEmpty()) {
                            runCatching {
                                context.startActivity(whatsAppDocumentShareIntent(files))
                            }.onFailure {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(strings.whatsAppShareFailed)
                                }
                            }
                        }
                    },
                    onConfirmExport = {
                        coroutineScope.launch {
                            viewModel.exportSelection(
                                contentResolver = context.contentResolver,
                                includeRawFiles = includeRawFiles
                            )
                        }
                    },
                    onReturnHome = { viewModel.returnHomeAfterExport() },
                    onReturnToGallery = { viewModel.returnToGalleryAfterExport() }
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

