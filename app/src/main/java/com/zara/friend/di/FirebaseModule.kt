package com.zara.friend.di

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Modul Firebase (Auth dipakai langsung via FirebaseAuthDataSource). */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Firestore default. Konsumen menyuntik via dagger.Lazy sehingga instance
     * hanya dibuat saat sync jalan (app tetap hidup tanpa google-services.json).
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
