package com.ranjan.adbsend.domain.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but never swallows coroutine cancellation. Plain runCatching around a
 * suspend call turns a cancelled job into a "failure" (e.g. "StandaloneCoroutine was cancelled").
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
