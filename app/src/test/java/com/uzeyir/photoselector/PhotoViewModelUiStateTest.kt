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
