package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryNowPlayingRepositoryTest {
    @Test
    fun `publish and clear expose immutable station-scoped state`() = runTest {
        val repository = InMemoryNowPlayingRepository()
        val stationId = StationId("adagio")

        repository.publish(NowPlayingState(stationId, "Verified raw ICY title"))
        assertEquals("Verified raw ICY title", repository.observeNowPlaying().first().displayTitle)

        repository.clear(stationId)
        val cleared = repository.observeNowPlaying().first()
        assertEquals(stationId, cleared.stationId)
        assertNull(cleared.displayTitle)
    }
}
