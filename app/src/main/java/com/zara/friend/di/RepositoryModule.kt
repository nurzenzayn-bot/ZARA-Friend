package com.zara.friend.di

import com.zara.friend.data.repository.ChatRepositoryImpl
import com.zara.friend.data.repository.HistoryRepositoryImpl
import com.zara.friend.data.repository.SettingsRepositoryImpl
import com.zara.friend.domain.repository.ChatRepository
import com.zara.friend.domain.repository.HistoryRepository
import com.zara.friend.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binding repository domain interface -> data impl. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
