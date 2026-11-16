package com.zara.friend

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.zara.friend.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class — Hilt + WorkManager (factory Hilt) + penjadwalan sync.
 * Firebase sendiri diinisialisasi otomatis oleh FirebaseInitProvider
 * (butuh app/google-services.json yang dihasilkan plugin google-services).
 */
@HiltAndroidApp
class ZaraApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Pemicu sync (PLAN 3.5.4): app start (WorkManager menunggu jaringan bila
        // offline) + periodic. Sync tetap hormati syncEnabled (dicek di repository).
        syncScheduler.requestImmediateSync()
        syncScheduler.ensurePeriodicSyncScheduled()
    }
}
