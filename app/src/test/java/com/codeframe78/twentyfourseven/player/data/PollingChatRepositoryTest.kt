package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollingChatRepositoryTest {
    @Test
    fun `polls immediately and no more than once every thirty seconds`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect()
        }

        runCurrent()
        assertEquals(1, remote.fetchCalls)
        advanceTimeBy(29_999)
        runCurrent()
        assertEquals(1, remote.fetchCalls)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, remote.fetchCalls)
    }

    @Test
    fun `manual refresh shares the thirty second read limit`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }

        repository.refresh(stationId)
        repository.refresh(stationId)
        assertEquals(1, remote.fetchCalls)
        advanceTimeBy(30_000)
        repository.refresh(stationId)
        assertEquals(2, remote.fetchCalls)
    }

    @Test
    fun `send returns refreshed messages without persisting history`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }

        repository.sendMessage(stationId, "Protocol test")
        val states = mutableListOf<com.codeframe78.twentyfourseven.player.domain.ChatState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect { states += it }
        }
        runCurrent()
        job.cancel()

        assertEquals(listOf("Protocol test"), remote.sentMessages)
        assertEquals(ChatLoadStatus.Ready, states.last().status)
        assertEquals("Protocol test", states.last().messages.single().messageText)
        assertFalse(states.last().isSending)
    }

    @Test
    fun `rejects unsupported characters before transport`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }
        val states = mutableListOf<com.codeframe78.twentyfourseven.player.domain.ChatState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect { states += it }
        }
        runCurrent()

        repository.sendMessage(stationId, "Unsupported — dash")
        runCurrent()
        job.cancel()

        assertEquals(0, remote.sendCalls)
        assertEquals("This station cannot send one or more characters in that message.", states.last().sendErrorMessage)
    }

    private fun repository(remote: ChatRemoteDataSource, now: () -> Long) = PollingChatRepository(
        remote = remote,
        elapsedRealtimeMillis = now,
    )

    private class FakeRemote : ChatRemoteDataSource {
        var fetchCalls = 0
        var sendCalls = 0
        val sentMessages = mutableListOf<String>()

        override suspend fun fetch(stationId: StationId): List<ChatMessage> {
            fetchCalls++
            return emptyList()
        }

        override suspend fun send(stationId: StationId, message: String): List<ChatMessage> {
            sendCalls++
            sentMessages += message
            return listOf(ChatMessage("Listener", message))
        }
    }

    private companion object {
        val stationId = StationId("sst")
    }
}
