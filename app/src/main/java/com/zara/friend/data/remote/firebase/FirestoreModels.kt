package com.zara.friend.data.remote.firebase

import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.data.mapper.toContentType
import com.zara.friend.data.mapper.toMessageRole
import com.zara.friend.data.mapper.toMessageStatus

/**
 * DTO mirror Firestore (PLAN 4.3):
 * - users/{uid}/conversations/{conversationId}
 * - users/{uid}/conversations/{conversationId}/messages/{messageId}
 *
 * Blob audio TIDAK di-sync (hemat biaya/kompleksitas) — hanya metadata +
 * transkrip teks; file audio tetap local-only, ditandai via `hasAudio`.
 */
data class FirestoreConversation(
    val id: String,
    val title: String,
    val personaMode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

data class FirestoreMessage(
    val id: String,
    val conversationId: String,
    val role: String,
    val contentText: String?,
    val contentType: String,
    val createdAt: Long,
    val status: String,
    val hasAudio: Boolean,
)

// ---------- Entity -> Firestore ----------

fun ConversationEntity.toFirestore(): FirestoreConversation = FirestoreConversation(
    id = id,
    title = title,
    personaMode = personaMode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
)

fun MessageEntity.toFirestore(): FirestoreMessage = FirestoreMessage(
    id = id,
    conversationId = conversationId,
    role = role,
    contentText = contentText,
    contentType = contentType,
    // Pesan immutable: updatedAt di remote setara createdAt lokal.
    createdAt = createdAt,
    status = status,
    hasAudio = audioLocalPath != null,
)

// ---------- Firestore -> Entity ----------

/** [syncState] dan [lastSyncedAt] dikelola lokal (bukan bagian mirror remote). */
fun FirestoreConversation.toEntity(syncState: String, lastSyncedAt: Long?): ConversationEntity =
    ConversationEntity(
        id = id,
        title = title,
        personaMode = personaMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        syncState = syncState,
        lastSyncedAt = lastSyncedAt,
    )

/** Nilai enum yang tak dikenal di-fallback aman (paralel dengan Mappers.kt). */
fun FirestoreMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.toMessageRole().name,
    contentText = contentText,
    contentType = contentType.toContentType().name,
    audioLocalPath = null, // blob audio tidak di-sync (local-only)
    audioMimeType = null,
    audioDurationMs = null,
    createdAt = createdAt,
    status = status.toMessageStatus().name,
    errorMessage = null,
)

// ---------- Map <-> DTO (dipakai FirestoreSyncDataSourceImpl) ----------

fun FirestoreConversation.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "personaMode" to personaMode,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isArchived" to isArchived,
)

fun FirestoreMessage.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "conversationId" to conversationId,
    "role" to role,
    "contentText" to contentText,
    "contentType" to contentType,
    "createdAt" to createdAt,
    "status" to status,
    "hasAudio" to hasAudio,
)

fun Map<String, Any?>.toFirestoreConversation(): FirestoreConversation? {
    val id = this["id"] as? String ?: return null
    val title = this["title"] as? String ?: return null
    val personaMode = this["personaMode"] as? String ?: return null
    val createdAt = (this["createdAt"] as? Number)?.toLong() ?: return null
    val updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: return null
    val isArchived = this["isArchived"] as? Boolean ?: return null
    return FirestoreConversation(id, title, personaMode, createdAt, updatedAt, isArchived)
}

fun Map<String, Any?>.toFirestoreMessage(): FirestoreMessage? {
    val id = this["id"] as? String ?: return null
    val conversationId = this["conversationId"] as? String ?: return null
    val role = this["role"] as? String ?: return null
    val contentText = this["contentText"] as? String
    val contentType = this["contentType"] as? String ?: return null
    val createdAt = (this["createdAt"] as? Number)?.toLong() ?: return null
    val status = this["status"] as? String ?: return null
    val hasAudio = this["hasAudio"] as? Boolean ?: false
    return FirestoreMessage(id, conversationId, role, contentText, contentType, createdAt, status, hasAudio)
}
