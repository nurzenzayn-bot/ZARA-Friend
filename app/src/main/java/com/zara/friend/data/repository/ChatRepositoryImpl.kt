package com.zara.friend.data.repository

import com.zara.friend.data.local.dao.MessageDao
import com.zara.friend.data.mapper.toDomain
import com.zara.friend.data.mapper.toEntity
import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementasi lokal ChatRepository. Room = source of truth.
 * Error dibungkus Result agar UI/use case bisa tampilkan fallback (PLAN 3.3.5).
 */
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
) : ChatRepository {

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeByConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getLatestMessages(
        conversationId: String,
        limit: Int,
    ): List<Message> = messageDao.getLatest(conversationId, limit)
        .map { it.toDomain() }
        // getLatest DESC → balikkan agar urut waktu naik (konsisten observe).
        .reversed()

    override suspend fun getMessage(id: String): Message? =
        messageDao.getById(id)?.toDomain()

    override suspend fun insertMessage(message: Message): Result<Unit> = runCatching {
        messageDao.insert(message.toEntity())
    }

    override suspend fun updateMessageStatus(
        id: String,
        status: MessageStatus,
        errorMessage: String?,
    ): Result<Unit> = runCatching {
        messageDao.updateStatus(id, status.name, errorMessage)
    }

    override suspend fun deleteMessage(id: String): Result<Unit> = runCatching {
        messageDao.deleteById(id)
    }

    override suspend fun clearMessages(conversationId: String): Result<Unit> = runCatching {
        messageDao.deleteByConversation(conversationId)
    }
}
