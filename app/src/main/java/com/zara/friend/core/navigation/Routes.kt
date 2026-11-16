package com.zara.friend.core.navigation

/**
 * Definisi route terpusat.
 * Chat menerima conversationId opsional: null → buat percakapan baru.
 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val CHAT_PATTERN = "chat/{conversationId}"
    const val CHAT = "chat"

    fun chat(conversationId: String? = null): String =
        if (conversationId.isNullOrBlank()) "$CHAT/" else "$CHAT/$conversationId"
}
