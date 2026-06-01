package com.uzeyir.photoselector

data class PricingDiscountTier(
    val photoCount: Int,
    val discountPercent: Int
)

private val defaultDiscountTiers = listOf(
    PricingDiscountTier(photoCount = 4, discountPercent = 5),
    PricingDiscountTier(photoCount = 5, discountPercent = 10),
    PricingDiscountTier(photoCount = 6, discountPercent = 15),
    PricingDiscountTier(photoCount = 7, discountPercent = 20),
    PricingDiscountTier(photoCount = 8, discountPercent = 25),
    PricingDiscountTier(photoCount = 9, discountPercent = 30),
    PricingDiscountTier(photoCount = 10, discountPercent = 35)
)

internal fun discountedPayablePrice(count: Int, unitPrice: Int): Int {
    val billableCount = count.coerceAtMost(10)
    val subtotal = billableCount * unitPrice
    val discountPercent = defaultDiscountTiers
        .lastOrNull { billableCount >= it.photoCount }
        ?.discountPercent
        ?: 0
    return subtotal * (100 - discountPercent) / 100
}
