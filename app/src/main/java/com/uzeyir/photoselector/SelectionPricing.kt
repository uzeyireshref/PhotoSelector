package com.uzeyir.photoselector

internal const val PHOTO_UNIT_PRICE = 300
internal const val VIDEO_UNIT_PRICE = 1000

internal data class SelectionPricing(
    val photoCount: Int,
    val videoCount: Int,
    val photoUnitPrice: Int = PHOTO_UNIT_PRICE,
    val videoUnitPrice: Int = VIDEO_UNIT_PRICE
) {
    val photoBasePrice: Int = photoCount * photoUnitPrice
    val photoDisplayPrice: Int = discountedPayablePrice(photoCount, photoUnitPrice)
    val photoDiscount: Int = photoBasePrice - photoDisplayPrice
    val videoBasePrice: Int = videoCount * videoUnitPrice
    val videoDisplayPrice: Int = discountedPayablePrice(videoCount, videoUnitPrice)
    val videoDiscount: Int = videoBasePrice - videoDisplayPrice
    val basePrice: Int = photoBasePrice + videoBasePrice
    val totalDisplayPrice: Int = photoDisplayPrice + videoDisplayPrice
    val discount: Int = basePrice - totalDisplayPrice
}
