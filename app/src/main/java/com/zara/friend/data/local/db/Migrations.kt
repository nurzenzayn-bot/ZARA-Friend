package com.zara.friend.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrasi skema Room. Tabel baru wajib ditambahkan di sini agar data
 * pengguna lama tidak hilang.
 */
object ZaraDatabaseMigrations {

    /** v1 → v2: tabel tombstone sync_deletes untuk sync penghapusan (Fase 3). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_deletes` " +
                    "(`conversationId` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`conversationId`))",
            )
        }
    }

    val ALL = arrayOf<Migration>(MIGRATION_1_2)
}
