package com.uzeyir.photoselector

import java.util.concurrent.CancellationException

inline fun <T> cancellationSafeRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
