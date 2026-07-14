package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
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
            listOf("sst", "1980s", "adagio", "death", "entranced"),
            stations.map { it.id.value },
        )
    }

    @Test
    fun `station selection updates observed station`() = runTest {
        repository.selectStation(StationId("adagio"))

        assertEquals("adagio", repository.observeSelectedStation().first().id.value)
    }

    @Test
    fun `every station has relay and source fallbacks`() = runTest {
        repository.observeStations().first().forEach { station ->
            assertEquals(listOf("Primary relay", "Source stream"), station.streams.map { it.label })
            assertEquals(listOf(0, 1), station.streams.map { it.priority })
            assertEquals(listOf(StreamFormat.Aac, StreamFormat.Aac), station.streams.map { it.format })
            assertEquals(listOf(128, 128), station.streams.map { it.bitrateKbps })
        }
    }

    @Test
    fun `every station exposes the administrator authorized queue and history capabilities`() = runTest {
        repository.observeStations().first().forEach { station ->
            assertEquals(true, station.capabilities.supportsQueue)
            assertEquals(true, station.capabilities.supportsHistory)
            assertEquals(true, station.capabilities.supportsRequests)
        }
    }
}
