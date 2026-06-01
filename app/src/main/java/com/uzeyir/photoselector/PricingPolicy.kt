package com.uzeyir.photoselector

data class PricingDiscountTier(
    val itemCount: Int,
    val discountPercent: Int
)

const val DEFAULT_PRICE_LIMIT_COUNT = 10

data class PricingSettings(
    val photoUnitPrice: Int = PHOTO_UNIT_PRICE,
    val videoUnitPrice: Int = VIDEO_UNIT_PRICE,
    val priceLimitCount: Int = DEFAULT_PRICE_LIMIT_COUNT,
    val discountTiers: List<PricingDiscountTier> = defaultDiscountTiers
) {
    fun isValid(): Boolean =
        photoUnitPrice >= 0 &&
            videoUnitPrice >= 0 &&
            priceLimitCount >= 1 &&
            discountTiers.all { it.itemCount >= 1 && it.discountPercent in 0..100 } &&
            discountTiers.map { it.itemCount }.distinct().size == discountTiers.size

    companion object {
        val Default = PricingSettings()
    }
}

val defaultDiscountTiers = listOf(
    PricingDiscountTier(itemCount = 4, discountPercent = 5),
    PricingDiscountTier(itemCount = 5, discountPercent = 10),
    PricingDiscountTier(itemCount = 6, discountPercent = 15),
    PricingDiscountTier(itemCount = 7, discountPercent = 20),
    PricingDiscountTier(itemCount = 8, discountPercent = 25),
    PricingDiscountTier(itemCount = 9, discountPercent = 30),
    PricingDiscountTier(itemCount = 10, discountPercent = 35)
)

internal fun discountedPayablePrice(
    count: Int,
    unitPrice: Int,
    priceLimitCount: Int = DEFAULT_PRICE_LIMIT_COUNT,
    discountTiers: List<PricingDiscountTier> = defaultDiscountTiers
): Int {
    val billableCount = count.coerceAtMost(priceLimitCount)
    val subtotal = billableCount * unitPrice
    val discountPercent = discountTiers
        .sortedBy { it.itemCount }
        .lastOrNull { billableCount >= it.itemCount }
        ?.discountPercent
        ?: 0
    return subtotal * (100 - discountPercent) / 100
}
