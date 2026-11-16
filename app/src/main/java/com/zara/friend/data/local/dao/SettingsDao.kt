package com.zara.friend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zara.friend.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/** DAO tabel user_settings (baris tunggal). */
@Dao
interface SettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Query("UPDATE user_settings SET anonymousUid = :uid WHERE id = 1")
    suspend fun updateAnonymousUid(uid: String?)
}
