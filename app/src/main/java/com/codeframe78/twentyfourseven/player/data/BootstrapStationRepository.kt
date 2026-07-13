package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class BootstrapStationRepository : StationRepository {
    private val stations = listOf(
        Station(StationId("sst"), "StreamingSoundtracks.com", "SST", "Movie, game, TV and anime scores", "https://www.streamingsoundtracks.com/", streams("streamingsoundtracks.com")),
        Station(StationId("1980s"), "1980s.FM", "1980s", "Music from the 1980s", "https://1980s.fm/", streams("1980s.fm")),
        Station(StationId("adagio"), "Adagio.FM", "Adagio", "Classical and light music", "https://adagio.fm/", streams("adagio.fm")),
        Station(StationId("death"), "Death.FM", "Death", "Extreme metal", "https://death.fm/", streams("death.fm")),
        Station(StationId("entranced"), "Entranced.FM", "Entranced", "Trance and electronic music", "https://entranced.fm/", streams("entranced.fm")),
    )
    private val selected = MutableStateFlow(stations.first())

    override fun observeStations(): Flow<List<Station>> = flowOf(stations)
    override fun observeSelectedStation(): Flow<Station> = selected.asStateFlow()
    override suspend fun selectStation(stationId: StationId) {
        selected.value = stations.first { it.id == stationId }
    }

    private companion object {
        fun streams(domain: String) = listOf(
            StreamVariant("http://hi5.$domain/;", "Primary relay", priority = 0),
            StreamVariant("http://hi.$domain/;", "Source stream", priority = 1),
        )
    }
}
