package com.zara.friend.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel conversations — sesuai skema PLAN.md 4.1.
 * Enum domain disimpan sebagai TEXT; konversi di data/mapper.
 */
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updatedAt"])],
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val personaMode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val syncState: String,
    val lastSyncedAt: Long?,
)
