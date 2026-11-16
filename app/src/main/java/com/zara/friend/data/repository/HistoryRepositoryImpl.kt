package com.zara.friend.data.repository

import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.SyncDeleteDao
import com.zara.friend.data.local.entity.SyncDeleteEntity
import com.zara.friend.data.mapper.toDomain
import com.zara.friend.data.mapper.toEntity
import com.zara.friend.domain.model.Conversation
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SyncState
import com.zara.friend.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Implementasi lokal HistoryRepository.
 * Judul default pesan pertama di-handle via [touchConversation].
 * Percakapan yang sudah SYNCED dan diubah/dihapus ditandai PENDING /
 * diberi tombstone agar SyncManager menyinkronkan perubahan ke remote.
 */
class HistoryRepositoryImpl internal constructor(
    private val conversationDao: ConversationDao,
    private val syncDeleteDao: SyncDeleteDao,
    private val clock: () -> Long,
) : HistoryRepository {

    /** Konstruktor untuk Hilt (lambda injectable tidak didukung Dagger). */
    @Inject
    constructor(
        conversationDao: ConversationDao,
        syncDeleteDao: SyncDeleteDao,
    ) : this(
        conversationDao,
        syncDeleteDao,
        clock = { System.currentTimeMillis() },
    )

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeConversation(id: String): Flow<Conversation?> =
        conversationDao.observeById(id).map { it?.toDomain() }

    override suspend fun getConversation(id: String): Conversation? =
        conversationDao.getById(id)?.toDomain()

    override suspend fun createConversation(personaMode: PersonaMode): Conversation {
        val now = clock()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = DEFAULT_TITLE,
            personaMode = personaMode,
            createdAt = now,
            updatedAt = now,
            isArchived = false,
            syncState = SyncState.LOCAL_ONLY,
            lastSyncedAt = null,
        )
        conversationDao.upsert(conversation.toEntity())
        return conversation
    }

    override suspend fun renameConversation(id: String, title: String): Result<Unit> =
        runCatching {
            conversationDao.updateTitle(id, title.trim(), clock())
            markPending(id)
        }

    override suspend fun touchConversation(id: String, autoTitle: String?): Result<Unit> =
        runCatching {
            val existing = conversationDao.getById(id)
            when {
                existing == null -> Unit
                autoTitle != null && existing.title == DEFAULT_TITLE -> {
                    val title = autoTitle.trim().take(TITLE_MAX_LENGTH)
                    conversationDao.updateTitle(id, title, clock())
                    markPending(id)
                }
                else -> {
                    conversationDao.touch(id, clock())
                    markPending(id)
                }
            }
        }

    override suspend fun setArchived(id: String, archived: Boolean): Result<Unit> =
        runCatching {
            conversationDao.setArchived(id, archived, clock())
            markPending(id)
        }

    override suspend fun deleteConversation(id: String): Result<Unit> = runCatching {
        val existing = conversationDao.getById(id)
        if (existing != null && existing.syncState == SyncState.SYNCED.name) {
            // Sudah tersinkron → catat tombstone agar SyncManager menghapus remote.
            syncDeleteDao.insert(SyncDeleteEntity(conversationId = id, deletedAt = clock()))
        }
        conversationDao.deleteById(id)
    }

    override suspend fun clearAll(): Result<Unit> = runCatching {
        val now = clock()
        conversationDao.getAll()
            .filter { it.syncState == SyncState.SYNCED.name }
            .forEach { conversation ->
                syncDeleteDao.insert(SyncDeleteEntity(conversation.id, now))
            }
        conversationDao.deleteAll()
    }

    /** Perubahan pada percakapan yang sudah SYNCED → tandai PENDING. */
    private suspend fun markPending(id: String) {
        conversationDao.getById(id)?.let {
            if (it.syncState == SyncState.SYNCED.name) {
                conversationDao.updateSyncState(id, SyncState.PENDING.name)
            }
        }
    }

    companion object {
        const val DEFAULT_TITLE = "Percakapan baru"
        const val TITLE_MAX_LENGTH = 64
    }
}
