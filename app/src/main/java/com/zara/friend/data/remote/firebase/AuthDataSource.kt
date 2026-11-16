package com.zara.friend.data.remote.firebase

/** Sumber autentikasi Firebase (anonim) untuk path sync users/{uid}. */
interface AuthDataSource {

    /**
     * Pastikan user sudah sign-in (anonim); sign-in bila belum.
     * Mengembalikan uid yang jadi root path data di Firestore.
     */
    suspend fun ensureSignedIn(): Result<String>
}
