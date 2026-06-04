package com.uzeyir.photoselector

internal const val LoopingPhotoPagerPageCount = Int.MAX_VALUE

internal fun loopingPhotoPagerInitialPage(itemCount: Int, selectedIndex: Int): Int {
    if (itemCount <= 1) return selectedIndex.coerceAtLeast(0)
    val centeredStart = LoopingPhotoPagerPageCount / 2
    return centeredStart - (centeredStart % itemCount) + selectedIndex.coerceIn(0, itemCount - 1)
}

internal fun wrappedPhotoIndex(page: Int, itemCount: Int): Int {
    if (itemCount <= 0) return -1
    return ((page % itemCount) + itemCount) % itemCount
}

internal fun nearestLoopingPhotoPage(currentPage: Int, targetIndex: Int, itemCount: Int): Int {
    if (itemCount <= 1) return targetIndex.coerceAtLeast(0)
    val target = targetIndex.coerceIn(0, itemCount - 1)
    val currentIndex = wrappedPhotoIndex(currentPage, itemCount)
    var candidate = currentPage - currentIndex + target
    val forwardCandidate = candidate + itemCount
    val backwardCandidate = candidate - itemCount

    if (kotlin.math.abs(forwardCandidate - currentPage) < kotlin.math.abs(candidate - currentPage)) {
        candidate = forwardCandidate
    }
    if (kotlin.math.abs(backwardCandidate - currentPage) < kotlin.math.abs(candidate - currentPage)) {
        candidate = backwardCandidate
    }
    return candidate.coerceIn(0, LoopingPhotoPagerPageCount - 1)
}
