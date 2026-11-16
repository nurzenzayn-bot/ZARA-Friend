package com.zara.friend.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zara.friend.domain.model.SyncTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker WorkManager untuk sync berkala/on-demand (PLAN 3.5.4).
 * Dijalankan dengan constraint jaringan; retry dengan backoff saat gagal.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
        syncManager.syncNow(SyncTrigger.WORKER).fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            },
        )

    companion object {
        const val MAX_RETRIES = 3
    }
}
