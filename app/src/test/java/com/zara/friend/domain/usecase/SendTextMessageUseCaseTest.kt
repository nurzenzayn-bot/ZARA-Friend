package com.zara.friend.domain.usecase

import com.zara.friend.core.common.DispatchersProvider
import com.zara.friend.data.remote.gemini.GeminiApiService
import com.zara.friend.data.remote.gemini.GeminiTextRequest
import com.zara.friend.data.remote.gemini.GeminiTextResponse
import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessagePart
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.UserSettings
import com.zara.friend.domain.repository.ChatRepository
import com.zara.friend.domain.repository.HistoryRepository
import com.zara.friend.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SendTextMessageUseCaseTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var geminiApiService: GeminiApiService
    private lateinit var dispatchers: DispatchersProvider
    private lateinit var useCase: SendTextMessageUseCase

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        chatRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        geminiApiService = mockk(relaxed = true)
        dispatchers = object : DispatchersProvider {
            override val io = testDispatcher
            override val default = testDispatcher
            override val main = testDispatcher
        }
        useCase = SendTextMessageUseCase(
            chatRepository = chatRepository,
            historyRepository = historyRepository,
            settingsRepository = settingsRepository,
            geminiApiService = geminiApiService,
            dispatchers = dispatchers,
        )

        coEvery { settingsRepository.getSettings() } returns UserSettings(personaMode = PersonaMode.AKU_KAMU)
        coEvery { historyRepository.createConversation(any()) } returns com.zara.friend.domain.model.Conversation(
            id = "c1", title = "Percakapan baru", personaMode = PersonaMode.AKU_KAMU,
            createdAt = 1L, updatedAt = 1L, isArchived = false,
            syncState = com.zara.friend.domain.model.SyncState.LOCAL_ONLY, lastSyncedAt = null,
        )
        coEvery { chatRepository.insertMessage(any()) } returns Result.success(Unit)
        coEvery { chatRepository.updateMessageStatus(any(), any(), any()) } returns Result.success(Unit)
        coEvery { historyRepository.touchConversation(any(), any()) } returns Result.success(Unit)
        coEvery { chatRepository.getLatestMessages(any(), any()) } returns emptyList()
        coEvery { geminiApiService.generateText(any()) } returns Result.success(
            GeminiTextResponse(text = "Halo! Ada yang bisa kubantu?"),
        )
    }

    @Test
    fun `blank text returns failure without touching repositories`() = runTest(testDispatcher) {
        val result = useCase(conversationId = null, text = "   ")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { chatRepository.insertMessage(any()) }
        coVerify(exactly = 0) { geminiApiService.generateText(any()) }
    }

    @Test
    fun `success creates conversation, saves user and model messages, touches title`() = runTest(testDispatcher) {
        val result = useCase(conversationId = null, text = "Hai Zara")

        assertTrue(result.isSuccess)
        val r = result.getOrThrow()
        assertEquals("c1", r.conversationId)
        assertEquals("Halo! Ada yang bisa kubantu?", r.replyText)

        coVerify { chatRepository.insertMessage(match { it.role == MessageRole.USER && it.text == "Hai Zara" }) }
        coVerify { chatRepository.updateMessageStatus(any(), MessageStatus.SENT, null) }
        coVerify { chatRepository.insertMessage(match { it.role == MessageRole.MODEL }) }
        coVerify { historyRepository.touchConversation("c1", "Hai Zara") }
    }

    @Test
    fun `uses existing conversation when conversationId given`() = runTest(testDispatcher) {
        coEvery { historyRepository.getConversation("cx") } returns com.zara.friend.domain.model.Conversation(
            id = "cx", title = "Judul", personaMode = PersonaMode.GUE_LO,
            createdAt = 1L, updatedAt = 1L, isArchived = false,
            syncState = com.zara.friend.domain.model.SyncState.SYNCED, lastSyncedAt = 1L,
        )

        val result = useCase(conversationId = "cx", text = "Halo")

        assertTrue(result.isSuccess)
        assertEquals("cx", result.getOrThrow().conversationId)
        coVerify(exactly = 0) { historyRepository.createConversation(any()) }
    }

    @Test
    fun `gemini failure marks user message FAILED and returns failure`() = runTest(testDispatcher) {
        coEvery { geminiApiService.generateText(any()) } returns Result.failure(IllegalStateException("timeout"))

        val result = useCase(conversationId = null, text = "Hai")

        assertTrue(result.isFailure)
        coVerify { chatRepository.updateMessageStatus(any(), MessageStatus.FAILED, "timeout") }
        // model message tidak disimpan
        coVerify(exactly = 1) { chatRepository.insertMessage(any()) }
    }

    @Test
    fun `passes sliding-window history to gemini`() = runTest(testDispatcher) {
        val history = listOf(
            Message(id = "m1", conversationId = "c1", role = MessageRole.USER,
                parts = listOf(MessagePart.TextPart("pesan lama")), createdAt = 1L, status = MessageStatus.SENT),
        )
        coEvery { chatRepository.getLatestMessages("c1", 21) } returns history

        useCase(conversationId = null, text = "baru")

        val slot = io.mockk.slot<GeminiTextRequest>()
        coVerify { geminiApiService.generateText(capture(slot)) }
        assertEquals(1, slot.captured.history.size)
        assertEquals("pesan lama", slot.captured.history[0].text)
        assertEquals("baru", slot.captured.userText)
    }
}
