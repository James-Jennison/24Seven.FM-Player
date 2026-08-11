package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import com.codeframe78.twentyfourseven.player.domain.StationPageKind
import com.codeframe78.twentyfourseven.player.domain.StationPageTrustPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BootstrapStationRepositoryTest {
    private val repository = BootstrapStationRepository()

    @Test
    fun `catalog contains the five network stations`() = runTest {
        val stations = repository.observeStations().first()

        assertEquals(
            listOf("sst", "1980s", "afm", "dfm", "efm"),
            stations.map { it.id.value },
        )
    }

    @Test
    fun `station selection updates observed station`() = runTest {
        repository.selectStation(StationId("afm"))

        assertEquals("afm", repository.observeSelectedStation().first().id.value)
    }

    @Test
    fun `every station uses its HTTPS live proxy`() = runTest {
        val stations = repository.observeStations().first()

        assertEquals(
            listOf(
                "https://streamingsoundtracks.com/live",
                "https://1980s.fm/live",
                "https://adagio.fm/live",
                "https://death.fm/live",
                "https://entranced.fm/live",
            ),
            stations.map { station -> station.streams.single().url },
        )
        stations.forEach { station ->
            assertEquals("Live stream", station.streams.single().label)
            assertEquals(0, station.streams.single().priority)
            assertEquals(StreamFormat.Aac, station.streams.single().format)
            assertEquals(192, station.streams.single().bitrateKbps)
        }
    }

    @Test
    fun `every station exposes its verified same origin cover logo`() = runTest {
        val stations = repository.observeStations().first()

        assertEquals(
            listOf(
                "https://streamingsoundtracks.com/images/logos/logo-sst-v200x200.png",
                "https://1980s.fm/images/logos/1980s_logo-200x200.png",
                "https://adagio.fm/images/logos/logo-afm-200x200.png",
                "https://death.fm/images/logos/logo-dfm-200x200.png",
                "https://entranced.fm/images/logos/logo-efm-g200x200.png",
            ),
            stations.map { station -> station.logoUrl },
        )
    }

    @Test
    fun `fixed startup station is selected before the repository emits`() = runTest {
        val preferences = InMemoryStationPreferencesRepository(
            LocalStationPreferences(
                startupMode = StartupStationMode.Fixed,
                defaultStationId = StationId("dfm"),
                lastStationId = StationId("afm"),
            ),
        )

        val selected = BootstrapStationRepository(preferences).observeSelectedStation().first()

        assertEquals(StationId("dfm"), selected.id)
    }

    @Test
    fun `last selected startup falls back across removed or corrupt station ids`() = runTest {
        val preferences = InMemoryStationPreferencesRepository(
            LocalStationPreferences(
                startupMode = StartupStationMode.Fixed,
                defaultStationId = StationId("removed-station"),
                lastStationId = StationId("efm"),
            ),
        )

        val selected = BootstrapStationRepository(preferences).observeSelectedStation().first()

        assertEquals(StationId("efm"), selected.id)
    }

    @Test
    fun `selection and startup actions persist only valid station ids`() = runTest {
        val preferences = InMemoryStationPreferencesRepository()
        val repository = BootstrapStationRepository(preferences)

        repository.selectStation(StationId("afm"))
        repository.setStartupStation(StationId("dfm"))
        repository.selectStation(StationId("unknown"))

        assertEquals(StationId("afm"), preferences.current.lastStationId)
        assertEquals(StartupStationMode.Fixed, preferences.current.startupMode)
        assertEquals(StationId("dfm"), preferences.current.defaultStationId)

        repository.useLastStationAtStartup()
        assertEquals(StartupStationMode.LastSelected, preferences.current.startupMode)
        assertEquals(null, preferences.current.defaultStationId)
    }

    @Test
    fun `every station exposes the administrator authorized queue and history capabilities`() = runTest {
        val stations = repository.observeStations().first()
        stations.forEach { station ->
            assertEquals(true, station.capabilities.supportsQueue)
            assertEquals(true, station.capabilities.supportsHistory)
            assertEquals(true, station.capabilities.supportsRequests)
        }
        assertEquals(
            listOf("sst"),
            stations.filter { it.capabilities.supportsRequestMessages }.map { it.id.value },
        )
    }

    @Test
    fun `play compliant contact page is capability scoped and trusted`() = runTest {
        val stations = repository.observeStations().first()

        assertEquals(
            listOf("sst", "1980s", "afm", "dfm", "efm"),
            stations.filter { it.capabilities.supportsSecondaryContent }.map { it.id.value },
        )
        stations.filter { it.capabilities.supportsSecondaryContent }.forEach { station ->
            assertEquals(
                listOf(StationPageKind.Contact),
                station.secondaryPages.map { it.kind },
            )
            assertEquals(
                true,
                station.secondaryPages.all { it.isTrustedFor(station) },
            )
        }
    }

    @Test
    fun `1980s certification contract does not inherit SST only capabilities`() = runTest {
        val station = repository.observeStations().first().single { it.id == StationId("1980s") }

        with(station.capabilities) {
            assertEquals(true, supportsAuthentication)
            assertEquals(true, supportsChat)
            assertEquals(true, supportsFavorites)
            assertEquals(true, supportsQueue)
            assertEquals(true, supportsHistory)
            assertEquals(true, supportsRequests)
            assertEquals(true, supportsSecondaryContent)
            assertEquals(false, supportsRequestMessages)
            assertEquals(false, supportsListenerActivity)
        }
        assertEquals("https://1980s.fm/", station.websiteUrl)
        assertEquals(
            listOf(StationPageKind.Contact),
            station.secondaryPages.map { it.kind },
        )
        assertEquals(
            true,
            station.secondaryPages.all { it.isTrustedFor(station) },
        )
    }

    @Test
    fun `adagio certification contract does not inherit SST only capabilities`() = runTest {
        val station = repository.observeStations().first().single { it.id == StationId("afm") }

        with(station.capabilities) {
            assertEquals(true, supportsAuthentication)
            assertEquals(true, supportsChat)
            assertEquals(true, supportsFavorites)
            assertEquals(true, supportsQueue)
            assertEquals(true, supportsHistory)
            assertEquals(true, supportsRequests)
            assertEquals(true, supportsSecondaryContent)
            assertEquals(false, supportsRequestMessages)
            assertEquals(false, supportsListenerActivity)
        }
        assertEquals("https://adagio.fm/", station.websiteUrl)
        assertEquals(
            listOf(StationPageKind.Contact),
            station.secondaryPages.map { it.kind },
        )
        assertEquals(
            true,
            station.secondaryPages.all { it.isTrustedFor(station) },
        )
    }

    @Test
    fun `death certification contract uses verified RIP pages without SST only capabilities`() = runTest {
        val station = repository.observeStations().first().single { it.id == StationId("dfm") }

        with(station.capabilities) {
            assertEquals(true, supportsAuthentication)
            assertEquals(true, supportsChat)
            assertEquals(true, supportsFavorites)
            assertEquals(true, supportsQueue)
            assertEquals(true, supportsHistory)
            assertEquals(true, supportsRequests)
            assertEquals(true, supportsSecondaryContent)
            assertEquals(false, supportsRequestMessages)
            assertEquals(false, supportsListenerActivity)
        }
        assertEquals("https://death.fm/", station.websiteUrl)
        assertEquals(
            listOf(StationPageKind.Contact),
            station.secondaryPages.map { it.kind },
        )
        assertEquals(
            true,
            station.secondaryPages.all { it.isTrustedFor(station) },
        )
    }

    @Test
    fun `entranced certification contract does not inherit SST only capabilities`() = runTest {
        val station = repository.observeStations().first().single { it.id == StationId("efm") }

        with(station.capabilities) {
            assertEquals(true, supportsAuthentication)
            assertEquals(true, supportsChat)
            assertEquals(true, supportsFavorites)
            assertEquals(true, supportsQueue)
            assertEquals(true, supportsHistory)
            assertEquals(true, supportsRequests)
            assertEquals(true, supportsSecondaryContent)
            assertEquals(false, supportsRequestMessages)
            assertEquals(false, supportsListenerActivity)
        }
        assertEquals("https://entranced.fm/", station.websiteUrl)
        assertEquals(
            listOf(StationPageKind.Contact),
            station.secondaryPages.map { it.kind },
        )
        assertEquals(
            true,
            station.secondaryPages.all { it.isTrustedFor(station) },
        )
    }

    private fun com.codeframe78.twentyfourseven.player.domain.StationPage.isTrustedFor(
        station: com.codeframe78.twentyfourseven.player.domain.Station,
    ): Boolean = if (kind == StationPageKind.Contact) {
        StationPageTrustPolicy.trustedEmailRecipient(station, this) ==
            com.codeframe78.twentyfourseven.player.domain.PLAYER_CONTACT_EMAIL
    } else {
        StationPageTrustPolicy.trustedUrl(station, this) == url
    }
}
