package com.zara.friend.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.zara.friend.domain.model.SyncSummary
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test retry semantics SyncWorker (PLAN 3.5.4): success, retry, failure.
 * Context & WorkerParameters di-mock karena doWork tidak menyentuh Android.
 */
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var params: WorkerParameters
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        context = mockk(relaxUnitFun = true)
        params = mockk()
        syncManager = mockk(relaxed = true)
    }

    private fun worker(runAttemptCount: Int): SyncWorker {
        every { params.runAttemptCount } returns runAttemptCount
        return SyncWorker(context, params, syncManager)
    }

    @Test
    fun `doWork returns success when sync succeeds`() = runTest {
        coEvery { syncManager.syncNow(any()) } returns Result.success(SyncSummary(pushedConversations = 1))

        val result = worker(runAttemptCount = 0).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `doWork retries before max attempts when sync fails`() = runTest {
        coEvery { syncManager.syncNow(any()) } returns Result.failure(IllegalStateException("offline"))

        val result = worker(runAttemptCount = SyncWorker.MAX_RETRIES - 1).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `doWork fails after max attempts`() = runTest {
        coEvery { syncManager.syncNow(any()) } returns Result.failure(IllegalStateException("offline"))

        val result = worker(runAttemptCount = SyncWorker.MAX_RETRIES).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }
}
