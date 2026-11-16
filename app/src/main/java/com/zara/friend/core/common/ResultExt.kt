package com.zara.friend.core.common

import kotlinx.coroutines.CancellationException

/**
 * runCatching untuk operasi suspend — [CancellationException] dilempar ulang
 * agar coroutine tetap bisa dibatalkan (dipakai lapisan sync & remote).
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
