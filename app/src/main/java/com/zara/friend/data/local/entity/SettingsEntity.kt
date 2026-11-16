package com.zara.friend.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabel user_settings — baris tunggal (id = 1), dibuat default saat
 * database pertama kali dibuka (lihat ZaraDatabase.Callback).
 */
@Entity(tableName = "user_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val themeMode: String,
    val personaMode: String,
    val voiceAutoPlay: Boolean,
    val syncEnabled: Boolean,
    val anonymousUid: String?,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
