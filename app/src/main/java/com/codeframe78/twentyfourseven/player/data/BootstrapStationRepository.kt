package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.StationPreferencesRepository
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class BootstrapStationRepository(
    private val preferences: StationPreferencesRepository = InMemoryStationPreferencesRepository(),
) : StationRepository {
    private val stations = listOf(
        Station(StationId("sst"), "StreamingSoundtracks.com", "SST", "Movie, game, TV and anime scores", "https://www.streamingsoundtracks.com/", streams("streamingsoundtracks.com"), queueCapabilities.copy(supportsRequestMessages = true, supportsListenerActivity = true)),
        Station(StationId("1980s"), "1980s.FM", "1980s", "Music from the 1980s", "https://1980s.fm/", streams("1980s.fm"), queueCapabilities),
        Station(StationId("adagio"), "Adagio.FM", "Adagio", "Classical and light music", "https://adagio.fm/", streams("adagio.fm"), queueCapabilities),
        Station(StationId("death"), "Death.FM", "Death", "Extreme metal", "https://death.fm/", streams("death.fm"), queueCapabilities),
        Station(StationId("entranced"), "Entranced.FM", "Entranced", "Trance and electronic music", "https://entranced.fm/", streams("entranced.fm"), queueCapabilities),
    )
    private val selected = MutableStateFlow(resolveStartupStation(preferences.current))

    override fun observeStations(): Flow<List<Station>> = flowOf(stations)
    override fun observeSelectedStation(): Flow<Station> = selected.asStateFlow()
    override fun observeStationPreferences(): Flow<LocalStationPreferences> = preferences.observePreferences()

    override suspend fun selectStation(stationId: StationId) {
        val station = stations.firstOrNull { it.id == stationId } ?: return
        selected.value = station
        preferences.recordLastStation(station.id)
    }

    override suspend fun useLastStationAtStartup() {
        preferences.setStartupPreference(StartupStationMode.LastSelected)
    }

    override suspend fun setStartupStation(stationId: StationId) {
        if (stations.none { it.id == stationId }) return
        preferences.setStartupPreference(StartupStationMode.Fixed, stationId)
    }

    private fun resolveStartupStation(saved: LocalStationPreferences): Station {
        val candidates = when (saved.startupMode) {
            StartupStationMode.Fixed -> listOfNotNull(saved.defaultStationId, saved.lastStationId)
            StartupStationMode.LastSelected -> listOfNotNull(saved.lastStationId, saved.defaultStationId)
        }
        return candidates.firstNotNullOfOrNull { candidate -> stations.firstOrNull { it.id == candidate } }
            ?: stations.first()
    }

    private companion object {
        val queueCapabilities = StationCapabilities(
            supportsAuthentication = true,
            supportsChat = true,
            supportsFavorites = true,
            supportsQueue = true,
            supportsHistory = true,
            supportsRequests = true,
        )

        fun streams(domain: String) = listOf(
            StreamVariant("http://hi5.$domain/;", "Primary relay", priority = 0, format = StreamFormat.Aac, bitrateKbps = 128),
            StreamVariant("http://hi.$domain/;", "Source stream", priority = 1, format = StreamFormat.Aac, bitrateKbps = 128),
        )
    }
}
