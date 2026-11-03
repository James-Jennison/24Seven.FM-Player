package com.codeframe78.twentyfourseven.player.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationPaletteTest {
    private val stations = listOf("sst", "1980s", "afm", "dfm", "efm")
    private val darkSurface = Color(0xFF0E0B14)
    private val lightSurface = Color(0xFFFFF8FC)
    private val onDarkAccent = Color(0xFF1C1424)

    private fun contrast(a: Color, b: Color): Float {
        val hi = maxOf(a.luminance(), b.luminance())
        val lo = minOf(a.luminance(), b.luminance())
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun `accents follow the station websites' brand colours`() {
        assertEquals(Color(0xFF99CCFF), stationPalette(StationId("sst")).accent)
        assertEquals(Color(0xFFFF99CC), stationPalette(StationId("1980s")).accent)
        assertEquals(Color(0xFFFFCC99), stationPalette(StationId("afm")).accent)
        assertEquals(Color(0xFFFFFF66), stationPalette(StationId("efm")).accent)
    }

    @Test
    fun `every station accent is distinct`() {
        assertEquals(stations.size, stations.map { stationPalette(StationId(it)).accent }.toSet().size)
    }

    @Test
    fun `accents are readable on the dark theme and text on a fill of them is readable`() {
        stations.forEach {
            val p = stationPalette(StationId(it))
            assertTrue("$it accent on dark", contrast(p.accent, darkSurface) >= 4.5f)
            assertTrue("$it text on accent fill", contrast(onDarkAccent, p.accent) >= 4.5f)
        }
    }

    @Test
    fun `light theme accents are readable and white on a fill of them is readable`() {
        stations.forEach {
            val p = stationPalette(StationId(it))
            assertTrue("$it accent on light", contrast(p.accentOnLight, lightSurface) >= 4.5f)
            assertTrue("$it white on accent fill", contrast(Color.White, p.accentOnLight) >= 4.5f)
        }
    }
}
