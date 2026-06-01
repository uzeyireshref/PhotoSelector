package com.uzeyir.photoselector

import androidx.lifecycle.SavedStateHandle
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoViewModelUiStateTest {

    @Test
    fun uiPreferencesSurviveViewModelRecreation() {
        val savedStateHandle = SavedStateHandle()
        val firstViewModel = PhotoViewModel(savedStateHandle)

        firstViewModel.selectLanguage(AppLanguage.English)
        firstViewModel.chooseIncludeRawFiles(false)
        firstViewModel.replaceUpdateStatus(AppUpdateStatus.Available("1.0.99"))

        val recreatedViewModel = PhotoViewModel(savedStateHandle)

        assertEquals(AppLanguage.English, recreatedViewModel.language)
        assertEquals(false, recreatedViewModel.includeRawFiles)
        assertEquals(AppUpdateStatus.Available("1.0.99"), recreatedViewModel.updateStatus)
    }

    @Test
    fun adminPricingSettingsDriveViewModelPrices() {
        val viewModel = PhotoViewModel()
        val photos = testPhotos(8)
        viewModel.setPhotos(photos)
        viewModel.replaceAdminSettings(
            AdminSettings.Default.copy(
                pricing = PricingSettings(
                    photoUnitPrice = 500,
                    videoUnitPrice = 1000,
                    priceLimitCount = 5,
                    discountTiers = listOf(PricingDiscountTier(itemCount = 5, discountPercent = 20))
                )
            )
        )

        photos.forEach { viewModel.toggleLike(it.uri) }

        assertEquals(4000, viewModel.photoBasePrice)
        assertEquals(2000, viewModel.photoDisplayPrice)
        assertEquals(2000, viewModel.totalDisplayPrice)
    }

    @Test
    fun cancellationSafeRunCatchingRethrowsCancellationException() {
        assertThrows(CancellationException::class.java) {
            cancellationSafeRunCatching {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun cancellationSafeRunCatchingStillWrapsRegularFailures() {
        val result = cancellationSafeRunCatching {
            error("boom")
        }

        assertEquals(true, result.isFailure)
    }
}
