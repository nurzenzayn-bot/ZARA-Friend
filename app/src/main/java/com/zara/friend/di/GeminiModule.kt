package com.zara.friend.di

import com.zara.friend.BuildConfig
import com.zara.friend.core.common.DispatchersProvider
import com.zara.friend.data.remote.gemini.GeminiApiService
import com.zara.friend.data.remote.gemini.GeminiApiServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Modul Gemini (Fase 4): klien teks berbasis API key. */
@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGeminiApiService(
        dispatchers: DispatchersProvider,
    ): GeminiApiService = GeminiApiServiceImpl(
        apiKey = BuildConfig.GEMINI_API_KEY,
        dispatchers = dispatchers,
    )
}
