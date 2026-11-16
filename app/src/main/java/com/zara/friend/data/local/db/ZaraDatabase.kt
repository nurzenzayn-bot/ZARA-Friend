package com.zara.friend.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.MessageDao
import com.zara.friend.data.local.dao.SettingsDao
import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.data.local.entity.SettingsEntity
import com.zara.friend.domain.model.UserSettingsDefaults

/**
 * Database Room = source of truth lokal.
 * Versi 1: conversations, messages, user_settings (sesuai PLAN.md 4.1).
 * Migrasi berikutnya ditambahkan di data/local/db/Migrations.kt.
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ZaraDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao

    /**
     * Pastikan baris user_settings (id=1) selalu ada — dibuat async
     * saat database pertama kali dibuka.
     */
    class DefaultSettingsCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            insertDefault(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Jaga-jaga bila database dibuat sebelum callback terpasang
            // (mis. perangkat lama): INSERT OR IGNORE idempotent.
            insertDefault(db)
        }

        private fun insertDefault(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO user_settings " +
                    "(id, themeMode, personaMode, voiceAutoPlay, syncEnabled, anonymousUid) VALUES (?, ?, ?, ?, ?, NULL)",
                arrayOf(
                    SettingsEntity.SINGLE_ROW_ID,
                    UserSettingsDefaults.themeMode.name,
                    UserSettingsDefaults.personaMode.name,
                    if (UserSettingsDefaults.voiceAutoPlay) 1 else 0,
                    if (UserSettingsDefaults.syncEnabled) 1 else 0,
                ),
            )
        }
    }

    companion object {
        const val NAME = "zara_friend.db"
    }
}
