package com.uzeyir.photoselector

import android.net.FakeUri
import android.net.Uri
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
    fun backFromConfirmationReturnsToReview() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Confirmation)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.Review, viewModel.currentScreen)
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
    fun backFromReviewViewerReturnsToReview() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.openLikedPhoto(photos[0].uri)

        val handled = viewModel.handleBack()

        assertEquals(true, handled)
        assertEquals(Screen.Review, viewModel.currentScreen)
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
        viewModel.navigateTo(Screen.Review)

        val continued = viewModel.goToConfirmationOrWarn()

        assertEquals(false, continued)
        assertEquals(Screen.Review, viewModel.currentScreen)
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
    fun confirmationWithNoLikedPhotosShowsWarningAndStaysOnReview() {
        val viewModel = PhotoViewModel()
        viewModel.navigateTo(Screen.Review)

        val continued = viewModel.goToConfirmationOrWarn()

        assertEquals(false, continued)
        assertEquals(Screen.Review, viewModel.currentScreen)
        assertEquals(UiMessage.SelectAtLeastOnePhoto, viewModel.selectionWarningMessage)
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
    fun unlikingLastPhotoInReviewViewerReturnsToReview() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(1)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)

        viewModel.openLikedPhoto(photos[0].uri)
        viewModel.toggleLike(photos[0].uri)

        assertEquals(Screen.Review, viewModel.currentScreen)
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

        val copiedBytes = copyDocumentBytes(
            input = ByteArrayInputStream(sourceBytes),
            output = destination,
            displayName = "IMG_0001.JPG"
        )

        assertEquals(4L, copiedBytes)
        assertArrayEquals(sourceBytes, destination.toByteArray())
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
        assertEquals(0f, ExportStatus.Copying(copiedFiles = 0, totalFiles = 4).progressFraction)
        assertEquals(0.5f, ExportStatus.Copying(copiedFiles = 2, totalFiles = 4).progressFraction)
        assertEquals(1f, ExportStatus.Copying(copiedFiles = 5, totalFiles = 4).progressFraction)
        assertEquals(null, ExportStatus.Copying(copiedFiles = 0, totalFiles = 0).progressFraction)
    }

    @Test
    fun exportProgressLabelShowsCopiedAndTotalCounts() {
        assertEquals("2/5", exportProgressCountLabel(ExportStatus.Copying(copiedFiles = 2, totalFiles = 5)))
        assertEquals(null, exportProgressCountLabel(ExportStatus.Copying()))
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
    fun inlineVideoPlayerIsHiddenForFullscreenVideoUriOnly() {
        val videoUri = FakeUri("videos/clip.mp4")

        assertEquals(true, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = null))
        assertEquals(false, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = videoUri))
        assertEquals(true, shouldRenderInlineVideoPlayer(videoUri, fullscreenVideoUri = FakeUri("videos/other.mp4")))
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
