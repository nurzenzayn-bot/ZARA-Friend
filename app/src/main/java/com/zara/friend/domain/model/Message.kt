package com.zara.friend.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Satu pesan dalam percakapan. Konten berbentuk daftar [MessagePart]
 * agar teks dan audio bisa hidup berdampingan (AUDIO/MIXED).
 */
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val parts: List<MessagePart>,
    val createdAt: Long,
    val status: MessageStatus,
    val errorMessage: String? = null,
) {
    /** Teks gabungan dari seluruh [TextPart] (transkrip/teks pendamping). */
    val text: String?
        get() = parts.filterIsInstance<MessagePart.TextPart>()
            .joinToString(separator = "\n") { it.text }
            .takeIf { it.isNotBlank() }

    /** Audio pertama (jika ada) — balasan suara atau rekaman user. */
    val audio: MessagePart.AudioPart?
        get() = parts.filterIsInstance<MessagePart.AudioPart>().firstOrNull()
}

/** Sealed part pesan: teks atau audio. */
sealed interface MessagePart {

    data class TextPart(val text: String) : MessagePart

    data class AudioPart(
        val localPath: String,
        val mime: String,
        val durationMs: Long? = null,
    ) : MessagePart {
        val duration: Duration?
            get() = durationMs?.milliseconds
    }
}
