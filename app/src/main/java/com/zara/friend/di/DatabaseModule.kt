package com.zara.friend.di

import android.content.Context
import androidx.room.Room
import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.MessageDao
import com.zara.friend.data.local.dao.SettingsDao
import com.zara.friend.data.local.dao.SyncDeleteDao
import com.zara.friend.data.local.db.ZaraDatabaseMigrations.ALL
import com.zara.friend.data.local.db.ZaraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Modul database Room. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ZaraDatabase = Room.databaseBuilder(
        context,
        ZaraDatabase::class.java,
        ZaraDatabase.NAME,
    )
        .addCallback(ZaraDatabase.DefaultSettingsCallback())
        .addMigrations(*ALL)
        .build()

    @Provides
    fun provideConversationDao(db: ZaraDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: ZaraDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideSettingsDao(db: ZaraDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideSyncDeleteDao(db: ZaraDatabase): SyncDeleteDao = db.syncDeleteDao()
}
