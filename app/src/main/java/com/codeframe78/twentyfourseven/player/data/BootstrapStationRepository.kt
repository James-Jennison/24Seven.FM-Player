package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationPage
import com.codeframe78.twentyfourseven.player.domain.StationPageKind
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
        station(
            id = "sst",
            name = "StreamingSoundtracks.com",
            shortName = "SST",
            description = "Movie, game, TV and anime scores",
            domain = "streamingsoundtracks.com",
            websiteDomain = "www.streamingsoundtracks.com",
            capabilities = queueCapabilities.copy(
                supportsRequestMessages = true,
                supportsListenerActivity = true,
                supportsSecondaryContent = true,
            ),
        ),
        station(
            id = "1980s",
            name = "1980s.FM",
            shortName = "1980s",
            description = "Music from the 1980s",
            domain = "1980s.fm",
            capabilities = queueCapabilities.copy(supportsSecondaryContent = true),
        ),
        station(
            id = "adagio",
            name = "Adagio.FM",
            shortName = "Adagio",
            description = "Classical and light music",
            domain = "adagio.fm",
            capabilities = queueCapabilities.copy(supportsSecondaryContent = true),
        ),
        station(
            id = "death",
            name = "Death.FM",
            shortName = "Death",
            description = "Extreme metal",
            domain = "death.fm",
            capabilities = queueCapabilities.copy(supportsSecondaryContent = true),
            membershipTitle = "RIP membership",
            membershipModule = "RIP_Subscribe",
        ),
        station(
            id = "entranced",
            name = "Entranced.FM",
            shortName = "Entranced",
            description = "Trance and electronic music",
            domain = "entranced.fm",
            capabilities = queueCapabilities.copy(supportsSecondaryContent = true),
        ),
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

        fun station(
            id: String,
            name: String,
            shortName: String,
            description: String,
            domain: String,
            websiteDomain: String = domain,
            capabilities: StationCapabilities,
            membershipTitle: String = "VIP membership",
            membershipModule: String = "VIP_Subscribe",
        ) = Station(
            id = StationId(id),
            name = name,
            shortName = shortName,
            description = description,
            websiteUrl = "https://$websiteDomain/",
            streams = streams(domain),
            capabilities = capabilities,
            secondaryPages = if (capabilities.supportsSecondaryContent) {
                secondaryPages(domain, membershipTitle, membershipModule)
            } else {
                emptyList()
            },
        )

        fun secondaryPages(domain: String, membershipTitle: String, membershipModule: String) = listOf(
            page(domain, StationPageKind.Contact, "Contact", "Contact the station team", "Contact_Us"),
            page(domain, StationPageKind.Membership, membershipTitle, "Station membership information", membershipModule),
        )

        fun page(
            domain: String,
            kind: StationPageKind,
            title: String,
            description: String,
            module: String,
        ) = StationPage(kind, title, description, "https://$domain/modules.php?name=$module")
    }
}
