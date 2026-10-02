package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLinksTest {
    private val station = StationId("sst")
    private val live = NowPlayingState(station, "Composer Three - Finale")
    private val onAir = NowPlayingState(station, "Composer Two - Opening")

    @Test
    fun `an idle player shows what is on air, and a playing one shows its own track`() {
        assertEquals(onAir, shownNowPlaying(live, onAir, station, PlaybackStatus.Idle))
        assertEquals(onAir, shownNowPlaying(live, onAir, station, PlaybackStatus.Paused))
        assertEquals(live, shownNowPlaying(live, onAir, station, PlaybackStatus.Playing))
        assertEquals(live, shownNowPlaying(live, null, station, PlaybackStatus.Idle))
    }

    @Test
    fun `while playback has not reported a track yet, the on-air reading stays on screen`() {
        val cleared = NowPlayingState(station)

        assertEquals(onAir, shownNowPlaying(cleared, onAir, station, PlaybackStatus.Buffering))
        assertEquals(cleared, shownNowPlaying(cleared, null, station, PlaybackStatus.Buffering))
    }

    @Test
    fun `tracks reported for another station are never shown`() {
        val other = StationId("afm")

        val shown = shownNowPlaying(live, onAir, other, PlaybackStatus.Idle)

        assertEquals(other, shown.stationId)
        assertNull(shown.displayTitle)
    }

    @Test
    fun `the on-air track is read again shortly after it should end, within sensible bounds`() {
        val track = NowPlayingState(station, "Opening", trackLengthMillis = 200_000, trackStartedElapsedRealtimeMillis = 1_000)

        assertEquals(30_000L, onAirRefreshDelayMillis(null, 0))
        assertEquals(30_000L, onAirRefreshDelayMillis(onAir, 0))
        // 40 seconds remain, so ask 43 seconds from now.
        assertEquals(43_000L, onAirRefreshDelayMillis(track, 161_000))
        assertEquals(60_000L, onAirRefreshDelayMillis(track, 1_000))
        assertEquals(15_000L, onAirRefreshDelayMillis(track, 500_000))
    }

    @Test
    fun `an idle player with a track says it is on air instead of not connected`() {
        assertEquals("On air now", playbackStatusMessage(PlaybackStatus.Idle, hasTrack = true))
        assertEquals("Not connected", playbackStatusMessage(PlaybackStatus.Idle, hasTrack = false))
        assertEquals("Playback paused", playbackStatusMessage(PlaybackStatus.Paused, hasTrack = true))
    }

    @Test
    fun `track progress is drawn while playing and while showing what is on air`() {
        assertTrue(PlaybackStatus.Playing.followsStationTrack)
        assertTrue(PlaybackStatus.Idle.followsStationTrack)
        assertFalse(PlaybackStatus.Buffering.followsStationTrack)
        assertFalse(PlaybackStatus.Connecting.followsStationTrack)
    }

    @Test
    fun `a cover image names its album`() {
        assertEquals("B000KNB1IM", albumIdFromCoverUrl("https://streamingsoundtracks.com/images/cover/500/B000KNB1IM.jpg"))
        assertEquals("B000KNB1IM", albumIdFromCoverUrl("/images/cover/B000KNB1IM.jpg?v=2"))
        assertNull(albumIdFromCoverUrl("https://streamingsoundtracks.com/images/news/banner.png"))
        assertNull(albumIdFromCoverUrl(null))
    }

    @Test
    fun `the messages button announces unread private messages`() {
        assertEquals("Private messages", messagesButtonDescription(0))
        assertEquals("Private messages, 1 unread", messagesButtonDescription(1))
        assertEquals("Private messages, 12 unread", messagesButtonDescription(12))
    }
}
