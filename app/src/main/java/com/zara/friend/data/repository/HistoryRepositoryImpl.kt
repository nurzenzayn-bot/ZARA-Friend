package com.zara.friend.data.repository

import com.zara.friend.data.local.dao.ConversationDao
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
 */
class HistoryRepositoryImpl internal constructor(
    private val conversationDao: ConversationDao,
    private val clock: () -> Long,
) : HistoryRepository {

    /** Konstruktor untuk Hilt (lambda injectable tidak didukung Dagger). */
    @Inject
    constructor(conversationDao: ConversationDao) : this(
        conversationDao,
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
        }

    override suspend fun touchConversation(id: String, autoTitle: String?): Result<Unit> =
        runCatching {
            val existing = conversationDao.getById(id)
            when {
                existing == null -> Unit
                autoTitle != null && existing.title == DEFAULT_TITLE -> {
                    val title = autoTitle.trim().take(TITLE_MAX_LENGTH)
                    conversationDao.updateTitle(id, title, clock())
                }
                else -> conversationDao.touch(id, clock())
            }
        }

    override suspend fun setArchived(id: String, archived: Boolean): Result<Unit> =
        runCatching {
            conversationDao.setArchived(id, archived, clock())
        }

    override suspend fun deleteConversation(id: String): Result<Unit> = runCatching {
        conversationDao.deleteById(id)
    }

    override suspend fun clearAll(): Result<Unit> = runCatching {
        conversationDao.deleteAll()
    }

    companion object {
        const val DEFAULT_TITLE = "Percakapan baru"
        const val TITLE_MAX_LENGTH = 64
    }
}
