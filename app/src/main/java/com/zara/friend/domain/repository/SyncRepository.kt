package com.zara.friend.domain.repository

import com.zara.friend.domain.model.SyncSummary

/**
 * Kontrak sinkronisasi riwayat ke Firestore (PLAN 3.5):
 * push perubahan lokal (Room = source of truth) lalu pull merge last-write-wins.
 */
interface SyncRepository {

    /**
     * Jalankan satu siklus push/pull. Idempoten — aman dipanggil berulang.
     * Menghormati `user_settings.syncEnabled`.
     */
    suspend fun syncNow(): Result<SyncSummary>
}
