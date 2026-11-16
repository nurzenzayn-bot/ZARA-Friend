package com.zara.friend.data.repository

import app.cash.turbine.test
import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.MessageDao
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.domain.model.ContentType
import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessagePart
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatRepositoryImplTest {

    private lateinit var dao: MessageDao
    private lateinit var conversationDao: ConversationDao
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        conversationDao = mockk(relaxed = true)
        repository = ChatRepositoryImpl(dao, conversationDao)
    }

    private fun entity(
        id: String,
        text: String?,
        createdAt: Long,
        role: MessageRole = MessageRole.USER,
    ) = MessageEntity(
        id = id,
        conversationId = "c1",
        role = role.name,
        contentText = text,
        contentType = ContentType.TEXT.name,
        audioLocalPath = null,
        audioMimeType = null,
        audioDurationMs = null,
        createdAt = createdAt,
        status = MessageStatus.SENT.name,
        errorMessage = null,
    )

    // ---------- observeMessages ----------

    @Test
    fun `observeMessages emits mapped messages from dao flow`() = runTest {
        every { dao.observeByConversation("c1") } returns flowOf(
            listOf(entity("m1", "Hai", 1L, MessageRole.USER), entity("m2", "Halo juga", 2L, MessageRole.MODEL)),
        )

        repository.observeMessages("c1").test {
            val messages = awaitItem()
            assertEquals(2, messages.size)
            assertEquals("Hai", messages[0].text)
            assertEquals(MessageRole.MODEL, messages[1].role)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeMessages maps empty dao flow to empty list`() = runTest {
        every { dao.observeByConversation("c1") } returns flowOf(emptyList())

        repository.observeMessages("c1").test {
            assertEquals(emptyList<Message>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- getLatestMessages ----------

    @Test
    fun `getLatestMessages reverses dao result to ascending order`() = runTest {
        // DAO return DESC (terbaru dulu).
        coEvery { dao.getLatest("c1", 2) } returns listOf(
            entity("m2", "kedua", 2L),
            entity("m1", "pertama", 1L),
        )

        val messages = repository.getLatestMessages("c1", 2)

        assertEquals(listOf("pertama", "kedua"), messages.map { it.text })
    }

    // ---------- getMessage ----------

    @Test
    fun `getMessage returns null when dao returns null`() = runTest {
        coEvery { dao.getById("m404") } returns null

        assertNull(repository.getMessage("m404"))
    }

    @Test
    fun `getMessage returns mapped message`() = runTest {
        coEvery { dao.getById("m1") } returns entity("m1", "isi", 1L)

        assertEquals("isi", repository.getMessage("m1")?.text)
    }

    // ---------- insertMessage ----------

    @Test
    fun `insertMessage inserts mapped entity and marks conversation PENDING`() = runTest {
        val message = Message(
            id = "m1",
            conversationId = "c1",
            role = MessageRole.USER,
            parts = listOf(MessagePart.TextPart("Hai Zara")),
            createdAt = 100L,
            status = MessageStatus.SENDING,
        )

        val result = repository.insertMessage(message)

        assertTrue(result.isSuccess)
        coVerify {
            dao.insert(
                withArg {
                    assertEquals("m1", it.id)
                    assertEquals("c1", it.conversationId)
                    assertEquals("Hai Zara", it.contentText)
                    assertEquals(MessageStatus.SENDING.name, it.status)
                },
            )
        }
        // PLAN 3.3.6: pesan baru → sync_state PENDING agar SyncManager meng-push.
        coVerify { conversationDao.updateSyncState("c1", SyncState.PENDING.name) }
    }

    @Test
    fun `insertMessage returns failure when dao throws`() = runTest {
        coEvery { dao.insert(any()) } throws IllegalStateException("db locked")
        val message = Message(
            id = "m1",
            conversationId = "c1",
            role = MessageRole.USER,
            parts = listOf(MessagePart.TextPart("x")),
            createdAt = 1L,
            status = MessageStatus.SENDING,
        )

        val result = repository.insertMessage(message)

        assertTrue(result.isFailure)
        assertEquals("db locked", result.exceptionOrNull()?.message)
    }

    // ---------- updateMessageStatus ----------

    @Test
    fun `updateMessageStatus passes status name and error to dao`() = runTest {
        val result = repository.updateMessageStatus(
            id = "m1",
            status = MessageStatus.FAILED,
            errorMessage = "timeout",
        )

        assertTrue(result.isSuccess)
        coVerify { dao.updateStatus("m1", "FAILED", "timeout") }
    }

    // ---------- delete ----------

    @Test
    fun `deleteMessage calls dao deleteById`() = runTest {
        coEvery { dao.getById("m1") } returns null

        val result = repository.deleteMessage("m1")
        assertTrue(result.isSuccess)
        coVerify { dao.deleteById("m1") }
    }

    @Test
    fun `deleteMessage marks conversation PENDING when message found`() = runTest {
        coEvery { dao.getById("m1") } returns entity("m1", "x", 1L)

        assertTrue(repository.deleteMessage("m1").isSuccess)

        coVerify { conversationDao.updateSyncState("c1", SyncState.PENDING.name) }
    }

    @Test
    fun `deleteMessage skips PENDING marking when message not found`() = runTest {
        coEvery { dao.getById("m404") } returns null

        assertTrue(repository.deleteMessage("m404").isSuccess)

        coVerify(exactly = 0) { conversationDao.updateSyncState(any(), any()) }
    }

    @Test
    fun `clearMessages calls dao deleteByConversation and marks PENDING`() = runTest {
        assertTrue(repository.clearMessages("c1").isSuccess)
        coVerify { dao.deleteByConversation("c1") }
        coVerify { conversationDao.updateSyncState("c1", SyncState.PENDING.name) }
    }
}
