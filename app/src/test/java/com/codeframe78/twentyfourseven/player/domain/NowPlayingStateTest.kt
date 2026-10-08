package com.codeframe78.twentyfourseven.player.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingStateTest {
    private val station = StationId("sst")

    @Test
    fun `station details for the same track are attached to the stream title`() {
        val stream = NowPlayingState(station, displayTitle = "The Passions - Story, The")
        val details = NowPlayingState(
            stationId = station,
            displayTitle = "The Passions - The Story",
            artworkUrl = "https://streamingsoundtracks.com/images/cover/500/B00Q5M2SYS.jpg",
            track = "The Story",
            albumId = "B00Q5M2SYS",
            requesterName = "Listener",
            listenerCount = 40,
            trackLengthMillis = 180_000L,
            trackStartedElapsedRealtimeMillis = 5_000L,
        )

        val enriched = stream.withStationDetails(details)

        assertEquals("The Passions - Story, The", enriched.displayTitle)
        assertEquals("Listener", enriched.requesterName)
        assertEquals("B00Q5M2SYS", enriched.albumId)
        assertEquals(180_000L, enriched.trackLengthMillis)
        assertEquals(5_000L, enriched.trackStartedElapsedRealtimeMillis)
    }

    @Test
    fun `details for a different track supply only artwork and the audience`() {
        val stream = NowPlayingState(station, displayTitle = "Don Davis - Bone Man Ben")
        val details = NowPlayingState(
            stationId = station,
            artworkUrl = "https://streamingsoundtracks.com/images/cover/500/B01M9B4IUW.jpg",
            track = "The Journey, Not The Destination",
            albumId = "B01M9B4IUW",
            requesterName = "Listener",
            listenerCount = 92,
            trackLengthMillis = 276_812L,
            trackStartedElapsedRealtimeMillis = 9_000L,
        )

        val enriched = stream.withStationDetails(details)

        assertEquals(details.artworkUrl, enriched.artworkUrl)
        assertEquals(92, enriched.listenerCount)
        assertNull(enriched.requesterName)
        assertNull(enriched.albumId)
        assertNull(enriched.trackLengthMillis)
        assertNull(enriched.trackStartedElapsedRealtimeMillis)
    }

    @Test
    fun `missing details leave the stream title unchanged`() {
        val stream = NowPlayingState(station, displayTitle = "Don Davis - Bone Man Ben")

        assertEquals(stream, stream.withStationDetails(null))
    }
}
