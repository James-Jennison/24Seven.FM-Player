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
    /** The station's song number, which its favorites controls act on; known for every row of the member's own list. */
    val songId: String? = requestTrack?.songId,
) {
    val identity: RequestTrackIdentity get() = requestTrack?.identity?.copy(
        artist = requestTrack.artist ?: artist,
        albumTitle = requestTrack.albumTitle ?: album,
    ) ?: RequestTrackIdentity(title = title, artist = artist, albumTitle = album)
}

/** A change to the member's own favorites list, sent as the station's own form sends it. */
enum class FavoriteChange { MoveUp, MoveDown, Remove }

enum class FavoriteChangeStatus { Idle, Working, Done, SignInRequired, Failed }

data class FavoriteChangeState(
    val status: FavoriteChangeStatus = FavoriteChangeStatus.Idle,
    val change: FavoriteChange? = null,
    /** The track the status describes, so a result is never shown against another row. */
    val songId: String? = null,
)

enum class RankedFavoritesStatus { Idle, Loading, Ready, Error }

/**
 * The member's favorites in the order they rank them, as the station shows them on the member's profile page in
 * pages of 50. Moving a track up or down changes this order; the full list is sorted by the station instead.
 */
data class RankedFavoritesState(
    val status: RankedFavoritesStatus = RankedFavoritesStatus.Idle,
    val tracks: List<FavoriteTrack> = emptyList(),
    val loadedPages: Int = 0,
    val hasMore: Boolean = false,
)

data class FavoriteTracksState(
    val stationId: StationId,
    val status: FavoriteTracksLoadStatus = FavoriteTracksLoadStatus.Idle,
    val tracks: List<FavoriteTrack> = emptyList(),
    val errorMessage: String? = null,
    val change: FavoriteChangeState = FavoriteChangeState(),
    val ranked: RankedFavoritesState = RankedFavoritesState(),
)

interface FavoriteTracksRepository {
    fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState>
    suspend fun refresh(stationId: StationId)

    /** Reads the first page of the member's ranked favorites. Needs the station session. */
    suspend fun refreshRanked(stationId: StationId)
    suspend fun loadMoreRanked(stationId: StationId)

    /** Sends one change for one track, then reads the ranked list (and, after a removal, the full list) again. */
    suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange)
    suspend fun clear(stationId: StationId)
}
