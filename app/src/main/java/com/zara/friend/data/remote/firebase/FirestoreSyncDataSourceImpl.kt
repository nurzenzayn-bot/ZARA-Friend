package com.zara.friend.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.zara.friend.core.common.runSuspendCatching
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import dagger.Lazy

/**
 * Implementasi [FirestoreSyncDataSource] dengan Firestore default.
 * Firestore disuntik via [Lazy] agar instance Firebase hanya dibuat saat
 * sync benar-benar jalan — app tetap jalan tanpa google-services.json
 * (sync akan gagal terkontrol dengan pesan error, bukan crash saat startup).
 */
class FirestoreSyncDataSourceImpl @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) : FirestoreSyncDataSource {

    private val db: FirebaseFirestore get() = firestore.get()

    override suspend fun pushConversation(
        uid: String,
        conversation: FirestoreConversation,
        messages: List<FirestoreMessage>,
    ): Result<Unit> = runSuspendCatching {
        val convRef = db.collection(USERS).document(uid)
            .collection(CONVERSATIONS).document(conversation.id)

        // Batch maksimal 500 operasi — sisakan slot untuk dokumen percakapan.
        val chunks = messages.chunked(MAX_BATCH_OPERATIONS - 1).ifEmpty { listOf(emptyList()) }
        chunks.forEachIndexed { index, chunk ->
            val batch = db.batch()
            if (index == 0) batch.set(convRef, conversation.toMap())
            chunk.forEach { message ->
                batch.set(convRef.collection(MESSAGES).document(message.id), message.toMap())
            }
            batch.commit().await()
        }
    }

    override suspend fun fetchConversationsUpdatedAfter(
        uid: String,
        since: Long,
    ): Result<List<FirestoreConversation>> = runSuspendCatching {
        val collection = db.collection(USERS).document(uid).collection(CONVERSATIONS)
        val query = if (since > 0L) {
            collection.whereGreaterThan(FIELD_UPDATED_AT, since)
        } else {
            collection
        }
        query.get().await().documents
            .mapNotNull { it.data?.toFirestoreConversation() }
    }

    override suspend fun fetchMessages(
        uid: String,
        conversationId: String,
    ): Result<List<FirestoreMessage>> = runSuspendCatching {
        db.collection(USERS).document(uid)
            .collection(CONVERSATIONS).document(conversationId)
            .collection(MESSAGES)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.toFirestoreMessage() }
    }

    override suspend fun deleteConversation(
        uid: String,
        conversationId: String,
    ): Result<Unit> = runSuspendCatching {
        val convRef = db.collection(USERS).document(uid)
            .collection(CONVERSATIONS).document(conversationId)
        val messageDocs = convRef.collection(MESSAGES).get().await().documents

        messageDocs.chunked(MAX_BATCH_OPERATIONS - 1).ifEmpty { listOf(emptyList()) }
            .forEachIndexed { index, chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                if (index == 0 || chunk.isEmpty()) batch.delete(convRef)
                batch.commit().await()
            }
    }

    private companion object {
        const val USERS = "users"
        const val CONVERSATIONS = "conversations"
        const val MESSAGES = "messages"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val MAX_BATCH_OPERATIONS = 450
    }
}
