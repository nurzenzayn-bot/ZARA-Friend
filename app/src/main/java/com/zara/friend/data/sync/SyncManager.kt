package com.zara.friend.data.sync

import com.zara.friend.core.common.DispatchersProvider
import com.zara.friend.domain.model.SyncStatus
import com.zara.friend.domain.model.SyncSummary
import com.zara.friend.domain.model.SyncTrigger
import com.zara.friend.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orkestrator sinkronisasi: menjalankan [SyncRepository.syncNow], menjaga
 * status untuk UI (PLAN Fase 3: "UI status sync dasar"), dan mendelegasikan
 * penjadwalan (WorkManager) ke [SyncScheduler].
 */
@Singleton
class SyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val dispatchers: DispatchersProvider,
    private val syncScheduler: SyncScheduler,
) {

    private val mutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    /** Status sync terakhir — diamati ViewModel untuk indikator UI. */
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Jalankan sync sekarang (suspend). Bila sync sedang berjalan, panggilan
     * berikutnya langsung gagal tanpa menumpuk pekerjaan.
     */
    suspend fun syncNow(trigger: SyncTrigger): Result<SyncSummary> {
        if (!mutex.tryLock()) {
            return Result.failure(IllegalStateException("Sinkronisasi sedang berjalan"))
        }
        try {
            _status.value = SyncStatus.Syncing(trigger)
            val result = withContext(dispatchers.io) { syncRepository.syncNow() }
            _status.value = result.fold(
                onSuccess = { summary -> SyncStatus.Success(summary) },
                onFailure = { throwable ->
                    SyncStatus.Error(throwable.message ?: "Sinkronisasi gagal")
                },
            )
            return result
        } finally {
            mutex.unlock()
        }
    }

    /** Minta sync sekali jalan via WorkManager (constraint network). */
    fun requestSync() {
        syncScheduler.requestImmediateSync()
    }

    /** Pemicu app start: sekali jalan + jadwal periodic. */
    fun scheduleOnAppStart() {
        syncScheduler.requestImmediateSync()
        syncScheduler.ensurePeriodicSyncScheduled()
    }
}
