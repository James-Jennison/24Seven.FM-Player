package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

data class NowPlayingState(
    val stationId: StationId? = null,
    val displayTitle: String? = null,
)

interface NowPlayingRepository {
    fun observeNowPlaying(): Flow<NowPlayingState>
}

interface NowPlayingPublisher {
    fun publish(state: NowPlayingState)
    fun clear(stationId: StationId? = null)
}
