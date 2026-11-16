package com.zara.friend.domain.model

/**
 * Hasil pengiriman satu pesan teks (Fase 4).
 * UI (Fase 5) memakai id untuk scroll/highlight dan teks untuk fallback.
 */
data class SendTextResult(
    val conversationId: String,
    val userMessageId: String,
    val modelMessageId: String,
    val replyText: String,
)
