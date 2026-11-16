package com.zara.friend.domain.repository

import com.zara.friend.domain.model.Conversation
import com.zara.friend.domain.model.PersonaMode
import kotlinx.coroutines.flow.Flow

/** Kontrak manajemen daftar percakapan (Riwayat, Home, navigasi chat). */
interface HistoryRepository {

    /** Observe daftar percakapan (urut updatedAt desc). */
    fun observeConversations(): Flow<List<Conversation>>

    /** Observe satu percakapan. */
    fun observeConversation(id: String): Flow<Conversation?>

    /** Ambil satu percakapan by id. */
    suspend fun getConversation(id: String): Conversation?

    /** Buat percakapan baru (UUID, judul sementara, persona mode dari settings). */
    suspend fun createConversation(personaMode: PersonaMode): Conversation

    /** Rename percakapan (juga menyentuh updatedAt). */
    suspend fun renameConversation(id: String, title: String): Result<Unit>

    /**
     * Tandai percakapan baru saja aktif; [autoTitle] dipakai pesan pertama
     * untuk mengganti judul default ("Percakapan baru").
     */
    suspend fun touchConversation(id: String, autoTitle: String? = null): Result<Unit>

    /** Arsipkan/kembalikan percakapan. */
    suspend fun setArchived(id: String, archived: Boolean): Result<Unit>

    /** Hapus satu percakapan (cascade pesan). */
    suspend fun deleteConversation(id: String): Result<Unit>

    /** Hapus semua riwayat. */
    suspend fun clearAll(): Result<Unit>
}
