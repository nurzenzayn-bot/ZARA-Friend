package com.zara.friend.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.zara.friend.core.common.runSuspendCatching
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Implementasi [AuthDataSource] via Firebase Auth anonymous sign-in. */
class FirebaseAuthDataSource @Inject constructor() : AuthDataSource {

    override suspend fun ensureSignedIn(): Result<String> = runSuspendCatching {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.uid
            ?: auth.signInAnonymously().await().user?.uid
                ?: error("Sign-in anonim berhasil tanpa uid")
    }
}
