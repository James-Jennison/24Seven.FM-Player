package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun observeStations(): Flow<List<Station>>
    fun observeSelectedStation(): Flow<Station>
    suspend fun selectStation(stationId: StationId)
}

