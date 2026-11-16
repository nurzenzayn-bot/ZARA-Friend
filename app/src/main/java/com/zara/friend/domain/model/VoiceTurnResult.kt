package com.zara.friend.domain.model

/**
 * Hasil satu giliran suara (Fase 6): balasan Gemini berupa audio native
 * + teks pendamping bila tersedia. Didefinisikan sekarang agar kontrak
 * use case/repository stabil sejak Fase 2.
 */
data class VoiceTurnResult(
    val replyText: String?,
    val replyAudioPath: String?,
    val mime: String,
    val durationMs: Long?,
)
