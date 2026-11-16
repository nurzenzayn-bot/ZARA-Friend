package com.zara.friend.domain.usecase

import com.zara.friend.core.common.DispatchersProvider
import com.zara.friend.data.remote.gemini.GeminiApiService
import com.zara.friend.data.remote.gemini.GeminiTextRequest
import com.zara.friend.data.remote.gemini.PromptBuilder
import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessagePart
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SendTextResult
import com.zara.friend.domain.model.UserSettings
import com.zara.friend.domain.repository.ChatRepository
import com.zara.friend.domain.repository.HistoryRepository
import com.zara.friend.domain.repository.SettingsRepository
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Kirim pesan teks ke Gemini (Fase 4):
 * 1. Validasi input.
 * 2. Buat percakapan bila belum ada.
 * 3. Insert pesan USER (status SENDING) → konteks → panggil Gemini.
 * 4. Sukses: update USER→SENT, insert pesan MODEL, touch judul percakapan.
 * 5. Gagal: update USER→FAILED + pesan error (opsi retry di Fase 5).
 *
 * Room = source of truth; sync ditandai PENDING otomatis oleh ChatRepository.
 */
class SendTextMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiApiService: GeminiApiService,
    private val dispatchers: DispatchersProvider,
) {

    suspend operator fun invoke(
        conversationId: String?,
        text: String,
    ): Result<SendTextResult> = withContext(dispatchers.io) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Pesan tidak boleh kosong"))
        }

        val settings = settingsRepository.getSettings()
        val conversation = conversationId?.let { historyRepository.getConversation(it) }
            ?: historyRepository.createConversation(settings.personaMode)

        val personaMode = conversation.personaMode
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = Message(
            id = userMessageId,
            conversationId = conversation.id,
            role = MessageRole.USER,
            parts = listOf(MessagePart.TextPart(trimmed)),
            createdAt = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
        )

        chatRepository.insertMessage(userMessage).getOrElse { return@withContext Result.failure(it) }

        // Konteks: N pesan terakhir (sliding window) SEBELUM pesan user ini.
        val contextMessages = chatRepository.getLatestMessages(conversation.id, PromptBuilder.DEFAULT_CONTEXT_SIZE + 1)
            .filter { it.id != userMessageId }
        val history = PromptBuilder.buildHistory(contextMessages)

        val request = GeminiTextRequest(
            systemInstruction = PromptBuilder.systemInstruction(personaMode),
            history = history,
            userText = trimmed,
        )

        val reply = geminiApiService.generateText(request).getOrElse { error ->
            chatRepository.updateMessageStatus(userMessageId, MessageStatus.FAILED, error.message)
            return@withContext Result.failure(error)
        }

        chatRepository.updateMessageStatus(userMessageId, MessageStatus.SENT, null)

        val modelMessageId = UUID.randomUUID().toString()
        val modelMessage = Message(
            id = modelMessageId,
            conversationId = conversation.id,
            role = MessageRole.MODEL,
            parts = listOf(MessagePart.TextPart(reply.text)),
            createdAt = System.currentTimeMillis(),
            status = MessageStatus.SENT,
        )
        chatRepository.insertMessage(modelMessage).getOrElse { return@withContext Result.failure(it) }

        // Judul percakapan: dari pesan pertama bila masih default.
        historyRepository.touchConversation(conversation.id, autoTitle = trimmed)

        Result.success(
            SendTextResult(
                conversationId = conversation.id,
                userMessageId = userMessageId,
                modelMessageId = modelMessageId,
                replyText = reply.text,
            ),
        )
    }
}
