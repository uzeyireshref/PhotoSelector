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

internal fun priceCalculationDetail(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    videoOriginalPrice: Int,
    strings: LocalizedStrings
): String =
    priceCalculationParts(
        photoCount = photoCount,
        videoCount = videoCount,
        photoOriginalPrice = photoOriginalPrice,
        videoOriginalPrice = videoOriginalPrice,
        strings = strings
    ).joinToString(" · ")

internal fun priceCalculationParts(
    photoCount: Int,
    videoCount: Int,
    photoOriginalPrice: Int,
    videoOriginalPrice: Int,
    strings: LocalizedStrings
): List<String> =
    buildList {
        if (photoCount > 0) {
            val unitPrice = photoOriginalPrice / photoCount
            add("${strings.photoShort}: $photoCount x ${strings.price(unitPrice)}")
        }
        if (videoCount > 0) {
            val unitPrice = videoOriginalPrice / videoCount
            add("${strings.videoShort}: $videoCount x ${strings.price(unitPrice)}")
        }
    }
