package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class FavoriteTracksLoadStatus { Idle, Loading, Ready, Error }

data class FavoriteTrack(
    val position: Int,
    val title: String,
    val album: String,
    val artist: String,
    val genre: String? = null,
    val year: String? = null,
    val duration: String? = null,
    val requestTrack: RequestableTrack? = null,
    val availabilityMessage: String? = null,
    val availability: TrackRequestAvailability = requestTrack?.availability
        ?: classifyStationRequestAvailability(availabilityMessage),
    /** The station's identifier for the track's album, known even when the track cannot be requested right now. */
    val albumId: String? = requestTrack?.albumId,
) {
    val identity: RequestTrackIdentity get() = requestTrack?.identity?.copy(
        artist = requestTrack.artist ?: artist,
        albumTitle = requestTrack.albumTitle ?: album,
    ) ?: RequestTrackIdentity(title = title, artist = artist, albumTitle = album)
}

data class FavoriteTracksState(
    val stationId: StationId,
    val status: FavoriteTracksLoadStatus = FavoriteTracksLoadStatus.Idle,
    val tracks: List<FavoriteTrack> = emptyList(),
    val errorMessage: String? = null,
)

interface FavoriteTracksRepository {
    fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState>
    suspend fun refresh(stationId: StationId)
    suspend fun clear(stationId: StationId)
}
