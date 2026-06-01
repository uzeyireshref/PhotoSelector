package com.uzeyir.photoselector

internal const val PHOTO_UNIT_PRICE = 300
internal const val VIDEO_UNIT_PRICE = 1000

internal data class SelectionPricing(
    val photoCount: Int,
    val videoCount: Int,
    val settings: PricingSettings = PricingSettings.Default,
    val photoUnitPrice: Int = settings.photoUnitPrice,
    val videoUnitPrice: Int = settings.videoUnitPrice
) {
    val photoBasePrice: Int = photoCount * photoUnitPrice
    val photoDisplayPrice: Int = discountedPayablePrice(
        count = photoCount,
        unitPrice = photoUnitPrice,
        priceLimitCount = settings.priceLimitCount,
        discountTiers = settings.discountTiers
    )
    val photoDiscount: Int = photoBasePrice - photoDisplayPrice
    val videoBasePrice: Int = videoCount * videoUnitPrice
    val videoDisplayPrice: Int = discountedPayablePrice(
        count = videoCount,
        unitPrice = videoUnitPrice,
        priceLimitCount = settings.priceLimitCount,
        discountTiers = settings.discountTiers
    )
    val videoDiscount: Int = videoBasePrice - videoDisplayPrice
    val basePrice: Int = photoBasePrice + videoBasePrice
    val totalDisplayPrice: Int = photoDisplayPrice + videoDisplayPrice
    val discount: Int = basePrice - totalDisplayPrice
}
