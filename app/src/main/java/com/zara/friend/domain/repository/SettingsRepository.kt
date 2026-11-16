package com.zara.friend.domain.repository

import com.zara.friend.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Kontrak pengaturan pengguna (Room table user_settings, baris tunggal). */
interface SettingsRepository {

    /** Observe pengaturan; mengembalikan nilai default bila belum ada. */
    fun observeSettings(): Flow<UserSettings>

    /** Ambil pengaturan saat ini (nilai default bila belum ada). */
    suspend fun getSettings(): UserSettings

    /** Simpan pengaturan (upsert baris tunggal). */
    suspend fun updateSettings(settings: UserSettings): Result<Unit>
}
