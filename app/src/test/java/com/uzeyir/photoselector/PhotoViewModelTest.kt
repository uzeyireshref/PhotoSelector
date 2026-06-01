package com.uzeyir.photoselector

import android.net.FakeUri
import android.net.Uri
import androidx.media3.common.Player
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun favoritesActionFromViewerReturnsToGalleryFavoritesTab() {
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
    fun openingPhotoFromFavoritesLimitsViewerToFavoriteMedia() {
        val viewModel = PhotoViewModel()
        val favoritePhoto = testPhoto("favorite.jpg")
        val otherPhoto = testPhoto("other.jpg")
        val favoriteVideo = testVideo("favorite-video.mp4")
        viewModel.setMediaItems(listOf(favoritePhoto, otherPhoto, favoriteVideo))
        viewModel.toggleLike(favoritePhoto.uri)
        viewModel.toggleLike(favoriteVideo.uri)

        viewModel.openFavoritePhoto(favoriteVideo.uri)

        assertEquals(Screen.PhotoDetail, viewModel.currentScreen)
        assertEquals(listOf(favoriteVideo, favoritePhoto), viewModel.viewerPhotos)
        assertEquals(0, viewModel.selectedPhotoIndex)
        assertEquals(favoriteVideo.uri, viewModel.selectedPhotoUri)
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
    fun returnAfterExportCanTargetHomeOrGallery() {
        val viewModel = PhotoViewModel()
        viewModel.setPhotos(testPhotos(1))
        viewModel.navigateTo(Screen.Confirmation)

        viewModel.returnToGalleryAfterExport()
        assertEquals(Screen.Gallery, viewModel.currentScreen)

        viewModel.returnHomeAfterExport()
        assertEquals(Screen.FolderSelection, viewModel.currentScreen)
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
    fun lastFolderRestoreIsAttemptedOnlyOnceForViewModelLifetime() {
        val viewModel = PhotoViewModel()

        assertEquals(true, viewModel.shouldRestoreLastFolderOnStartup())
        viewModel.markLastFolderRestoreAttempted()

        assertEquals(false, viewModel.shouldRestoreLastFolderOnStartup())
    }
}
