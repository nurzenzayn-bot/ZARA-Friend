package com.zara.friend.domain.model

/**
 * Percakapan. Model utama untuk Home (lanjut percakapan terakhir) & Riwayat.
 */
data class Conversation(
    val id: String,
    val title: String,
    val personaMode: PersonaMode,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val syncState: SyncState,
    val lastSyncedAt: Long?,
)
