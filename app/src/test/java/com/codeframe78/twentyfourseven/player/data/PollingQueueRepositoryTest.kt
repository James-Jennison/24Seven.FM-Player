package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollingQueueRepositoryTest {
    @Test
    fun `polls immediately and no more than once every sixty seconds`() = runTest {
        val remote = FakeRemote()
        val repository = PollingQueueRepository(
            remote = remote,
            elapsedRealtimeMillis = { testScheduler.currentTime },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeQueue(stationId).collect()
        }

        runCurrent()
        assertEquals(1, remote.calls)

        advanceTimeBy(59_999)
        runCurrent()
        assertEquals(1, remote.calls)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, remote.calls)
    }

    @Test
    fun `manual refresh uses the same sixty second rate limit`() = runTest {
        val remote = FakeRemote()
        val repository = PollingQueueRepository(
            remote = remote,
            elapsedRealtimeMillis = { testScheduler.currentTime },
        )

        repository.refresh(stationId)
        repository.refresh(stationId)
        assertEquals(1, remote.calls)

        advanceTimeBy(60_000)
        repository.refresh(stationId)
        assertEquals(2, remote.calls)
    }

    @Test
    fun `failed initial request exposes a generic error without remote details`() = runTest {
        val repository = PollingQueueRepository(
            remote = FakeRemote(failure = IllegalStateException("sensitive server detail")),
            elapsedRealtimeMillis = { testScheduler.currentTime },
        )

        repository.refresh(stationId)
        val states = mutableListOf<com.codeframe78.twentyfourseven.player.domain.QueueState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeQueue(stationId).collect { states += it }
        }
        runCurrent()
        job.cancel()

        assertEquals(QueueLoadStatus.Error, states.last().status)
        assertEquals("Queue and history could not be refreshed.", states.last().errorMessage)
    }

    @Test
    fun `failed refresh preserves cached rows but marks them stale`() = runTest {
        val remote = FakeRemote()
        val repository = PollingQueueRepository(
            remote = remote,
            elapsedRealtimeMillis = { testScheduler.currentTime },
        )
        repository.refresh(stationId)
        advanceTimeBy(60_000)
        remote.failure = IllegalStateException("sensitive server detail")

        repository.refresh(stationId)

        val state = repository.currentQueue(stationId)
        assertEquals(QueueLoadStatus.Ready, state.status)
        assertEquals("Upcoming", state.upcoming.single().displayTitle)
        assertTrue(state.isStale)
        assertEquals("Cached Queue data could not be refreshed.", state.errorMessage)
    }

    @Test
    fun `read that outlives its observer still reaches the next observer without an error`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(fetchGate = gate)
        val repository = PollingQueueRepository(
            remote = remote,
            elapsedRealtimeMillis = { testScheduler.currentTime },
        )
        val first = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeQueue(stationId).collect()
        }
        runCurrent()
        first.cancel()
        val states = mutableListOf<QueueLoadStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeQueue(stationId).collect { states += it.status }
        }
        runCurrent()

        gate.complete(Unit)
        runCurrent()

        assertEquals(1, remote.calls)
        assertTrue(QueueLoadStatus.Error !in states)
        assertEquals(QueueLoadStatus.Ready, states.last())
    }

    private class FakeRemote(
        var failure: Throwable? = null,
        private val fetchGate: CompletableDeferred<Unit>? = null,
    ) : QueueRemoteDataSource {
        var calls = 0

        override suspend fun fetch(stationId: StationId): QueuePayload {
            calls++
            fetchGate?.await()
            failure?.let { throw it }
            return QueuePayload(
                upcoming = listOf(QueueTrack(1, "Upcoming")),
                recentlyPlayed = listOf(HistoryTrack("Played")),
            )
        }
    }

    private companion object {
        val stationId = StationId("sst")
    }
}
