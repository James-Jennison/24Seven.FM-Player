package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnavailableChatRepositoryTest {
    @Test
    fun `chat remains unavailable without a verified transport`() = runTest {
        val stationId = StationId("sst")
        val repository = UnavailableChatRepository()
        val state = repository.observeChat(stationId).first()

        assertEquals(stationId, state.stationId)
        assertEquals(ChatLoadStatus.Unavailable, state.status)
        assertTrue(state.messages.isEmpty())
        repository.sendMessage(stationId, "Ignored")
    }
}
