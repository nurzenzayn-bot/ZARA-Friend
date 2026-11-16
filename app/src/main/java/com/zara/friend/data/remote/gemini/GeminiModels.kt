package com.zara.friend.data.remote.gemini

/**
 * Model perantara antara domain dan SDK Gemini.
 * Dipakai [PromptBuilder] dan [GeminiApiService] agar logika konteks
 * bisa diuji tanpa ketergantungan SDK Android.
 */
enum class GeminiRole { USER, MODEL }

data class GeminiMessage(
    val role: GeminiRole,
    val text: String,
)

data class GeminiTextRequest(
    val systemInstruction: String,
    val history: List<GeminiMessage>,
    val userText: String,
)

data class GeminiTextResponse(
    val text: String,
    val finishReason: String? = null,
)
