package com.zara.friend.data.sync

import app.cash.turbine.test
import com.zara.friend.core.common.DispatchersProvider
import com.zara.friend.domain.model.SyncStatus
import com.zara.friend.domain.model.SyncSummary
import com.zara.friend.domain.model.SyncTrigger
import com.zara.friend.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncManagerTest {

    private lateinit var repository: SyncRepository
    private lateinit var scheduler: SyncScheduler
    private lateinit var manager: SyncManager

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testDispatchers = object : DispatchersProvider {
        override val io = testDispatcher
        override val default = testDispatcher
        override val main = testDispatcher
    }

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        manager = SyncManager(repository, testDispatchers, scheduler)
    }

    private fun summary(pushed: Int = 2) = SyncSummary(
        pushedConversations = pushed,
        pulledConversations = 1,
        remoteDeletions = 0,
        syncedAt = 100L,
    )

    // ---------- status flow ----------

    @Test
    fun `syncNow emits Idle then Success with summary`() = runTest(testDispatcher) {
        coEvery { repository.syncNow() } returns Result.success(summary())

        val result = manager.syncNow(SyncTrigger.MANUAL)

        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.Success::class, manager.status.value::class)
    }

    @Test
    fun `syncNow emits Error status when repository fails`() = runTest(testDispatcher) {
        coEvery { repository.syncNow() } returns Result.failure(IllegalStateException("jaringan putus"))

        val result = manager.syncNow(SyncTrigger.APP_START)

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.Error::class, manager.status.value::class)
        assertEquals("jaringan putus", (manager.status.value as SyncStatus.Error).message)
    }

    @Test
    fun `syncNow returns skipped summary untouched`() = runTest(testDispatcher) {
        coEvery { repository.syncNow() } returns
            Result.success(SyncSummary(isSkipped = true))

        val result = manager.syncNow(SyncTrigger.MANUAL)

        assertTrue((manager.status.value as SyncStatus.Success).summary.isSkipped)
        assertTrue(result.getOrThrow().isSkipped)
    }

    // ---------- guard konkurensi ----------

    @Test
    fun `syncNow rejects concurrent call while sync in progress`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Result<SyncSummary>>()
        coEvery { repository.syncNow() } coAnswers { gate.await() }

        val first = async { manager.syncNow(SyncTrigger.APP_START) }
        // Beri kesempatan coroutine pertama memegang mutex.
        testScheduler.advanceUntilIdle()

        val second = manager.syncNow(SyncTrigger.MANUAL)

        assertTrue(second.isFailure)
        coVerify(exactly = 1) { repository.syncNow() }

        gate.complete(Result.success(summary()))
        assertTrue(first.await().isSuccess)
    }

    // ---------- penjadwalan ----------

    @Test
    fun `requestSync delegates to scheduler`() {
        manager.requestSync()

        verify(exactly = 1) { scheduler.requestImmediateSync() }
        verify(exactly = 0) { scheduler.ensurePeriodicSyncScheduled() }
    }

    @Test
    fun `scheduleOnAppStart requests immediate and periodic sync`() {
        manager.scheduleOnAppStart()

        verify(exactly = 1) { scheduler.requestImmediateSync() }
        verify(exactly = 1) { scheduler.ensurePeriodicSyncScheduled() }
    }
}
