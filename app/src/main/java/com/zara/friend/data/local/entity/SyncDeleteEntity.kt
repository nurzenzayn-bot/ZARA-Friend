package com.zara.friend.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tombstone percakapan yang dihapus lokal tapi pernah tersinkron (SYNCED).
 * SyncManager menghapus dokumen remote lalu menghapus tombstone ini.
 * Mencegah pull mengembalikan (resurrect) percakapan yang sudah dihapus.
 */
@Entity(tableName = "sync_deletes")
data class SyncDeleteEntity(
    @PrimaryKey val conversationId: String,
    val deletedAt: Long,
)
