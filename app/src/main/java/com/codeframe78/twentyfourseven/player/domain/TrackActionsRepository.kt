package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class FavoriteActionStatus {
    Idle,
    Working,
    Added,
    AlreadyFavorite,

    /** The station moved on to another track before the favorite could be added. */
    TrackChanged,
    SignInRequired,
    Failed,
}

data class FavoriteActionState(
    val status: FavoriteActionStatus = FavoriteActionStatus.Idle,
    /** The track the status describes, so a result is never shown against a later track. */
    val trackTitle: String? = null,
)

data class AlbumRatingOption(val value: String, val label: String)

enum class AlbumRatingStatus {
    Idle,
    Loading,
    Ready,
    AlreadyRated,
    Submitting,
    Rated,
    SignInRequired,

    /** The rating was sent, but the station did not confirm it; the listener should not send it again blindly. */
    Unconfirmed,
    Error,
}

data class AlbumRatingState(
    val status: AlbumRatingStatus = AlbumRatingStatus.Idle,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val artist: String? = null,
    val currentRating: String? = null,
    val voteCount: String? = null,
    val options: List<AlbumRatingOption> = emptyList(),
)

data class TrackActionsState(
    val stationId: StationId,
    val favorite: FavoriteActionState = FavoriteActionState(),
    val rating: AlbumRatingState = AlbumRatingState(),
)

/** Listener actions on the track a station is playing now. Every action is a single explicit request. */
interface TrackActionsRepository {
    fun observeTrackActions(stationId: StationId): Flow<TrackActionsState>
    suspend fun addCurrentTrackToFavorites(stationId: StationId, trackTitle: String)
    suspend fun openAlbumRating(stationId: StationId, albumId: String)
    suspend fun submitAlbumRating(stationId: StationId, value: String)
    suspend fun closeAlbumRating(stationId: StationId)
    suspend fun clear(stationId: StationId)
}

object UnavailableTrackActionsRepository : TrackActionsRepository {
    override fun observeTrackActions(stationId: StationId): Flow<TrackActionsState> =
        flowOf(TrackActionsState(stationId))

    override suspend fun addCurrentTrackToFavorites(stationId: StationId, trackTitle: String) = Unit
    override suspend fun openAlbumRating(stationId: StationId, albumId: String) = Unit
    override suspend fun submitAlbumRating(stationId: StationId, value: String) = Unit
    override suspend fun closeAlbumRating(stationId: StationId) = Unit
    override suspend fun clear(stationId: StationId) = Unit
}
