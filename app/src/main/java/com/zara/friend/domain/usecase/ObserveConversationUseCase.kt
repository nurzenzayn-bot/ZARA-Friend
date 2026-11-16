package com.zara.friend.domain.usecase

import com.zara.friend.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observe pesan satu percakapan (Fase 4/5): Room = source of truth.
 */
class ObserveConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(conversationId: String): Flow<List<com.zara.friend.domain.model.Message>> =
        chatRepository.observeMessages(conversationId)
}
