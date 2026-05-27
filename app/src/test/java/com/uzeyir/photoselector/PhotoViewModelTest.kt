package com.uzeyir.photoselector

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(1, viewModel.selectedPhotoIndex)
        assertEquals(listOf("photo_1.jpg", "photo_3.jpg"), viewModel.viewerPhotos.map { it.displayName })
        assertEquals("photo_3.jpg", viewModel.selectedPhoto?.displayName)
    }

    @Test
    fun reviewViewerNavigationStaysWithinLikedPhotos() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(3)
        viewModel.setPhotos(photos)
        viewModel.toggleLike(photos[0].uri)
        viewModel.toggleLike(photos[2].uri)

        viewModel.openLikedPhoto(photos[2].uri)
        viewModel.showPreviousPhoto()

        assertEquals(0, viewModel.selectedPhotoIndex)
        assertEquals("photo_1.jpg", viewModel.selectedPhoto?.displayName)
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
            testDocument("IMG_0001.JPG", "image/jpeg"),
            testDocument("clip.MP4", ""),
            testDocument("movie.mov", "application/octet-stream"),
            testDocument("IMG_0001.CR3", "application/octet-stream"),
            testDocument("notes.txt", "text/plain")
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

    private fun testPhotos(count: Int): List<MediaItemData> =
        (1..count).map { index ->
            testPhoto("photo_$index.jpg")
        }

    private fun testVideos(count: Int): List<MediaItemData> =
        (1..count).map { index ->
            testVideo("video_$index.mp4")
        }

    private fun testPhoto(displayName: String): MediaItemData =
        MediaItemData(
            uri = FakeUri(displayName),
            displayName = displayName,
            mimeType = "image/jpeg",
            mediaType = MediaType.Photo
        )

    private fun testVideo(displayName: String): MediaItemData =
        MediaItemData(
            uri = FakeUri(displayName),
            displayName = displayName,
            mimeType = "video/mp4",
            mediaType = MediaType.Video
        )

    private fun testDocument(
        displayName: String,
        mimeType: String = "application/octet-stream"
    ): FolderDocumentData =
        FolderDocumentData(
            uri = FakeUri(displayName),
            displayName = displayName,
            mimeType = mimeType
        )
}
