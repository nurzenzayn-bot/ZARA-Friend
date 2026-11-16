package com.zara.friend.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Abstraksi penjadwalan sync agar orkestrasi bisa diuji tanpa WorkManager. */
interface SyncScheduler {
    /** Sync sekali jalan (menunggu jaringan bila offline). */
    fun requestImmediateSync()

    /** Pastikan sync berkala terjadwal (idempoten). */
    fun ensurePeriodicSyncScheduled()
}

/** Implementasi [SyncScheduler] di atas WorkManager + [SyncWorker]. */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncScheduler {

    override fun requestImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun ensurePeriodicSyncScheduled() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        const val ONE_TIME_WORK_NAME = "zara_sync_onetime"
        const val PERIODIC_WORK_NAME = "zara_sync_periodic"
        const val PERIODIC_INTERVAL_HOURS = 6L
        const val BACKOFF_SECONDS = 10L
    }
}
