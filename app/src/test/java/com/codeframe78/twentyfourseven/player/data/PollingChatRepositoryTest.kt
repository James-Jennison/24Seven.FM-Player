package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonCatalog
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonChoice
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonGroup
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonTab
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `send shows at once behind a read in flight and a second tap does not post twice`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(fetchGate = gate)
        val repository = repository(remote) { testScheduler.currentTime }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect()
        }
        runCurrent()

        val first = launch(UnconfinedTestDispatcher(testScheduler)) { repository.sendMessage(stationId, "Hello") }
        val second = launch(UnconfinedTestDispatcher(testScheduler)) { repository.sendMessage(stationId, "Hello") }
        runCurrent()
        assertTrue(repository.observeCachedChat(stationId).first().isSending)
        assertEquals(0, remote.sendCalls)

        gate.complete(Unit)
        runCurrent()
        first.join()
        second.join()

        assertEquals(listOf("Hello"), remote.sentMessages)
        assertFalse(repository.observeCachedChat(stationId).first().isSending)
    }

    @Test
    fun `send cancelled while waiting for a read does not stay marked as sending`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(fetchGate = gate)
        val repository = repository(remote) { testScheduler.currentTime }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect()
        }
        runCurrent()
        val send = launch(UnconfinedTestDispatcher(testScheduler)) { repository.sendMessage(stationId, "Hello") }
        runCurrent()

        send.cancel()
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        assertEquals(0, remote.sendCalls)
        assertFalse(repository.observeCachedChat(stationId).first().isSending)
    }

    @Test
    fun `sends characters outside Latin-1 as numeric references`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }
        val states = mutableListOf<com.codeframe78.twentyfourseven.player.domain.ChatState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect { states += it }
        }
        runCurrent()

        repository.sendMessage(stationId, "Dash — and 💫 ok")
        runCurrent()
        job.cancel()

        assertEquals(listOf("Dash &#8212; and &#128171; ok"), remote.sentMessages)
        assertEquals(null, states.last().sendErrorMessage)
    }

    @Test
    fun `rejects a message whose encoded form passes the station limit`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }
        val states = mutableListOf<com.codeframe78.twentyfourseven.player.domain.ChatState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect { states += it }
        }
        runCurrent()

        repository.sendMessage(stationId, "💫".repeat(30))
        runCurrent()
        job.cancel()

        assertEquals(0, remote.sendCalls)
        assertEquals(
            "This message is too long for the station: each emoji counts as several characters.",
            states.last().sendErrorMessage,
        )
    }

    @Test
    fun `loads the smiley catalog once and keeps it across a send`() = runTest {
        val remote = FakeRemote()
        val repository = repository(remote) { testScheduler.currentTime }
        val states = mutableListOf<com.codeframe78.twentyfourseven.player.domain.ChatState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect { states += it }
        }
        runCurrent()

        repository.loadEmoticons(stationId)
        repository.loadEmoticons(stationId)
        runCurrent()
        assertEquals(1, remote.catalogCalls)
        assertEquals(catalog, states.last().emoticons)
        assertTrue(states.any { it.isLoadingEmoticons })

        repository.sendMessage(stationId, "Hello")
        runCurrent()
        job.cancel()
        assertEquals(catalog, states.last().emoticons)
        assertFalse(states.last().isLoadingEmoticons)
    }

    @Test
    fun `reports a catalog failure and allows a retry`() = runTest {
        val remote = FakeRemote(catalogFails = true)
        val repository = repository(remote) { testScheduler.currentTime }

        repository.loadEmoticons(stationId)
        var state = repository.observeCachedChat(stationId).first()
        assertEquals("The station's smilies could not be loaded.", state.emoticonsErrorMessage)
        assertFalse(state.isLoadingEmoticons)

        remote.catalogFails = false
        repository.loadEmoticons(stationId)
        state = repository.observeCachedChat(stationId).first()
        assertEquals(catalog, state.emoticons)
        assertEquals(null, state.emoticonsErrorMessage)
    }

    @Test
    fun `read that outlives its observer still reaches the next observer without an error`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(fetchGate = gate)
        val repository = repository(remote) { testScheduler.currentTime }
        val first = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect()
        }
        runCurrent()
        first.cancel()
        val states = mutableListOf<ChatLoadStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeChat(stationId).collect { states += it.status }
        }
        runCurrent()

        gate.complete(Unit)
        runCurrent()

        assertEquals(1, remote.fetchCalls)
        assertFalse(ChatLoadStatus.Error in states)
        assertEquals(ChatLoadStatus.Ready, states.last())
    }

    private fun repository(remote: ChatRemoteDataSource, now: () -> Long) = PollingChatRepository(
        remote = remote,
        elapsedRealtimeMillis = now,
    )

    private class FakeRemote(
        private val fetchGate: CompletableDeferred<Unit>? = null,
        var catalogFails: Boolean = false,
    ) : ChatRemoteDataSource {
        var fetchCalls = 0
        var sendCalls = 0
        var catalogCalls = 0
        val sentMessages = mutableListOf<String>()

        override suspend fun fetchEmoticons(stationId: StationId): ChatEmoticonCatalog {
            catalogCalls++
            if (catalogFails) throw java.io.IOException("offline")
            return catalog
        }

        override suspend fun fetch(stationId: StationId): List<ChatMessage> {
            fetchCalls++
            fetchGate?.await()
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
        val catalog = ChatEmoticonCatalog(
            listOf(ChatEmoticonTab("faces", "Common", listOf(ChatEmoticonGroup("Everyday", listOf(ChatEmoticonChoice("-smile-", "Smile")))))),
        )
    }
}
