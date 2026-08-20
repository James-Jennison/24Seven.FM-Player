package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

data class NowPlayingState(
    val stationId: StationId? = null,
    val displayTitle: String? = null,
    val artworkUrl: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val track: String? = null,
)

interface NowPlayingRepository {
    fun observeNowPlaying(): Flow<NowPlayingState>
}

interface NowPlayingPublisher {
    fun publish(state: NowPlayingState)
    fun clear(stationId: StationId? = null)
}

interface NowPlayingArtworkRepository {
    suspend fun fetchArtwork(stationId: StationId): String?
}

interface NowPlayingDetailsRepository {
    suspend fun fetchNowPlaying(stationId: StationId): NowPlayingState?
}
