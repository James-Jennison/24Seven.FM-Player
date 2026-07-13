package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.NowPlayingPublisher
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryNowPlayingRepository : NowPlayingRepository, NowPlayingPublisher {
    private val state = MutableStateFlow(NowPlayingState())

    override fun observeNowPlaying(): Flow<NowPlayingState> = state.asStateFlow()

    override fun publish(state: NowPlayingState) {
        this.state.value = state
    }

    override fun clear(stationId: StationId?) {
        state.value = NowPlayingState(stationId = stationId)
    }
}
