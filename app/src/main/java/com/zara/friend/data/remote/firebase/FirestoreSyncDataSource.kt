package com.zara.friend.data.remote.firebase

/**
 * Data source mirror Firestore per-user (PLAN 4.3).
 * Implementasi memakai batched writes (limit 500 operasi/batch).
 */
interface FirestoreSyncDataSource {

    /** Tulis percakapan + pesan-pesannya (REPLACE per dokumen). */
    suspend fun pushConversation(
        uid: String,
        conversation: FirestoreConversation,
        messages: List<FirestoreMessage>,
    ): Result<Unit>

    /** Ambil percakapan dengan updatedAt > [since]; since <= 0 berarti semua. */
    suspend fun fetchConversationsUpdatedAfter(
        uid: String,
        since: Long,
    ): Result<List<FirestoreConversation>>

    /** Ambil semua pesan milik satu percakapan. */
    suspend fun fetchMessages(uid: String, conversationId: String): Result<List<FirestoreMessage>>

    /** Hapus percakapan + subcollection pesannya di remote. */
    suspend fun deleteConversation(uid: String, conversationId: String): Result<Unit>
}
