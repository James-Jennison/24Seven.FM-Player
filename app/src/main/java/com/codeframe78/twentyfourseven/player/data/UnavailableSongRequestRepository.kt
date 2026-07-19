package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.RequestConfirmationContext
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UnavailableSongRequestRepository : SongRequestRepository {
    override fun observeRequests(stationId: StationId): Flow<SongRequestState> = flowOf(SongRequestState(stationId))
    override suspend fun search(stationId: StationId, query: String, field: RequestSearchField) = Unit
    override suspend fun suggest(stationId: StationId, mode: RequestSuggestionMode) = Unit
    override suspend fun openSearchResult(stationId: StationId, target: RequestSearchTarget) = Unit
    override suspend fun prepareRequest(stationId: StationId, songId: String, accountDisplayName: String) = Unit
    override suspend fun prepareRequest(stationId: StationId, track: RequestableTrack, accountDisplayName: String) = Unit
    override suspend fun cancelRequest(stationId: StationId) = Unit
    override suspend fun clear(stationId: StationId) = Unit
    override suspend fun confirmRequest(stationId: StationId, context: RequestConfirmationContext, message: String) = Unit
}
