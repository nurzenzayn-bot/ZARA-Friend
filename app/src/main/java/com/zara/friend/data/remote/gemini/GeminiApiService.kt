package com.zara.friend.data.remote.gemini

import com.zara.friend.core.common.runSuspendCatching

/**
 * Kontrak klien Gemini untuk teks (Fase 4).
 * Abstraksi agar use case bisa diuji dengan fake/mock.
 */
interface GeminiApiService {

    suspend fun generateText(request: GeminiTextRequest): Result<GeminiTextResponse>
}
