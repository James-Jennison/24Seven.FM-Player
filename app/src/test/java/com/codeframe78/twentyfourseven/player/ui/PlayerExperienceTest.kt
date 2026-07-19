package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerExperienceTest {
    @Test
    fun `adjacent stations wrap in both directions`() {
        val stations = listOf("sst", "1980s", "afm", "dfm", "efm").map(::station)

        assertEquals(StationId("1980s"), adjacentStationId(stations, StationId("sst"), 1))
        assertEquals(StationId("efm"), adjacentStationId(stations, StationId("sst"), -1))
        assertEquals(StationId("sst"), adjacentStationId(stations, StationId("efm"), 1))
    }

    @Test
    fun `double back opens confirmation only inside its window`() {
        val gate = DoubleBackExitGate(windowMillis = 2_000)

        assertFalse(gate.registerPress(1_000))
        assertTrue(gate.registerPress(2_500))
        assertFalse(gate.registerPress(2_600))
        assertFalse(gate.registerPress(5_000))
        assertTrue(gate.registerPress(7_000))
    }

    @Test
    fun `sleep timer countdown rounds up and formats hours`() {
        assertEquals("0:01", formatSleepTimerRemaining(1L))
        assertEquals("29:59", formatSleepTimerRemaining(30L * 60L * 1_000L - 1_000L))
        assertEquals("1:30:00", formatSleepTimerRemaining(90L * 60L * 1_000L))
    }

    private fun station(id: String) = Station(
        id = StationId(id),
        name = id,
        shortName = id,
        description = id,
        websiteUrl = "https://example.invalid/",
    )
}
