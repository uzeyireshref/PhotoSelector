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

class SelectionPricingTest {

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
    fun defaultDiscountedPayablePriceMatchesPhotoPricingPolicy() {
        assertEquals(900, discountedPayablePrice(count = 3, unitPrice = 300))
        assertEquals(1140, discountedPayablePrice(count = 4, unitPrice = 300))
        assertEquals(1950, discountedPayablePrice(count = 10, unitPrice = 300))
        assertEquals(1950, discountedPayablePrice(count = 12, unitPrice = 300))
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
    fun customPriceLimitCapsPayablePriceAtConfiguredCount() {
        val pricing = SelectionPricing(
            photoCount = 8,
            videoCount = 0,
            settings = PricingSettings(
                photoUnitPrice = 300,
                videoUnitPrice = 1000,
                priceLimitCount = 5,
                discountTiers = listOf(
                    PricingDiscountTier(itemCount = 5, discountPercent = 20)
                )
            )
        )

        assertEquals(2400, pricing.photoBasePrice)
        assertEquals(1200, pricing.photoDisplayPrice)
        assertEquals(1200, pricing.photoDiscount)
    }

    @Test
    fun customDiscountTiersDrivePhotoAndVideoPricingSeparately() {
        val pricing = SelectionPricing(
            photoCount = 6,
            videoCount = 6,
            settings = PricingSettings(
                photoUnitPrice = 400,
                videoUnitPrice = 1200,
                priceLimitCount = 15,
                discountTiers = listOf(
                    PricingDiscountTier(itemCount = 3, discountPercent = 10),
                    PricingDiscountTier(itemCount = 6, discountPercent = 25)
                )
            )
        )

        assertEquals(2400, pricing.photoBasePrice)
        assertEquals(1800, pricing.photoDisplayPrice)
        assertEquals(7200, pricing.videoBasePrice)
        assertEquals(5400, pricing.videoDisplayPrice)
        assertEquals(7200, pricing.totalDisplayPrice)
    }

    @Test
    fun priceCalculationDetailShowsSelectedUnitPrices() {
        val strings = UiText.strings(AppLanguage.Turkish)

        assertEquals(
            "Foto: 2 x 300 TL · Video: 1 x 1000 TL",
            priceCalculationDetail(
                photoCount = 2,
                videoCount = 1,
                photoOriginalPrice = 600,
                videoOriginalPrice = 1000,
                strings = strings
            )
        )
        assertEquals(
            "Foto: 2 x 300 TL",
            priceCalculationDetail(
                photoCount = 2,
                videoCount = 0,
                photoOriginalPrice = 600,
                videoOriginalPrice = 0,
                strings = strings
            )
        )
    }
}
