package com.zara.friend.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel messages — sesuai skema PLAN.md 4.1.
 * Satu pesan bisa teks, audio, atau campuran (transkrip + audio).
 * FK ke conversations dengan cascade delete.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "createdAt"])],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val contentText: String?,
    val contentType: String,
    val audioLocalPath: String?,
    val audioMimeType: String?,
    val audioDurationMs: Long?,
    val createdAt: Long,
    val status: String,
    val errorMessage: String?,
)
