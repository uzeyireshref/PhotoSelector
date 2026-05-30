package com.uzeyir.photoselector

import android.net.FakeUri
import android.net.Uri
import androidx.media3.common.Player
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoViewModelTest {
    @Test
    fun openPhotoAt_selectsRequestedPhoto() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(3))

        viewModel.openPhotoAt(1)

        assertEquals(Screen.PhotoDetail, viewModel.currentScreen)
        assertEquals(1, viewModel.selectedPhotoIndex)
        assertEquals("photo_2.jpg", viewModel.selectedPhoto?.displayName)
    }

    @Test
    fun nextAndPreviousPhotoStayInsideBounds() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(2))

        viewModel.openPhotoAt(0)
        viewModel.showPreviousPhoto()
        assertEquals(0, viewModel.selectedPhotoIndex)

        viewModel.showNextPhoto()
        assertEquals(1, viewModel.selectedPhotoIndex)

        viewModel.showNextPhoto()
        assertEquals(1, viewModel.selectedPhotoIndex)
    }

    @Test
    fun resetClearsSelectedPhoto() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(1))

        viewModel.openPhotoAt(0)
        viewModel.reset()

        assertEquals(Screen.FolderSelection, viewModel.currentScreen)
        assertEquals(-1, viewModel.selectedPhotoIndex)
        assertNull(viewModel.selectedPhoto)
    }

    @Test
    fun likingSelectedPhotoUpdatesPrice() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(1))
        viewModel.openPhotoAt(0)

        viewModel.toggleLike(viewModel.selectedPhoto!!.uri)

        assertEquals(1, viewModel.likedPhotos.size)
        assertEquals(300, viewModel.basePrice)
        assertEquals(300, viewModel.totalDisplayPrice)
        assertEquals(0, viewModel.discount)
    }

    @Test
    fun likedPhotoUriSetTracksTogglesForFastMembershipChecks() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(2)
        viewModel.setPhotos(photos)

        viewModel.toggleLike(photos[0].uri)
        viewModel.toggleLike(photos[1].uri)
        viewModel.toggleLike(photos[0].uri)

        assertEquals(setOf(photos[1].uri), viewModel.likedPhotoUriSet)
        assertEquals(listOf(photos[1].uri), viewModel.likedPhotos)
    }

    @Test
    fun appleDoubleMetadataFilesAreNotLoadedAsMedia() {
        val metadataPhoto = FolderDocumentData(
            uri = FakeUri("folder/._9S8A7592.JPG"),
            displayName = "._9S8A7592.JPG",
            mimeType = "image/jpeg"
        )
        val metadataVideo = FolderDocumentData(
            uri = FakeUri("folder/._5R7A4816.MP4"),
            displayName = "._5R7A4816.MP4",
            mimeType = "video/mp4"
        )

        assertEquals(null, metadataPhoto.toMediaItemOrNull())
        assertEquals(null, metadataVideo.toMediaItemOrNull())
    }

    @Test
    fun mediaLoadVersionChangesOnlyWhenMediaItemsAreReplaced() {
        val viewModel = PhotoViewModel()
        val initialVersion = viewModel.mediaLoadVersion
        val photos = testPhotos(1)

        viewModel.setMediaItems(photos)
        val loadedVersion = viewModel.mediaLoadVersion
        viewModel.toggleLike(photos[0].uri)

        assertEquals(initialVersion + 1, loadedVersion)
        assertEquals(loadedVersion, viewModel.mediaLoadVersion)
    }

    @Test
    fun priceAtThreePhotosHasNoDiscount() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(3)
        viewModel.setPhotos(photos)

        photos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(900, viewModel.basePrice)
        assertEquals(900, viewModel.totalDisplayPrice)
        assertEquals(0, viewModel.discount)
    }

    @Test
    fun priceAtFourPhotosStartsDiscountAtFivePercent() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(4)
        viewModel.setPhotos(photos)

        photos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(1200, viewModel.basePrice)
        assertEquals(1140, viewModel.totalDisplayPrice)
        assertEquals(60, viewModel.discount)
    }

    @Test
    fun priceAtTenPhotosUsesMaximumDiscount() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(10)
        viewModel.setPhotos(photos)

        photos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(3000, viewModel.basePrice)
        assertEquals(1950, viewModel.totalDisplayPrice)
        assertEquals(1050, viewModel.discount)
    }

    @Test
    fun priceAfterTenPhotosStaysAtMaximumDiscountedPrice() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(11)
        viewModel.setPhotos(photos)

        photos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(3300, viewModel.basePrice)
        assertEquals(1950, viewModel.totalDisplayPrice)
        assertEquals(1350, viewModel.discount)
    }

    @Test
    fun cappedPriceDoesNotIncreaseAfterElevenPhotos() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(15)
        viewModel.setPhotos(photos)

        photos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(4500, viewModel.basePrice)
        assertEquals(1950, viewModel.totalDisplayPrice)
        assertEquals(2550, viewModel.discount)
    }

    @Test
    fun videoPriceAtThreeVideosHasNoDiscount() {
        val viewModel = PhotoViewModel()
        val videos = testVideos(3)
        viewModel.setMediaItems(videos)

        videos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(3, viewModel.selectedVideoCount)
        assertEquals(3000, viewModel.videoBasePrice)
        assertEquals(3000, viewModel.videoDisplayPrice)
        assertEquals(0, viewModel.videoDiscount)
    }

    @Test
    fun videoPriceAtFourVideosStartsDiscountAtFivePercent() {
        val viewModel = PhotoViewModel()
        val videos = testVideos(4)
        viewModel.setMediaItems(videos)

        videos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(4000, viewModel.videoBasePrice)
        assertEquals(3800, viewModel.videoDisplayPrice)
        assertEquals(200, viewModel.videoDiscount)
    }

    @Test
    fun videoPriceAfterTenVideosStaysAtMaximumDiscountedPrice() {
        val viewModel = PhotoViewModel()
        val videos = testVideos(12)
        viewModel.setMediaItems(videos)

        videos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(12000, viewModel.videoBasePrice)
        assertEquals(6500, viewModel.videoDisplayPrice)
        assertEquals(5500, viewModel.videoDiscount)
    }

    @Test
    fun photoAndVideoPricesAreDiscountedSeparatelyThenAdded() {
        val viewModel = PhotoViewModel()
        val media = testPhotos(4) + testVideos(4)
        viewModel.setMediaItems(media)

        media.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(1200, viewModel.photoBasePrice)
        assertEquals(1140, viewModel.photoDisplayPrice)
        assertEquals(4000, viewModel.videoBasePrice)
        assertEquals(3800, viewModel.videoDisplayPrice)
        assertEquals(4940, viewModel.totalDisplayPrice)
    }

    @Test
    fun backFromGalleryReturnsToFolderSelection() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Gallery)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.FolderSelection, viewModel.currentScreen)
    }

    @Test
    fun backFromFolderSelectionIsNotHandled() {
        val viewModel = PhotoViewModel()

        val handled = viewModel.handleBack()

        assertEquals(false, handled)
        assertEquals(Screen.FolderSelection, viewModel.currentScreen)
    }

    @Test
    fun backFromReviewReturnsToGallery() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Review)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
    }

    @Test
    fun backFromConfirmationReturnsToGallery() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Confirmation)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
    }

    @Test
    fun backFromSuccessfulConfirmationReturnsHomeAndClearsSession() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.navigateTo(Screen.Confirmation)
        viewModel.replaceExportStatus(ExportStatus.Success(folderName = "done", copiedFiles = 1))

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.FolderSelection, viewModel.currentScreen)
        assertEquals(emptyList<MediaItemData>(), viewModel.photos)
        assertEquals(0, viewModel.selectedPhotoCount)
    }

    @Test
    fun backFromGalleryViewerReturnsToGallery() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(1))
        viewModel.openPhotoAt(0)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
    }

    @Test
    fun backFromFavoritesViewerReturnsToGallery() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.openLikedPhoto(photos[0].uri)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
    }

    @Test
    fun reviewSelectionWithNoLikedPhotosShowsWarningAndStaysOnCurrentScreen() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Gallery)

        val continued = viewModel.goToReviewOrWarn()

        assertEquals(false, continued)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(UiMessage.SelectAtLeastOnePhoto, viewModel.selectionWarningMessage)
    }

    @Test
    fun reviewSelectionIgnoresStaleLikedUrisFromPreviousFolder() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(listOf(testPhoto("current.jpg")))
        viewModel.likedPhotos.add(FakeUri("previous-folder/old.jpg"))
        viewModel.navigateTo(Screen.Gallery)

        val continued = viewModel.goToReviewOrWarn()

        assertEquals(false, continued)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(UiMessage.SelectAtLeastOnePhoto, viewModel.selectionWarningMessage)
    }

    @Test
    fun confirmationIgnoresStaleLikedUrisFromPreviousFolder() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(listOf(testPhoto("current.jpg")))
        viewModel.likedPhotos.add(FakeUri("previous-folder/old.jpg"))
        viewModel.navigateTo(Screen.Gallery)

        val continued = viewModel.goToConfirmationOrWarn()

        assertEquals(false, continued)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(UiMessage.SelectAtLeastOnePhoto, viewModel.selectionWarningMessage)
    }

    @Test
    fun reviewSelectionWithLikedPhotoNavigatesToReviewAndClearsWarning() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.navigateTo(Screen.Gallery)

        val continued = viewModel.goToReviewOrWarn()

        assertEquals(true, continued)
        assertEquals(Screen.Review, viewModel.currentScreen)
        assertNull(viewModel.selectionWarningMessage)
    }

    @Test
    fun reviewSelectionFromViewerReturnsToGalleryFavoritesTab() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.openPhoto(photos[0].uri)

        val continued = viewModel.showFavoritesFromViewerOrWarn()

        assertEquals(true, continued)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(GalleryTab.Favorites, viewModel.galleryTab)
        assertNull(viewModel.selectionWarningMessage)
    }

    @Test
    fun galleryTabCanBeSelectedFromViewModel() {
        val viewModel = PhotoViewModel()

        viewModel.selectGalleryTab(GalleryTab.Favorites)

        assertEquals(GalleryTab.Favorites, viewModel.galleryTab)
    }

    @Test
    fun requestHomeFromGalleryShowsPendingResetConfirmationAndKeepsScreen() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(1))
        viewModel.navigateTo(Screen.Gallery)

        val handled = viewModel.requestReturnToFolderSelection()

        assertEquals(true, handled)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(true, viewModel.pendingReturnToFolderConfirmation)
    }

    @Test
    fun cancelHomeResetConfirmationKeepsSelectionAndCurrentScreen() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.navigateTo(Screen.Gallery)
        viewModel.requestReturnToFolderSelection()

        viewModel.cancelReturnToFolderSelection()

        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(false, viewModel.pendingReturnToFolderConfirmation)
        assertEquals(1, viewModel.selectedPhotoCount)
    }

    @Test
    fun confirmHomeResetReturnsToFolderSelectionAndClearsSelection() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.navigateTo(Screen.Gallery)
        viewModel.requestReturnToFolderSelection()

        viewModel.confirmReturnToFolderSelection()

        assertEquals(Screen.FolderSelection, viewModel.currentScreen)
        assertEquals(false, viewModel.pendingReturnToFolderConfirmation)
        assertEquals(0, viewModel.selectedPhotoCount)
        assertEquals(emptyList<MediaItemData>(), viewModel.photos)
    }

    @Test
    fun confirmationWithNoLikedPhotosShowsWarningAndStaysOnGallery() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Gallery)

        val continued = viewModel.goToConfirmationOrWarn()

        assertEquals(false, continued)
        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(UiMessage.SelectAtLeastOnePhoto, viewModel.selectionWarningMessage)
    }

    @Test
    fun confirmationWithLikedPhotoNavigatesFromGalleryToConfirmationAndClearsWarning() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.navigateTo(Screen.Gallery)

        val continued = viewModel.goToConfirmationOrWarn()

        assertEquals(true, continued)
        assertEquals(Screen.Confirmation, viewModel.currentScreen)
        assertNull(viewModel.selectionWarningMessage)
    }

    @Test
    fun openLikedPhoto_usesOnlyLikedPhotosInViewer() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(3)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.toggleLike(photos[2].uri)

        viewModel.openLikedPhoto(photos[2].uri)

        assertEquals(Screen.PhotoDetail, viewModel.currentScreen)
        assertEquals(PhotoViewerSource.Review, viewModel.viewerSource)
        assertEquals(0, viewModel.selectedPhotoIndex)
        assertEquals(listOf("photo_3.jpg", "photo_1.jpg"), viewModel.viewerPhotos.map { it.displayName })
        assertEquals("photo_3.jpg", viewModel.selectedPhoto?.displayName)
    }

    @Test
    fun reviewViewerNavigationStaysWithinLikedPhotos() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(3)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.toggleLike(photos[2].uri)

        viewModel.openLikedPhoto(photos[0].uri)
        viewModel.showPreviousPhoto()

        assertEquals(0, viewModel.selectedPhotoIndex)
        assertEquals("photo_3.jpg", viewModel.selectedPhoto?.displayName)
    }

    @Test
    fun unlikingLastPhotoInFavoritesViewerReturnsToGallery() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)

        viewModel.openLikedPhoto(photos[0].uri)
        viewModel.toggleLike(photos[0].uri)

        assertEquals(Screen.Gallery, viewModel.currentScreen)
        assertEquals(-1, viewModel.selectedPhotoIndex)
        assertNull(viewModel.selectedPhoto)
    }

    @Test
    fun exportSummaryCountsLikedJpgsAndMatchingRawFiles() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(3)
        viewModel.setPhotos(photos)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("photo_1.CR3"),
                testDocument("photo_1.NEF"),
                testDocument("photo_2.CR3"),
                testDocument("photo_3.txt")
            )
        )
        viewModel.toggleLike(photos[0].uri)
        viewModel.toggleLike(photos[2].uri)

        val summary = viewModel.exportSummary

        assertEquals(2, summary.selectedJpgCount)
        assertEquals(2, summary.matchedRawCount)
        assertEquals(4, summary.totalFileCount)
    }

    @Test
    fun copyDocumentBytesCopiesNonEmptyStreams() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4)
        val destination = ByteArrayOutputStream()
        val progress = mutableListOf<Long>()

        val copiedBytes = copyDocumentBytes(
            input = ByteArrayInputStream(sourceBytes),
            output = destination,
            displayName = "IMG_0001.JPG",
            onProgress = { copied, _ -> progress.add(copied) }
        )

        assertEquals(4L, copiedBytes)
        assertArrayEquals(sourceBytes, destination.toByteArray())
        assertEquals(4L, progress.last())
    }

    @Test
    fun copyDocumentBytesRejectsZeroByteCopies() {
        val error = assertThrows(IllegalStateException::class.java) {
            copyDocumentBytes(
                input = ByteArrayInputStream(ByteArray(0)),
                output = ByteArrayOutputStream(),
                displayName = "IMG_0001.JPG"
            )
        }

        assertEquals(UiMessage.CopyVerificationFailed.name, error.message)
    }

    @Test
    fun exportCannotStartWhileCopying() {
        assertEquals(false, shouldBeginExport(ExportStatus.Copying()))
        assertEquals(true, shouldBeginExport(ExportStatus.Idle))
        assertEquals(true, shouldBeginExport(ExportStatus.Success(folderName = "done", copiedFiles = 1)))
    }

    @Test
    fun exportProgressFractionIsBoundedByCopiedAndTotalFiles() {
        assertEquals(0f, ExportStatus.Copying(copiedBytes = 0, totalBytes = 400).progressFraction)
        assertEquals(0.5f, ExportStatus.Copying(copiedBytes = 200, totalBytes = 400).progressFraction)
        assertEquals(1f, ExportStatus.Copying(copiedBytes = 500, totalBytes = 400).progressFraction)
        assertEquals(null, ExportStatus.Copying(copiedBytes = 0, totalBytes = null).progressFraction)
    }

    @Test
    fun fileProgressFractionUsesByteCountsWhenSizeIsKnown() {
        val file = ExportFileProgress(
            fileName = "video.mp4",
            state = ExportFileState.Copying,
            bytesCopied = 512,
            totalBytes = 1024
        )

        assertEquals(0.5f, file.progressFraction)
    }

    @Test
    fun exportProgressLabelShowsCopiedAndTotalCounts() {
        assertEquals("2/5", exportProgressCountLabel(ExportStatus.Copying(copiedFiles = 2, totalFiles = 5)))
        assertEquals(null, exportProgressCountLabel(ExportStatus.Copying()))
    }

    @Test
    fun exportProgressTracksSuccessfulFiles() {
        val status = ExportStatus.Copying(
            copiedFiles = 2,
            totalFiles = 3,
            currentFileName = "third.jpg",
            files = listOf(
                ExportFileProgress("first.jpg", ExportFileState.Copied),
                ExportFileProgress("second.jpg", ExportFileState.Copied),
                ExportFileProgress("third.jpg", ExportFileState.Copying)
            )
        )

        assertEquals(listOf("first.jpg", "second.jpg"), copiedExportFileNames(status))
    }

    @Test
    fun mediaItemsKeepKnownDocumentSizeForExportVerification() {
        val document = FolderDocumentData(
            uri = FakeUri("folder/photo.jpg"),
            displayName = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1234L
        )

        assertEquals(1234L, document.toMediaItemOrNull()?.sizeBytes)
    }

    @Test
    fun failedExportCleanupDeletesCreatedDocumentsAndFolderInReverseOrder() {
        val firstFile = FakeUri("export/photo_1.jpg")
        val secondFile = FakeUri("export/photo_2.jpg")
        val exportFolder = FakeUri("export")
        val deleted = mutableListOf<String>()

        cleanupCreatedExportDocuments(
            createdFileUris = listOf(firstFile, secondFile),
            exportFolderUri = exportFolder
        ) { uri ->
            deleted.add(uri.toString())
            true
        }

        assertEquals(
            listOf(
                secondFile.toString(),
                firstFile.toString(),
                exportFolder.toString()
            ),
            deleted
        )
    }

    @Test
    fun exportSummaryCountsSelectedVideosWithoutRawMatches() {
        val viewModel = PhotoViewModel()
        val media = listOf(
            testPhoto("clip_1.jpg"),
            testVideo("clip_1.mp4"),
            testVideo("movie.mov")
        )
        viewModel.setMediaItems(media)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("clip_1.CR3"),
                testDocument("movie.CR3")
            )
        )
        media.forEach { viewModel.toggleLike(it.uri) }

        val summary = viewModel.exportSummary

        assertEquals(1, summary.selectedJpgCount)
        assertEquals(1, summary.matchedRawCount)
        assertEquals(2, summary.selectedVideoCount)
        assertEquals(4, summary.totalFileCount)
    }

    @Test
    fun matchingRawFilesAreCaseInsensitiveAndLimitedToSameBaseName() {
        val viewModel = PhotoViewModel()
        val photos = listOf(
            testPhoto("IMG_0001.JPG"),
            testPhoto("IMG_0002.JPG")
        )
        viewModel.setPhotos(photos)
        viewModel.setFolderDocuments(
            listOf(
                testDocument("img_0001.arw"),
                testDocument("IMG_0002.CR2"),
                testDocument("IMG_00010.CR3")
            )
        )
        viewModel.toggleLike(photos[0].uri)

        val matches = viewModel.matchingRawFilesFor(photos[0])

        assertEquals(listOf("img_0001.arw"), matches.map { it.displayName })
    }

    @Test
    fun mediaClassificationIncludesJpgAndVideosOnly() {
        val documents = listOf(
            testDocument("IMG_0001.JPG", "image/jpeg", lastModified = 10),
            testDocument("clip.MP4", "", lastModified = 20),
            testDocument("movie.mov", "application/octet-stream", lastModified = 30),
            testDocument("IMG_0001.CR3", "application/octet-stream", lastModified = 40),
            testDocument("notes.txt", "text/plain", lastModified = 50)
        )

        val media = documents.mapNotNull { it.toMediaItemOrNull() }

        assertEquals(
            listOf(MediaType.Photo, MediaType.Video, MediaType.Video),
            media.map { it.mediaType }
        )
        assertEquals(
            listOf("IMG_0001.JPG", "clip.MP4", "movie.mov"),
            media.map { it.displayName }
        )
    }

    @Test
    fun galleryMediaIsSortedByNewestLastModifiedFirst() {
        val media = listOf(
            testPhoto("old.jpg", lastModified = 100),
            testVideo("new.mp4", lastModified = 300),
            testPhoto("middle.jpg", lastModified = 200)
        )

        val sorted = sortMediaItemsForGallery(media)

        assertEquals(listOf("new.mp4", "middle.jpg", "old.jpg"), sorted.map { it.displayName })
    }

    @Test
    fun galleryMediaSortsVideosFirstThenEachGroupByNewest() {
        val media = listOf(
            testPhoto("newest-photo.jpg", lastModified = 500),
            testVideo("older-video.mp4", lastModified = 200),
            testPhoto("older-photo.jpg", lastModified = 100),
            testVideo("newest-video.mp4", lastModified = 300)
        )

        val sorted = sortMediaItemsForGallery(media)

        assertEquals(
            listOf("newest-video.mp4", "older-video.mp4", "newest-photo.jpg", "older-photo.jpg"),
            sorted.map { it.displayName }
        )
    }

    @Test
    fun galleryMediaWithNoLastModifiedFallsBackToReverseDisplayName() {
        val media = listOf(
            testPhoto("IMG_0001.JPG", lastModified = 0),
            testPhoto("IMG_0003.JPG", lastModified = 0),
            testVideo("IMG_0002.MP4", lastModified = 0)
        )

        val sorted = sortMediaItemsForGallery(media)

        assertEquals(listOf("IMG_0002.MP4", "IMG_0003.JPG", "IMG_0001.JPG"), sorted.map { it.displayName })
    }

    @Test
    fun exportFolderTimestampUsesHourMinuteDayMonthFormat() {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MAY, 29, 19, 26, 0)
        }

        val timestamp = formatExportFolderTimestamp(calendar.time)

        assertEquals("19.26_29-05", timestamp)
    }

    @Test
    fun exportFolderNameUsesOnlyTimestampWithoutPrefix() {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MAY, 29, 19, 26, 0)
        }

        val folderName = exportFolderName(calendar.time)

        assertEquals("19.26_29-05", folderName)
    }

    @Test
    fun rotateSelectedMediaCyclesByNinetyDegrees() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.openPhotoAt(0)

        viewModel.rotateSelectedMedia()
        assertEquals(90, viewModel.rotationFor(photos[0].uri))

        viewModel.rotateSelectedMedia()
        assertEquals(180, viewModel.rotationFor(photos[0].uri))

        viewModel.rotateSelectedMedia()
        assertEquals(270, viewModel.rotationFor(photos[0].uri))

        viewModel.rotateSelectedMedia()
        assertEquals(0, viewModel.rotationFor(photos[0].uri))
    }

    @Test
    fun rotatedMediaSizeSwapsContainerForQuarterTurns() {
        assertEquals(1080 to 1920, rotatedMediaSize(1080, 1920, 0))
        assertEquals(1080 to 1920, rotatedMediaSize(1080, 1920, 180))
        assertEquals(1920 to 1080, rotatedMediaSize(1080, 1920, 90))
        assertEquals(1920 to 1080, rotatedMediaSize(1080, 1920, 270))
    }

    @Test
    fun rotatedMediaSizeLeavesInvalidContainersUnchanged() {
        assertEquals(0 to 1000, rotatedMediaSize(0, 1000, 90))
        assertEquals(1000 to 0, rotatedMediaSize(1000, 0, 90))
    }

    @Test
    fun videoFullscreenRotationTurnsOnlyLandscapeVideoSideways() {
        assertEquals(90, videoFullscreenRotationDegrees(videoWidth = 1920, videoHeight = 1080))
        assertEquals(0, videoFullscreenRotationDegrees(videoWidth = 1080, videoHeight = 1920))
        assertEquals(0, videoFullscreenRotationDegrees(videoWidth = 0, videoHeight = 0))
    }

    @Test
    fun fullscreenVideoSurfaceSizePreservesLandscapeAspectAfterRotation() {
        assertEquals(
            2113 to 1189,
            fullscreenVideoSurfaceSize(
                containerWidthPx = 1080,
                containerHeightPx = 2113,
                videoWidth = 1920,
                videoHeight = 1080,
                rotationDegrees = 90
            )
        )
    }

    @Test
    fun videoControlsInsetLeavesRoomForTabletPriceBar() {
        assertEquals(112, videoControlsBottomInsetDp(widthDp = 411, controlsVisible = true))
        assertEquals(172, videoControlsBottomInsetDp(widthDp = 866, controlsVisible = true))
        assertEquals(24, videoControlsBottomInsetDp(widthDp = 866, controlsVisible = false))
    }

    @Test
    fun videoSurfaceTypesPreferSurfaceViewInlineAndTextureViewFullscreen() {
        assertEquals(VideoSurfaceKind.SurfaceView, inlineVideoSurfaceKind())
        assertEquals(VideoSurfaceKind.TextureView, fullscreenVideoSurfaceKind())
        assertEquals(AspectResizeMode.Fit, inlineVideoResizeMode())
        assertEquals(true, fullscreenVideoUsesComposeSurface())
    }

    @Test
    fun inlineVideoPlayerIsHiddenForFullscreenVideoUriOnly() {
        val videoUri = FakeUri("videos/clip.mp4")

        assertEquals(false, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = null, activeInlineVideoUri = null))
        assertEquals(false, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = null, activeInlineVideoUri = videoUri))
        assertEquals(false, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = videoUri, activeInlineVideoUri = videoUri))
        assertEquals(false, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = null, activeInlineVideoUri = FakeUri("videos/other.mp4")))
    }

    @Test
    fun sharedVideoSessionOpensFullscreenImmediately() {
        val videoUri = FakeUri("videos/clip.mp4")
        val initial = VideoPlaybackSession()

        val fullscreen = initial.playFullscreen(videoUri)
        val exited = fullscreen.exitFullscreen()

        assertEquals(videoUri, fullscreen.activeUri)
        assertEquals(videoUri, fullscreen.fullscreenUri)
        assertEquals(null, exited.activeUri)
        assertEquals(null, exited.fullscreenUri)
    }

    @Test
    fun sharedVideoSessionClearsWhenPageChanges() {
        val first = FakeUri("videos/first.mp4")
        val second = FakeUri("videos/second.mp4")
        val session = VideoPlaybackSession(activeUri = first, fullscreenUri = first)

        val cleared = session.clearIfDifferentPage(second)

        assertEquals(null, cleared.activeUri)
        assertEquals(null, cleared.fullscreenUri)
    }

    @Test
    fun videoFullscreenExitsWhenPlaybackEnds() {
        assertEquals(false, shouldExitFullscreenForPlaybackState(Player.STATE_READY))
        assertEquals(false, shouldExitFullscreenForPlaybackState(Player.STATE_BUFFERING))
        assertEquals(true, shouldExitFullscreenForPlaybackState(Player.STATE_ENDED))
    }

    @Test
    fun videoFullscreenShowsLoadingOnlyBeforeReady() {
        assertEquals(true, shouldShowFullscreenVideoLoading(Player.STATE_IDLE))
        assertEquals(true, shouldShowFullscreenVideoLoading(Player.STATE_BUFFERING))
        assertEquals(false, shouldShowFullscreenVideoLoading(Player.STATE_READY))
        assertEquals(false, shouldShowFullscreenVideoLoading(Player.STATE_ENDED))
    }

    @Test
    fun videoPlaybackStateSelectsActiveAndFullscreenMedia() {
        val first = testVideo("first.mp4")
        val second = testVideo("second.mp4")
        val session = VideoPlaybackSession().playFullscreen(second.uri)

        val state = videoPlaybackStateFor(listOf(first, second), session)

        assertEquals(second, state.activeMedia)
        assertEquals(second, state.fullscreenMedia)
        assertEquals(true, state.isFullscreen)
    }

    @Test
    fun videoPlaybackStateIgnoresMissingSessionMedia() {
        val media = testVideo("clip.mp4")
        val session = VideoPlaybackSession().playFullscreen(FakeUri("missing.mp4"))

        val state = videoPlaybackStateFor(listOf(media), session)

        assertEquals(null, state.activeMedia)
        assertEquals(null, state.fullscreenMedia)
        assertEquals(false, state.isFullscreen)
    }

    @Test
    fun videoPosterRequestSizeCapsLargeSurfacesWhileKeepingAspect() {
        assertEquals(640 to 360, videoPosterRequestSizePx(containerWidthPx = 1920, containerHeightPx = 1080))
        assertEquals(360 to 640, videoPosterRequestSizePx(containerWidthPx = 1080, containerHeightPx = 1920))
    }

    @Test
    fun videoPosterRequestSizeDoesNotUpscaleSmallOrInvalidSurfaces() {
        assertEquals(320 to 180, videoPosterRequestSizePx(containerWidthPx = 320, containerHeightPx = 180))
        assertEquals(1 to 1, videoPosterRequestSizePx(containerWidthPx = 0, containerHeightPx = 180))
        assertEquals(1 to 1, videoPosterRequestSizePx(containerWidthPx = 320, containerHeightPx = 0))
    }

    @Test
    fun galleryGridUsesMoreColumnsOnTabletWidth() {
        assertEquals(3, galleryColumnCountForWidthDp(411))
        assertEquals(3, galleryColumnCountForWidthDp(599))
        assertEquals(4, galleryColumnCountForWidthDp(600))
        assertEquals(4, galleryColumnCountForWidthDp(866))
    }

    @Test
    fun thumbnailRequestSizeFollowsGridCellWidth() {
        assertEquals(362, thumbnailRequestSizePx(widthDp = 411, density = 2.75f))
        assertEquals(452, thumbnailRequestSizePx(widthDp = 866, density = 2.125f))
    }

    @Test
    fun setMediaItemsClearsSessionRotation() {
        val viewModel = PhotoViewModel()
        val firstPhoto = testPhoto("first.jpg")
        viewModel.setMediaItems(listOf(firstPhoto))
        viewModel.openPhotoAt(0)
        viewModel.rotateSelectedMedia()

        viewModel.setMediaItems(listOf(testPhoto("second.jpg")))

        assertEquals(0, viewModel.rotationFor(firstPhoto.uri))
    }

    @Test
    fun setMediaItemsClearsPreviousFolderSelection() {
        val viewModel = PhotoViewModel()
        val firstPhoto = testPhoto("first.jpg")
        viewModel.setMediaItems(listOf(firstPhoto))
        viewModel.toggleLike(firstPhoto.uri)

        viewModel.setMediaItems(listOf(testPhoto("second.jpg")))

        assertEquals(emptyList<Uri>(), viewModel.likedPhotos)
        assertEquals(0, viewModel.selectedPhotoCount)
    }

    @Test
    fun lastFolderStoreResolvesOnlyWhenReadAndWritePermissionsPersisted() {
        val store = FakeLastFolderStore()
        val folderUri = "content://tree/sdcard"
        store.save(folderUri)

        assertEquals(
            folderUri,
            store.resolveAvailableFolder(
                persistedReadUris = setOf(folderUri),
                persistedWriteUris = setOf(folderUri)
            )
        )
        assertEquals(
            null,
            store.resolveAvailableFolder(
                persistedReadUris = setOf(folderUri),
                persistedWriteUris = emptySet()
            )
        )
    }

    @Test
    fun lastFolderStoreClearsUnavailableFolder() {
        val store = FakeLastFolderStore()
        store.save("content://tree/missing")

        val resolved = store.resolveAvailableFolder(
            persistedReadUris = emptySet(),
            persistedWriteUris = emptySet()
        )

        assertEquals(null, resolved)
        assertEquals(null, store.savedUri)
    }

    @Test
    fun sdCardDcimInitialUriIsBuiltFromStorageVolumeRootUri() {
        assertEquals(
            "content://com.android.externalstorage.documents/document/1234-5678%3ADCIM",
            sdCardDcimInitialUriStringFromRootUri(
                "content://com.android.externalstorage.documents/root/1234-5678"
            )
        )
    }

    @Test
    fun sdCardDcimInitialUriFallsBackForUnsupportedRootUri() {
        assertEquals(
            null,
            sdCardDcimInitialUriStringFromRootUri("content://other.provider/root/1234-5678")
        )
    }

    private fun testPhotos(count: Int): List<MediaItemData> =
        (1..count).map { index ->
            testPhoto("photo_$index.jpg")
        }

    private fun testVideos(count: Int): List<MediaItemData> =
        (1..count).map { index ->
            testVideo("video_$index.mp4")
        }

    private fun testPhoto(displayName: String, lastModified: Long = 0): MediaItemData =
        MediaItemData(
            uri = FakeUri(displayName),
            displayName = displayName,
            mimeType = "image/jpeg",
            mediaType = MediaType.Photo,
            lastModified = lastModified
        )

    private fun testVideo(displayName: String, lastModified: Long = 0): MediaItemData =
        MediaItemData(
            uri = FakeUri(displayName),
            displayName = displayName,
            mimeType = "video/mp4",
            mediaType = MediaType.Video,
            lastModified = lastModified
        )

    private fun testDocument(
        displayName: String,
        mimeType: String = "application/octet-stream",
        lastModified: Long = 0
    ): FolderDocumentData =
        FolderDocumentData(
            uri = FakeUri(displayName),
            displayName = displayName,
            mimeType = mimeType,
            lastModified = lastModified
        )

    private class FakeLastFolderStore : LastFolderStore {
        var savedUri: String? = null

        override fun save(folderUri: String) {
            savedUri = folderUri
        }

        override fun clear() {
            savedUri = null
        }

        override fun resolveAvailableFolder(persistedReadUris: Set<String>, persistedWriteUris: Set<String>): String? {
            val current = savedUri
            return if (current != null && current in persistedReadUris && current in persistedWriteUris) {
                current
            } else {
                clear()
                null
            }
        }
    }
}
