package com.zara.friend.data.repository

import com.zara.friend.core.common.runSuspendCatching
import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.MessageDao
import com.zara.friend.data.local.dao.SettingsDao
import com.zara.friend.data.local.dao.SyncDeleteDao
import com.zara.friend.data.remote.firebase.toFirestore
import com.zara.friend.data.remote.firebase.toEntity
import com.zara.friend.domain.model.SyncState
import com.zara.friend.domain.model.SyncSummary
import com.zara.friend.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Orkestrasi satu siklus sinkronisasi (PLAN 3.5):
 *
 * 1. Hormati `user_settings.syncEnabled`.
 * 2. Anonymous sign-in → uid (cache di user_settings.anonymousUid).
 * 3. Push percakapan LOCAL_ONLY/PENDING + pesan-pesannya → tandai SYNCED.
 * 4. Sinkronkan tombstone penghapusan (lokal dihapus):
 *    - bila remote lebih lama/stale → hapus remote,
 *    - bila remote lebih baru → resurrect (remote menang, LWW),
 *    - bila remote sudah hilang → bersihkan idempoten.
 * 5. Pull percakapan remote yang berubah → merge last-write-wins pada
 *    `updatedAt`; pesan di-merge per id (REPLACE, pesan immutable).
 *
 * Audio tidak ikut di-sync (local-only); transkrip/metadata saja (PLAN 3.5.5).
 */
class SyncRepositoryImpl internal constructor(
    private val authDataSource: com.zara.friend.data.remote.firebase.AuthDataSource,
    private val firestoreSyncDataSource: com.zara.friend.data.remote.firebase.FirestoreSyncDataSource,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val syncDeleteDao: SyncDeleteDao,
    private val settingsDao: SettingsDao,
    private val clock: () -> Long,
) : SyncRepository {

    /** Konstruktor untuk Hilt (lambda injectable tidak didukung Dagger). */
    @Inject
    constructor(
        authDataSource: com.zara.friend.data.remote.firebase.AuthDataSource,
        firestoreSyncDataSource: com.zara.friend.data.remote.firebase.FirestoreSyncDataSource,
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        syncDeleteDao: SyncDeleteDao,
        settingsDao: SettingsDao,
    ) : this(
        authDataSource,
        firestoreSyncDataSource,
        conversationDao,
        messageDao,
        syncDeleteDao,
        settingsDao,
        clock = { System.currentTimeMillis() },
    )

    override suspend fun syncNow(): Result<SyncSummary> = runSuspendCatching {
        val settings = settingsDao.get()
        if (settings?.syncEnabled == false) {
            return@runSuspendCatching SyncSummary(isSkipped = true)
        }

        val uid = authDataSource.ensureSignedIn().getOrThrow()
        settingsDao.updateAnonymousUid(uid)

        val now = clock()
        var pushed = 0
        var pulled = 0
        var remoteDeletions = 0

        // Snapshot data lokal SEBELUM push — dipakai untuk delta pull.
        val locals = conversationDao.getAll()
        val tombstones = syncDeleteDao.getAll()
        val tombstoneById = tombstones.associateBy { it.conversationId }

        // Delta pull: ambil remote yang berubah sejak sync paling lama di lokal.
        // Bila ada tombstone, ambil SEMUA remote agar penghapusan/resurrect
        // bisa dievaluasi (termasuk doc dengan updatedAt lama).
        val hasTombstones = tombstones.isNotEmpty()
        val since = if (hasTombstones) 0L else (locals.minOfOrNull { it.lastSyncedAt ?: 0L } ?: 0L)

        // 1. Push percakapan yang belum tersinkron beserta seluruh pesannya.
        val pending = conversationDao.getPendingSync()
        for (conversation in pending) {
            val messages = messageDao.getByConversation(conversation.id)
            firestoreSyncDataSource.pushConversation(
                uid = uid,
                conversation = conversation.toFirestore(),
                messages = messages.map { it.toFirestore() },
            ).getOrThrow()
            conversationDao.updateSyncInfo(conversation.id, SyncState.SYNCED.name, now)
            pushed++
        }

        // 2. Sinkronkan tombstone penghapusan (lokal dihapus).
        //    - remote stale (updatedAt <= deletedAt) → hapus remote.
        //    - remote lebih baru → resurrect (remote menang, LWW).
        //    - remote sudah hilang → bersihkan idempoten.
        val remoteConversations = firestoreSyncDataSource
            .fetchConversationsUpdatedAfter(uid, since)
            .getOrThrow()
        val remoteById = remoteConversations.associateBy { it.id }

        for (tombstone in tombstones) {
            val remote = remoteById[tombstone.conversationId]
            when {
                remote == null -> {
                    // Remote sudah hilang → bersihkan (idempoten).
                    firestoreSyncDataSource.deleteConversation(uid, tombstone.conversationId).getOrThrow()
                    syncDeleteDao.deleteById(tombstone.conversationId)
                }
                remote.updatedAt <= tombstone.deletedAt -> {
                    // Remote stale → hapus.
                    firestoreSyncDataSource.deleteConversation(uid, tombstone.conversationId).getOrThrow()
                    syncDeleteDao.deleteById(tombstone.conversationId)
                    remoteDeletions++
                }
                else -> {
                    // Remote lebih baru → resurrect (remote menang).
                    conversationDao.upsert(remote.toEntity(SyncState.SYNCED.name, now))
                    mergeRemoteMessages(uid, remote.id)
                    syncDeleteDao.deleteById(tombstone.conversationId)
                    pulled++
                }
            }
        }

        // 3. Pull: merge percakapan remote (last-write-wins berdasarkan updatedAt).
        //    Tombstone yang sudah ditangani di atas di-skip.
        for (remote in remoteConversations) {
            if (tombstoneById.containsKey(remote.id)) continue

            val local = locals.firstOrNull { it.id == remote.id }
            when {
                // Percakapan baru dari perangkat lain.
                local == null -> {
                    conversationDao.upsert(remote.toEntity(SyncState.SYNCED.name, now))
                    mergeRemoteMessages(uid, remote.id)
                    pulled++
                }
                // LWW: remote lebih baru → timpa header + gabungkan pesan.
                remote.updatedAt > local.updatedAt -> {
                    conversationDao.upsert(remote.toEntity(SyncState.SYNCED.name, now))
                    mergeRemoteMessages(uid, remote.id)
                    pulled++
                }
                // Lokal sama/lebih baru → pastikan tercatat SYNCED.
                else -> {
                    conversationDao.updateSyncInfo(local.id, SyncState.SYNCED.name, now)
                }
            }
        }

        SyncSummary(
            pushedConversations = pushed,
            pulledConversations = pulled,
            remoteDeletions = remoteDeletions,
            syncedAt = now,
            isSkipped = false,
        )
    }

    private suspend fun mergeRemoteMessages(uid: String, conversationId: String) {
        val remoteMessages = firestoreSyncDataSource.fetchMessages(uid, conversationId).getOrThrow()
        messageDao.insertAll(remoteMessages.map { it.toEntity() })
    }
}
