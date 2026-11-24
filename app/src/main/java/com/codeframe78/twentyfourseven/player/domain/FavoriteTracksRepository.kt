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
    /** True when the last attempt to read the next page failed; the pages already read stay. */
    val moreFailed: Boolean = false,
)

data class FavoriteTracksState(
    val stationId: StationId,
    val status: FavoriteTracksLoadStatus = FavoriteTracksLoadStatus.Idle,
    val tracks: List<FavoriteTrack> = emptyList(),
    val errorMessage: String? = null,
    val change: FavoriteChangeState = FavoriteChangeState(),
    val ranked: RankedFavoritesState = RankedFavoritesState(),
    /** Pages of the full list read so far; the stations list 50 tracks a page. */
    val loadedPages: Int = 0,
    /** Whether the station links to a page after the last one read. */
    val hasMore: Boolean = false,
    /** True while the next page of the full list is being read. */
    val loadingMore: Boolean = false,
    /** True when the last attempt to read the next page failed; the pages already read stay. */
    val moreFailed: Boolean = false,
    /** The size of the whole list as the station states it, when it states one. */
    val totalTracks: Int? = null,
    /** Whether the list was read with the station's "Requestable only" choice, which leaves out unavailable tracks. */
    val requestableOnly: Boolean = false,
)

interface FavoriteTracksRepository {
    fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState>
    /** Reads the first page of the full list again. */
    suspend fun refresh(stationId: StationId)

    /** Reads the next page of the full list and adds it to the tracks already shown. */
    suspend fun loadMore(stationId: StationId)

    /**
     * Chooses whether the full list leaves out tracks that cannot be requested now, as the station's own
     * "Requestable only" link does, and reads the first page again. Nothing is saved on the station.
     */
    suspend fun setRequestableOnly(stationId: StationId, requestableOnly: Boolean)

    /** Reads the first page of the member's ranked favorites. Needs the station session. */
    suspend fun refreshRanked(stationId: StationId)
    suspend fun loadMoreRanked(stationId: StationId)

    /** Sends one change for one track, then reads the ranked list (and, after a removal, the full list) again. */
    suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange)

    /**
     * Sends one change for the ranked track at [position] when the ranked list names no song number for it, which is
     * the case for a track that cannot be requested now. The number is read from the track's album page first.
     */
    suspend fun changeRankedFavorite(stationId: StationId, position: Int, change: FavoriteChange)
    suspend fun clear(stationId: StationId)
}
