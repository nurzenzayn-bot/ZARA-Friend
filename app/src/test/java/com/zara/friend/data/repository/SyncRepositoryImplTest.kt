package com.zara.friend.data.repository

import com.zara.friend.data.local.dao.ConversationDao
import com.zara.friend.data.local.dao.MessageDao
import com.zara.friend.data.local.dao.SettingsDao
import com.zara.friend.data.local.dao.SyncDeleteDao
import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.data.local.entity.SettingsEntity
import com.zara.friend.data.local.entity.SyncDeleteEntity
import com.zara.friend.data.remote.firebase.AuthDataSource
import com.zara.friend.data.remote.firebase.FirestoreConversation
import com.zara.friend.data.remote.firebase.FirestoreMessage
import com.zara.friend.data.remote.firebase.FirestoreSyncDataSource
import com.zara.friend.domain.model.ContentType
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SyncState
import com.zara.friend.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test orkestrasi push/pull SyncRepositoryImpl (PLAN 3.5):
 * skip syncEnabled, anonymous auth, push tombstone + pending, merge LWW pull.
 */
class SyncRepositoryImplTest {

    private lateinit var auth: AuthDataSource
    private lateinit var remote: FirestoreSyncDataSource
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDao: MessageDao
    private lateinit var syncDeleteDao: SyncDeleteDao
    private lateinit var settingsDao: SettingsDao
    private lateinit var repository: SyncRepositoryImpl

    private var now = 10_000L

    @Before
    fun setUp() {
        auth = mockk(relaxed = true)
        remote = mockk(relaxed = true)
        conversationDao = mockk(relaxed = true)
        messageDao = mockk(relaxed = true)
        syncDeleteDao = mockk(relaxed = true)
        settingsDao = mockk(relaxed = true)
        repository = SyncRepositoryImpl(
            authDataSource = auth,
            firestoreSyncDataSource = remote,
            conversationDao = conversationDao,
            messageDao = messageDao,
            syncDeleteDao = syncDeleteDao,
            settingsDao = settingsDao,
            clock = { now },
        )

        // Default stub "jalan tanpa hambatan".
        coEvery { auth.ensureSignedIn() } returns Result.success("uid-1")
        coEvery { settingsDao.get() } returns settingsEntity(syncEnabled = true)
        coEvery { settingsDao.updateAnonymousUid(any()) } returns Unit
        coEvery { conversationDao.getAll() } returns emptyList()
        coEvery { conversationDao.getPendingSync() } returns emptyList()
        coEvery { syncDeleteDao.getAll() } returns emptyList()
        coEvery { messageDao.getByConversation(any()) } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", any()) } returns
            Result.success(emptyList())
    }

    private fun settingsEntity(syncEnabled: Boolean) = SettingsEntity(
        id = 1,
        themeMode = ThemeMode.SYSTEM.name,
        personaMode = PersonaMode.AKU_KAMU.name,
        voiceAutoPlay = true,
        syncEnabled = syncEnabled,
        anonymousUid = null,
    )

    private fun conversation(
        id: String = "c1",
        syncState: SyncState = SyncState.LOCAL_ONLY,
        updatedAt: Long = 100L,
        lastSyncedAt: Long? = null,
    ) = ConversationEntity(
        id = id,
        title = "Judul $id",
        personaMode = PersonaMode.AKU_KAMU.name,
        createdAt = 1L,
        updatedAt = updatedAt,
        isArchived = false,
        syncState = syncState.name,
        lastSyncedAt = lastSyncedAt,
    )

