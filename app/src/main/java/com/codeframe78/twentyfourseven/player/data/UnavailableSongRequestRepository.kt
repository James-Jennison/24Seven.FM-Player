package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UnavailableSongRequestRepository : SongRequestRepository {
    override fun observeRequests(stationId: StationId): Flow<SongRequestState> = flowOf(SongRequestState(stationId))
    override suspend fun search(stationId: StationId, query: String, field: RequestSearchField) = Unit
    override suspend fun openAlbum(stationId: StationId, albumId: String) = Unit
    override suspend fun prepareRequest(stationId: StationId, songId: String) = Unit
    override suspend fun cancelRequest(stationId: StationId) = Unit
    override suspend fun confirmRequest(stationId: StationId, message: String) = Unit
}
