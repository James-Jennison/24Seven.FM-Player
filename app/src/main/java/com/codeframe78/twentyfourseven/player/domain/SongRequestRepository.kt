package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class RequestSearchField(val wireValue: String) {
    Title("title"),
    Album("album"),
    Artist("artist"),
    Genre("genre"),
}

enum class RequestSuggestionMode(val wireValue: String) {
    Random("random"),
    LeastPlayed("randomleast"),
}

enum class SongRequestLoadStatus { Idle, Loading, Ready, Submitting, Error }

const val MAX_REQUEST_MESSAGE_CHARACTERS = 80

sealed interface RequestSearchTarget {
    data class Album(val albumId: String) : RequestSearchTarget
    data class Artist(val artistName: String) : RequestSearchTarget
}

data class RequestSearchResult(
    val target: RequestSearchTarget,
    val title: String,
    val subtitle: String? = null,
    val year: String? = null,
)

data class RequestableTrack(
    val albumId: String,
    val songId: String,
    val title: String,
    val artist: String? = null,
    val duration: String? = null,
    val eligible: Boolean,
    val albumTitle: String? = null,
    val availability: TrackRequestAvailability = if (eligible) {
        TrackRequestAvailability.available()
    } else {
        TrackRequestAvailability.unknown()
    },
) {
    val identity: RequestTrackIdentity get() = RequestTrackIdentity(
        songId = songId,
        albumId = albumId,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
    )
}

/**
 * The immutable identity captured when a listener opens the request confirmation.
 * A request may only be sent for this station/account/track combination.
 */
data class PreparedSongRequest(
    val stationId: StationId,
    val accountDisplayName: String,
    val track: RequestableTrack,
) {
    /** Convenience accessors keep request-state consumers focused on the confirmed track. */
    val songId: String get() = track.songId
}

/** Fresh state collected immediately before the single request submission. */
data class RequestConfirmationContext(
    val auth: AuthState,
    val queue: QueueState,
    val listenerActivity: ListenerActivityState? = null,
    val requiresListenerActivity: Boolean = false,
)

/** A bounded, in-memory safety block after a request result is uncertain or rejected. */
data class RequestTransactionBlock(
    val availability: TrackRequestAvailability,
    val identity: RequestTrackIdentity? = null,
)

data class SongRequestState(
    val stationId: StationId,
    val status: SongRequestLoadStatus = SongRequestLoadStatus.Idle,
    val query: String = "",
    val searchField: RequestSearchField = RequestSearchField.Title,
    val searchResults: List<RequestSearchResult> = emptyList(),
    val albumTitle: String? = null,
    val tracks: List<RequestableTrack> = emptyList(),
    val pendingRequest: PreparedSongRequest? = null,
    val transactionBlocks: List<RequestTransactionBlock> = emptyList(),
    val notice: String? = null,
    val errorMessage: String? = null,
)

interface SongRequestRepository {
    fun observeRequests(stationId: StationId): Flow<SongRequestState>
    suspend fun search(stationId: StationId, query: String, field: RequestSearchField)
    suspend fun suggest(stationId: StationId, mode: RequestSuggestionMode)
    suspend fun openSearchResult(stationId: StationId, target: RequestSearchTarget)
    suspend fun prepareRequest(stationId: StationId, songId: String, accountDisplayName: String)
    suspend fun prepareRequest(stationId: StationId, track: RequestableTrack, accountDisplayName: String)
    suspend fun cancelRequest(stationId: StationId)
    /** Clears pending state and in-memory request-result blocks for one station session. */
    suspend fun clear(stationId: StationId)
    suspend fun confirmRequest(
        stationId: StationId,
        context: RequestConfirmationContext,
        message: String = "",
    )
}
