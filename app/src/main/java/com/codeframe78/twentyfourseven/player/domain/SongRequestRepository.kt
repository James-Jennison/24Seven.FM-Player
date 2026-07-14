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

data class RequestSearchResult(
    val albumId: String,
    val trackTitle: String,
    val albumTitle: String,
    val year: String? = null,
)

data class RequestableTrack(
    val albumId: String,
    val songId: String,
    val title: String,
    val artist: String? = null,
    val duration: String? = null,
    val eligible: Boolean,
)

data class SongRequestState(
    val stationId: StationId,
    val status: SongRequestLoadStatus = SongRequestLoadStatus.Idle,
    val query: String = "",
    val searchField: RequestSearchField = RequestSearchField.Title,
    val searchResults: List<RequestSearchResult> = emptyList(),
    val albumTitle: String? = null,
    val tracks: List<RequestableTrack> = emptyList(),
    val pendingRequest: RequestableTrack? = null,
    val notice: String? = null,
    val errorMessage: String? = null,
)

interface SongRequestRepository {
    fun observeRequests(stationId: StationId): Flow<SongRequestState>
    suspend fun search(stationId: StationId, query: String, field: RequestSearchField)
    suspend fun suggest(stationId: StationId, mode: RequestSuggestionMode)
    suspend fun openAlbum(stationId: StationId, albumId: String)
    suspend fun prepareRequest(stationId: StationId, songId: String)
    suspend fun cancelRequest(stationId: StationId)
    suspend fun confirmRequest(stationId: StationId, message: String = "")
}
