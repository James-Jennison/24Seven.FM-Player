package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

interface StationRepository {
    /**
     * The fixed, public station catalog that can be safely exposed to system media surfaces.
     *
     * This deliberately excludes station-scoped account and community data.
     */
    fun availableStations(): List<Station>
    fun observeStations(): Flow<List<Station>>
    fun observeSelectedStation(): Flow<Station>
    fun observeStationPreferences(): Flow<LocalStationPreferences>
    suspend fun selectStation(stationId: StationId)
    suspend fun useLastStationAtStartup()
    suspend fun setStartupStation(stationId: StationId)
}
