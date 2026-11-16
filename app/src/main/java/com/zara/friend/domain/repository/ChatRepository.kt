package com.zara.friend.domain.repository

import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak repository percakapan aktif (sumber lokal Room = source of truth).
 * Fase 2: operasi lokal. Panggilan Gemini menyusul di Fase 4 via use case.
 */
interface ChatRepository {

    /** Observe seluruh pesan satu percakapan (urut waktu naik). */
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /** Ambil N pesan terakhir — dipakai untuk sliding window konteks prompt. */
    suspend fun getLatestMessages(conversationId: String, limit: Int): List<Message>

    /** Ambil satu pesan by id. */
    suspend fun getMessage(id: String): Message?

    /** Simpan pesan baru (USER/MODEL). */
    suspend fun insertMessage(message: Message): Result<Unit>

    /** Update status/error pesan (retry UI). */
    suspend fun updateMessageStatus(
        id: String,
        status: MessageStatus,
        errorMessage: String? = null,
    ): Result<Unit>

    /** Hapus satu pesan. */
    suspend fun deleteMessage(id: String): Result<Unit>

    /** Hapus seluruh pesan satu percakapan. */
    suspend fun clearMessages(conversationId: String): Result<Unit>
}
