package com.zara.friend.domain.usecase

import com.zara.friend.data.sync.SyncManager
import com.zara.friend.domain.model.SyncSummary
import com.zara.friend.domain.model.SyncTrigger
import javax.inject.Inject

/**
 * Sinkronisasi riwayat on-demand (dipicu manual / setelah kirim pesan).
 * Status untuk UI diamati via [SyncManager.status].
 */
class SyncHistoryUseCase @Inject constructor(
    private val syncManager: SyncManager,
) {

    suspend operator fun invoke(trigger: SyncTrigger): Result<SyncSummary> =
        syncManager.syncNow(trigger)
}
