package com.zara.friend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zara.friend.data.local.entity.SyncDeleteEntity

/** DAO tabel tombstone sync_deletes (penghapusan yang belum di-push ke remote). */
@Dao
interface SyncDeleteDao {

    @Query("SELECT * FROM sync_deletes ORDER BY deletedAt ASC")
    suspend fun getAll(): List<SyncDeleteEntity>

    @Query("SELECT * FROM sync_deletes WHERE conversationId = :conversationId")
    suspend fun getById(conversationId: String): SyncDeleteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tombstone: SyncDeleteEntity)

    @Query("DELETE FROM sync_deletes WHERE conversationId = :conversationId")
    suspend fun deleteById(conversationId: String)

    @Query("DELETE FROM sync_deletes")
    suspend fun clear()
}
