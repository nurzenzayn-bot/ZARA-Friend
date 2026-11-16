package com.zara.friend.data.repository

import com.zara.friend.data.local.dao.SettingsDao
import com.zara.friend.data.mapper.toDomain
import com.zara.friend.data.mapper.toDomainOrDefault
import com.zara.friend.data.mapper.toEntity
import com.zara.friend.domain.model.UserSettings
import com.zara.friend.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementasi SettingsRepository (Room table user_settings, baris tunggal).
 */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> =
        settingsDao.observe().map { it.toDomainOrDefault() }

    override suspend fun getSettings(): UserSettings =
        settingsDao.get().toDomainOrDefault()

    override suspend fun updateSettings(settings: UserSettings): Result<Unit> = runCatching {
        settingsDao.upsert(settings.toEntity())
    }

    /** Simpan uid Firebase anonim (dipakai Fase 3). */
    suspend fun updateAnonymousUid(uid: String?): Result<Unit> = runCatching {
        settingsDao.updateAnonymousUid(uid)
    }
}