    private fun message(
        id: String = "m1",
        conversationId: String = "c1",
        withAudio: Boolean = false,
    ) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = MessageRole.USER.name,
        contentText = "Hai",
        contentType = ContentType.TEXT.name,
        audioLocalPath = if (withAudio) "/cache/a.wav" else null,
        audioMimeType = if (withAudio) "audio/wav" else null,
        audioDurationMs = null,
        createdAt = 50L,
        status = MessageStatus.SENT.name,
        errorMessage = null,
    )

    private fun remoteConversation(
        id: String = "r1",
        updatedAt: Long = 500L,
    ) = FirestoreConversation(
        id = id,
        title = "Remote $id",
        personaMode = PersonaMode.GUE_LO.name,
        createdAt = 1L,
        updatedAt = updatedAt,
        isArchived = false,
    )

    // ---------- skip & auth ----------

    @Test
    fun `syncNow skips when syncEnabled false`() = runTest {
        coEvery { settingsDao.get() } returns settingsEntity(syncEnabled = false)

        val summary = repository.syncNow().getOrThrow()

        assertTrue(summary.isSkipped)
        coVerify(exactly = 0) { auth.ensureSignedIn() }
        coVerify(exactly = 0) { remote.pushConversation(any(), any(), any()) }
    }

    @Test
    fun `syncNow fails when auth fails`() = runTest {
        coEvery { auth.ensureSignedIn() } returns Result.failure(IllegalStateException("nope"))

        val result = repository.syncNow()

        assertTrue(result.isFailure)
        assertEquals("nope", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { remote.pushConversation(any(), any(), any()) }
    }

    @Test
    fun `syncNow caches anonymous uid in settings`() = runTest {
        repository.syncNow().getOrThrow()

        coVerify { settingsDao.updateAnonymousUid("uid-1") }
    }

    // ---------- push ----------

    @Test
    fun `syncNow pushes pending conversations with messages and marks SYNCED`() = runTest {
        coEvery { conversationDao.getPendingSync() } returns listOf(
            conversation("c1", syncState = SyncState.PENDING),
        )
        coEvery { messageDao.getByConversation("c1") } returns listOf(
            message("m1", withAudio = true),
        )

        val summary = repository.syncNow().getOrThrow()

        assertEquals(1, summary.pushedConversations)
        coVerify {
            remote.pushConversation(
                "uid-1",
                withArg { dto ->
                    assertEquals("c1", dto.id)
                    assertEquals(100L, dto.updatedAt)
                },
                withArg { messages ->
                    assertEquals(1, messages.size)
                    assertEquals(true, messages[0].hasAudio)
                    assertEquals("Hai", messages[0].contentText)
                },
            )
        }
        coVerify { conversationDao.updateSyncInfo("c1", SyncState.SYNCED.name, 10_000L) }
    }

    @Test
    fun `syncNow fails when push fails and leaves conversation pending`() = runTest {
        coEvery { conversationDao.getPendingSync() } returns listOf(conversation("c1"))
        coEvery { remote.pushConversation(any(), any(), any()) } returns
            Result.failure(IllegalStateException("network down"))

        val result = repository.syncNow()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { conversationDao.updateSyncInfo(any(), any(), any()) }
    }

    // ---------- tombstone push ----------

    @Test
    fun `syncNow pushes tombstones to remote and clears them`() = runTest {
        coEvery { syncDeleteDao.getAll() } returns listOf(
            SyncDeleteEntity(conversationId = "c9", deletedAt = 9_000L),
        )

        val summary = repository.syncNow().getOrThrow()

        coVerify { remote.deleteConversation("uid-1", "c9") }
        coVerify { syncDeleteDao.deleteById("c9") }
        assertEquals(0, summary.remoteDeletions)
    }

    // ---------- pull: merge LWW ----------

    @Test
    fun `syncNow inserts new remote conversation with its messages`() = runTest {
        coEvery { conversationDao.getAll() } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", 0L) } returns
            Result.success(listOf(remoteConversation("r1")))
        coEvery { remote.fetchMessages("uid-1", "r1") } returns
            Result.success(listOf(FirestoreMessage("rm1", "r1", "MODEL", "Halo", "TEXT", 400L, "SENT", false)))

        val summary = repository.syncNow().getOrThrow()

        assertEquals(1, summary.pulledConversations)
        val slot = slot<ConversationEntity>()
        coVerify { conversationDao.upsert(capture(slot)) }
        assertEquals("r1", slot.captured.id)
        assertEquals(SyncState.SYNCED.name, slot.captured.syncState)
        assertEquals(10_000L, slot.captured.lastSyncedAt)
        coVerify {
            messageDao.insertAll(
                withArg { list ->
                    assertEquals(listOf("rm1"), list.map { it.id })
                },
            )
        }
    }

    @Test
    fun `syncNow overwrites local header when remote is newer`() = runTest {
        val local = conversation("c1", syncState = SyncState.SYNCED, updatedAt = 100L, lastSyncedAt = 100L)
        coEvery { conversationDao.getAll() } returns listOf(local)
        coEvery { conversationDao.getPendingSync() } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", 100L) } returns
            Result.success(listOf(remoteConversation("c1", updatedAt = 500L)))
        coEvery { remote.fetchMessages("uid-1", "c1") } returns Result.success(emptyList())

        val summary = repository.syncNow().getOrThrow()

        assertEquals(1, summary.pulledConversations)
        val slot = slot<ConversationEntity>()
        coVerify { conversationDao.upsert(capture(slot)) }
        assertEquals("Remote c1", slot.captured.title)
        assertEquals(500L, slot.captured.updatedAt)
    }

    @Test
    fun `syncNow keeps newer local header and just marks SYNCED`() = runTest {
        val local = conversation("c1", syncState = SyncState.SYNCED, updatedAt = 1_000L, lastSyncedAt = 1_000L)
        coEvery { conversationDao.getAll() } returns listOf(local)
        coEvery { conversationDao.getPendingSync() } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", 1_000L) } returns
            Result.success(listOf(remoteConversation("c1", updatedAt = 500L)))

        val summary = repository.syncNow().getOrThrow()

        assertEquals(0, summary.pulledConversations)
        coVerify(exactly = 0) { conversationDao.upsert(any()) }
        coVerify(exactly = 0) { remote.fetchMessages(any(), any()) }
        coVerify { conversationDao.updateSyncInfo("c1", SyncState.SYNCED.name, 10_000L) }
    }

    // ---------- pull vs tombstone ----------

    @Test
    fun `syncNow deletes stale remote copy of tombstoned conversation`() = runTest {
        coEvery { syncDeleteDao.getAll() } returns listOf(
            SyncDeleteEntity(conversationId = "c9", deletedAt = 9_000L),
        )
        coEvery { conversationDao.getAll() } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", 0L) } returns
            Result.success(listOf(remoteConversation("c9", updatedAt = 5_000L)))

        val summary = repository.syncNow().getOrThrow()

        coVerify { remote.deleteConversation("uid-1", "c9") }
        coVerify(exactly = 0) { conversationDao.upsert(any()) }
        assertEquals(1, summary.remoteDeletions)
    }

    @Test
    fun `syncNow resurrects remote conversation modified after local deletion`() = runTest {
        coEvery { syncDeleteDao.getAll() } returns listOf(
            SyncDeleteEntity(conversationId = "c9", deletedAt = 9_000L),
        )
        coEvery { conversationDao.getAll() } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", 0L) } returns
            Result.success(listOf(remoteConversation("c9", updatedAt = 11_000L)))
        coEvery { remote.fetchMessages("uid-1", "c9") } returns Result.success(emptyList())

        val summary = repository.syncNow().getOrThrow()

        coVerify(exactly = 0) { remote.deleteConversation(any(), any()) }
        coVerify { conversationDao.upsert(withArg { assertEquals("c9", it.id) }) }
        assertEquals(1, summary.pulledConversations)
        assertEquals(0, summary.remoteDeletions)
    }

    // ---------- delta sejak sync terakhir ----------

    @Test
    fun `syncNow pulls delta since oldest lastSyncedAt`() = runTest {
        coEvery { conversationDao.getAll() } returns listOf(
            conversation("a", syncState = SyncState.SYNCED, lastSyncedAt = 300L),
            conversation("b", syncState = SyncState.SYNCED, lastSyncedAt = 700L),
        )
        coEvery { conversationDao.getPendingSync() } returns emptyList()

        repository.syncNow().getOrThrow()

        coVerify { remote.fetchConversationsUpdatedAfter("uid-1", 300L) }
    }

    @Test
    fun `syncNow uses since 0 when nothing was synced before`() = runTest {
        coEvery { conversationDao.getAll() } returns listOf(
            conversation("a", syncState = SyncState.LOCAL_ONLY, lastSyncedAt = null),
        )
        coEvery { conversationDao.getPendingSync() } returns listOf(conversation("a"))

        repository.syncNow().getOrThrow()

        coVerify { remote.fetchConversationsUpdatedAfter("uid-1", 0L) }
    }

    // ---------- ringkasan ----------

    @Test
    fun `syncNow returns summary with counts and timestamp`() = runTest {
        coEvery { conversationDao.getPendingSync() } returns listOf(conversation("c1"))
        coEvery { messageDao.getByConversation("c1") } returns emptyList()
        coEvery { remote.fetchConversationsUpdatedAfter("uid-1", 0L) } returns
            Result.success(listOf(remoteConversation("r1")))
        coEvery { remote.fetchMessages("uid-1", "r1") } returns Result.success(emptyList())

        val summary = repository.syncNow().getOrThrow()

        assertTrue(summary.isSkipped.not())
        assertEquals(1, summary.pushedConversations)
        assertEquals(1, summary.pulledConversations)
        assertEquals(0, summary.remoteDeletions)
        assertEquals(10_000L, summary.syncedAt)
    }
}
