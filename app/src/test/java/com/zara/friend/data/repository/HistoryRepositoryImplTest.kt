package com.zara.friend.data.repository

import app.cash.turbine.test
import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.SyncDeleteDao
import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.SyncDeleteEntity
import com.zara.friend.data.mapper.toDomain
import com.zara.friend.data.mapper.toEntity
import com.zara.friend.domain.model.Conversation
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryRepositoryImplTest {

    private lateinit var dao: ConversationDao
    private lateinit var syncDeleteDao: SyncDeleteDao
    private lateinit var repository: HistoryRepositoryImpl

    private var now = 1_000L

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        syncDeleteDao = mockk(relaxed = true)
        repository = HistoryRepositoryImpl(
            conversationDao = dao,
            syncDeleteDao = syncDeleteDao,
            clock = { now },
        )
    }

    private fun entity(
        id: String = "c1",
        updatedAt: Long = 100L,
        syncState: SyncState = SyncState.LOCAL_ONLY,
    ) = ConversationEntity(
        id = id,
        title = "Judul",
        personaMode = PersonaMode.AKU_KAMU.name,
        createdAt = 1L,
        updatedAt = updatedAt,
        isArchived = false,
        syncState = syncState.name,
        lastSyncedAt = if (syncState == SyncState.SYNCED) 50L else null,
    )

    // ---------- observeConversations ----------

    @Test
    fun `observeConversations emits mapped list`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(entity("c2", 200L), entity("c1", 100L)))

        repository.observeConversations().test {
            val conversations = awaitItem()
            assertEquals(listOf("c2", "c1"), conversations.map { it.id })
            assertEquals(SyncState.LOCAL_ONLY, conversations[1].syncState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- getConversation ----------

    @Test
    fun `getConversation returns null when not found`() = runTest {
        coEvery { dao.getById("c404") } returns null

        assertEquals(null, repository.getConversation("c404"))
    }

    // ---------- createConversation ----------

    @Test
    fun `createConversation saves entity with defaults and returns it`() = runTest {
        val slot = slot<ConversationEntity>()
        coEvery { dao.upsert(capture(slot)) } returns Unit

        val created = repository.createConversation(PersonaMode.GUE_LO)

        assertEquals(HistoryRepositoryImpl.DEFAULT_TITLE, created.title)
        assertEquals(PersonaMode.GUE_LO, created.personaMode)
        assertEquals(SyncState.LOCAL_ONLY, created.syncState)
        assertEquals(1_000L, created.createdAt)
        assertEquals(1_000L, created.updatedAt)
        assertEquals(false, created.isArchived)
        assertEquals(created.id, slot.captured.id)
        assertEquals(created.createdAt, slot.captured.createdAt)
    }

    // ---------- touchConversation ----------

    @Test
    fun `touchConversation replaces default title with first message text truncated`() = runTest {
        now = 5_000L
        coEvery { dao.getById("c1") } returns entity().copy(title = HistoryRepositoryImpl.DEFAULT_TITLE)
        coEvery { dao.updateTitle(any(), any(), any()) } returns Unit

        val longText = "H".repeat(100)
        val result = repository.touchConversation("c1", autoTitle = longText)

        assertTrue(result.isSuccess)
        coVerify { dao.updateTitle("c1", "H".repeat(HistoryRepositoryImpl.TITLE_MAX_LENGTH), 5_000L) }
    }

    @Test
    fun `touchConversation only bumps updatedAt when title already custom`() = runTest {
        now = 6_000L
        coEvery { dao.getById("c1") } returns entity().copy(title = "Judul custom")

        val result = repository.touchConversation("c1", autoTitle = "Halo")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { dao.updateTitle(any(), any(), any()) }
        coVerify { dao.touch("c1", 6_000L) }
    }

    @Test
    fun `touchConversation no-op when conversation missing`() = runTest {
        coEvery { dao.getById("c404") } returns null

        assertTrue(repository.touchConversation("c404", autoTitle = "Halo").isSuccess)
        coVerify(exactly = 0) { dao.touch(any(), any()) }
        coVerify(exactly = 0) { dao.updateTitle(any(), any(), any()) }
    }

    // ---------- renameConversation ----------

    @Test
    fun `renameConversation trims title and touches updatedAt`() = runTest {
        now = 7_000L
        coEvery { dao.getById("c1") } returns entity()

        val result = repository.renameConversation("c1", "  Ngobrol bareng Zara  ")

        assertTrue(result.isSuccess)
        coVerify { dao.updateTitle("c1", "Ngobrol bareng Zara", 7_000L) }
    }

    // ---------- others ----------

    @Test
    fun `setArchived delegates to dao`() = runTest {
        coEvery { dao.getById("c1") } returns entity()

        assertTrue(repository.setArchived("c1", true).isSuccess)
        coVerify { dao.setArchived("c1", true, 1_000L) }
    }

    @Test
    fun `deleteConversation delegates to dao`() = runTest {
        // LOCAL_ONLY (belum tersinkron) → tanpa tombstone.
        coEvery { dao.getById("c1") } returns entity()

        assertTrue(repository.deleteConversation("c1").isSuccess)
        coVerify { dao.deleteById("c1") }
        coVerify(exactly = 0) { syncDeleteDao.insert(any()) }
    }

    @Test
    fun `deleteConversation on SYNCED conversation records tombstone`() = runTest {
        coEvery { dao.getById("c1") } returns entity(syncState = SyncState.SYNCED)

        assertTrue(repository.deleteConversation("c1").isSuccess)

        val slot = slot<SyncDeleteEntity>()
        coVerify { syncDeleteDao.insert(capture(slot)) }
        assertEquals("c1", slot.captured.conversationId)
        assertEquals(1_000L, slot.captured.deletedAt)
        coVerify { dao.deleteById("c1") }
    }

    @Test
    fun `clearAll delegates to dao`() = runTest {
        coEvery { dao.getAll() } returns emptyList()

        assertTrue(repository.clearAll().isSuccess)
        coVerify { dao.deleteAll() }
        coVerify(exactly = 0) { syncDeleteDao.insert(any()) }
    }

    @Test
    fun `clearAll records tombstones only for SYNCED conversations`() = runTest {
        coEvery { dao.getAll() } returns listOf(
            entity("synced1", syncState = SyncState.SYNCED),
            entity("local1", syncState = SyncState.LOCAL_ONLY),
            entity("pending1", syncState = SyncState.PENDING),
        )

        assertTrue(repository.clearAll().isSuccess)

        val tombstones = mutableListOf<SyncDeleteEntity>()
        coVerify { syncDeleteDao.insert(capture(tombstones)) }
        assertEquals(listOf("synced1"), tombstones.map { it.conversationId })
        coVerify { dao.deleteAll() }
    }

    @Test
    fun `renameConversation marks SYNCED conversation PENDING`() = runTest {
        now = 7_000L
        coEvery { dao.getById("c1") } returns entity(syncState = SyncState.SYNCED)

        val result = repository.renameConversation("c1", "  Ngobrol bareng Zara  ")

        assertTrue(result.isSuccess)
        coVerify { dao.updateTitle("c1", "Ngobrol bareng Zara", 7_000L) }
        coVerify { dao.updateSyncState("c1", SyncState.PENDING.name) }
    }

    @Test
    fun `renameConversation keeps LOCAL_ONLY state`() = runTest {
        coEvery { dao.getById("c1") } returns entity(syncState = SyncState.LOCAL_ONLY)

        assertTrue(repository.renameConversation("c1", "Judul baru").isSuccess)

        coVerify(exactly = 0) { dao.updateSyncState(any(), any()) }
    }

    @Test
    fun `setArchived marks SYNCED conversation PENDING`() = runTest {
        coEvery { dao.getById("c1") } returns entity(syncState = SyncState.SYNCED)

        assertTrue(repository.setArchived("c1", true).isSuccess)

        coVerify { dao.setArchived("c1", true, 1_000L) }
        coVerify { dao.updateSyncState("c1", SyncState.PENDING.name) }
    }

    @Test
    fun `touchConversation marks SYNCED conversation PENDING`() = runTest {
        now = 6_000L
        coEvery { dao.getById("c1") } returns entity(syncState = SyncState.SYNCED)

        assertTrue(repository.touchConversation("c1", autoTitle = "Halo").isSuccess)

        coVerify { dao.touch("c1", 6_000L) }
        coVerify { dao.updateSyncState("c1", SyncState.PENDING.name) }
    }

    @Test
    fun `deleteConversation returns failure when dao throws`() = runTest {
        coEvery { dao.getById("c1") } returns entity(syncState = SyncState.SYNCED)
        coEvery { syncDeleteDao.insert(any()) } throws IllegalStateException("fk")

        val result = repository.deleteConversation("c1")

        assertTrue(result.isFailure)
    }

    // ---------- round trip ----------

    @Test
    fun `conversation entity domain round trip is lossless`() = runTest {
        val domain = Conversation(
            id = "c9",
            title = "T",
            personaMode = PersonaMode.GUE_LO,
            createdAt = 1L,
            updatedAt = 2L,
            isArchived = false,
            syncState = SyncState.PENDING,
            lastSyncedAt = 3L,
        )

        assertEquals(domain, domain.toEntity().toDomain())
    }
}
