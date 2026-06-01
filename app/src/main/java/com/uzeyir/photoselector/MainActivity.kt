package com.uzeyir.photoselector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val updateStatus = viewModel.updateStatus
    val language = viewModel.language
    val includeRawFiles = viewModel.includeRawFiles
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
            cancellationSafeRunCatching {
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
            val permissionGranted = cancellationSafeRunCatching {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.onFailure {
                viewModel.warn(UiMessage.FolderLoadFailed)
            }.isSuccess
            if (!permissionGranted) return

            coroutineScope.launch {
                cancellationSafeRunCatching {
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
                            viewModel.chooseIncludeRawFiles(true)
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
                    onLanguageSelected = { selectedLanguage -> viewModel.selectLanguage(selectedLanguage) },
                    strings = strings,
                    onCheckUpdate = {
                        coroutineScope.launch {
                            viewModel.replaceUpdateStatus(AppUpdateStatus.Checking)
                            cancellationSafeRunCatching {
                                val latestRelease = updateRepository.fetchLatestRelease()
                                when (val decision = UpdatePolicy.decide(BuildConfig.VERSION_CODE, latestRelease)) {
                                    UpdateDecision.UpToDate -> {
                                        viewModel.replaceUpdateStatus(AppUpdateStatus.UpToDate)
                                    }
                                    is UpdateDecision.UpdateAvailable -> {
                                        viewModel.replaceUpdateStatus(AppUpdateStatus.Available(decision.updateInfo.versionName))
                                        delay(450)
                                        viewModel.replaceUpdateStatus(AppUpdateStatus.Downloading)
                                        val apkFile = updateRepository.downloadApk(context, decision.updateInfo)
                                        viewModel.replaceUpdateStatus(AppUpdateStatus.ReadyToInstall)
                                        ApkInstaller.openInstaller(context, apkFile)
                                    }
                                }
                            }.onFailure {
                                viewModel.replaceUpdateStatus(AppUpdateStatus.Error)
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
                    onIncludeRawFilesChange = { viewModel.chooseIncludeRawFiles(it) },
                    onShareWhatsApp = {
                        val files = viewModel.selectedTransferFiles(includeRawFiles = includeRawFiles)
                        if (files.isNotEmpty()) {
                            cancellationSafeRunCatching {
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

