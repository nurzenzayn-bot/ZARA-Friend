package com.zara.friend.data.remote.gemini

import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.PersonaMode

/**
 * Penyusun persona Zara + konteks percakapan (PLAN 3.6).
 * Murni logika, tanpa ketergantungan framework — mudah diuji.
 */
object PromptBuilder {

    /** Jumlah pesan terakhir yang dimasukkan ke konteks (sliding window). */
    const val DEFAULT_CONTEXT_SIZE = 20

    /** System instruction konsisten berdasarkan gaya sapaan (PLAN 3.6). */
    fun systemInstruction(personaMode: PersonaMode): String = buildString {
        appendLine("Kamu adalah Zara, teman ngobrol santai dan hangat.")
        appendLine("Gaya bahasa: Indonesia sehari-hari yang natural dan tidak baku (gaul wajar),")
        appendLine("empatik, tidak menghakimi, jujur soal batasanmu sebagai AI.")
        when (personaMode) {
            PersonaMode.AKU_KAMU -> appendLine("Gunakan kata ganti 'aku' dan 'kamu'.")
            PersonaMode.GUE_LO -> appendLine("Gunakan kata ganti 'gue' dan 'lo'.")
        }
        appendLine("Jawab singkat-padat tapi tetap hangat. Hindari jawaban kaku/formal.")
        appendLine("Jika tidak tahu, akui dengan jujur. Jangan mengarang fakta.")
    }.trim()

    /**
     * Sliding window: ambil N pesan terakhir yang punya teks,
     * dipetakan ke [GeminiMessage] dengan role user/model.
     */
    fun buildHistory(
        messages: List<Message>,
        maxMessages: Int = DEFAULT_CONTEXT_SIZE,
    ): List<GeminiMessage> =
        messages
            .filter { !it.text.isNullOrBlank() }
            .takeLast(maxMessages)
            .map { msg ->
                GeminiMessage(
                    role = if (msg.role == MessageRole.USER) GeminiRole.USER else GeminiRole.MODEL,
                    text = msg.text!!.trim(),
                )
            }
}
