package com.zara.friend.di

import com.zara.friend.data.remote.firebase.AuthDataSource
import com.zara.friend.data.remote.firebase.FirebaseAuthDataSource
import com.zara.friend.data.remote.firebase.FirestoreSyncDataSource
import com.zara.friend.data.remote.firebase.FirestoreSyncDataSourceImpl
import com.zara.friend.data.repository.SyncRepositoryImpl
import com.zara.friend.data.sync.SyncScheduler
import com.zara.friend.data.sync.WorkManagerSyncScheduler
import com.zara.friend.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Modul sinkronisasi (Fase 3): auth, Firestore data source, repository, scheduler. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindAuthDataSource(impl: FirebaseAuthDataSource): AuthDataSource

    @Binds
    @Singleton
    abstract fun bindFirestoreSyncDataSource(impl: FirestoreSyncDataSourceImpl): FirestoreSyncDataSource

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler
}
