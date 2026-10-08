package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.ChatRole
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
        assertEquals("Tap Play to listen", playbackStatusMessage(PlaybackStatus.Idle, hasTrack = false))
        assertEquals("Paused", playbackStatusMessage(PlaybackStatus.Paused, hasTrack = true))
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
    fun `membership shows the tier the station reports and the member's rank`() {
        assertEquals("VIP member · Admiral (Administrator)", membershipLabel(MembershipTier.Vip, "Admiral (Administrator)"))
        assertEquals("Standard member", membershipLabel(MembershipTier.Standard, null))
        assertEquals("Captain", membershipLabel(MembershipTier.Unknown, "Captain"))
        assertEquals("Not reported by station", membershipLabel(null, null))
    }

    @Test
    fun `a queued request says about when it will play`() {
        assertEquals("Due shortly", queuedRequestLabel(20))
        assertEquals("Plays in about 1 minute", queuedRequestLabel(70))
        assertEquals("Plays in about 18 minutes", queuedRequestLabel(1054))
        assertEquals("Plays in about 1 hr 5 min", queuedRequestLabel(3_900))
    }

    @Test
    fun `request times read naturally and unknown formats are left as the station wrote them`() {
        assertEquals("Oct 2 · 4:16 PM", requestTimeLabel("2026-10-02 16:16:02", currentYear = 2026))
        assertEquals("Oct 2, 2026 · 4:16 PM", requestTimeLabel("2026-10-02 16:16:02", currentYear = 2027))
        assertEquals("14 Jul 26 - 17:01", requestTimeLabel("14 Jul 26 - 17:01"))
    }

    @Test
    fun `the messages button announces unread private messages`() {
        assertEquals("Private messages", messagesButtonDescription(0))
        assertEquals("Private messages, 1 unread", messagesButtonDescription(1))
        assertEquals("Private messages, 12 unread", messagesButtonDescription(12))
    }

    @Test
    fun `chat time drops the year and seconds and keeps anything it does not recognize`() {
        assertEquals(ChatStamp("02 Oct", "15:04"), chatStamp("02 Oct 26 - 15:04:41"))
        assertEquals(ChatStamp("2 Oct", "09:04"), chatStamp(" 2 Oct 26 - 9:04:00 "))
        assertEquals(null, chatStamp("yesterday"))
        assertEquals(null, chatStamp(null))
        assertNull(chatRoleColor(ChatRole.Member, onDark = true))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFCC66), chatRoleColor(ChatRole.Administrator, onDark = true))
        assertTrue(chatMentions("thanks, listener!", "Listener"))
        assertFalse(chatMentions("many listeners tonight", "Listener"))
        assertFalse(chatMentions("hello", null))
    }

    @Test
    fun `a request splits into track and artist at the last dash`() {
        assertEquals("Suite From Seven" to "Howard Shore", splitTrackSummary("Suite From Seven \u2014 Howard Shore"))
        assertEquals("One \u2014 Two" to "Composer Two", splitTrackSummary("One \u2014 Two \u2014 Composer Two"))
        assertEquals("Untitled track" to null, splitTrackSummary("Untitled track"))
    }

    @Test
    fun `a favorite's cover comes from the station's own image host and only for a plain album id`() {
        val station = com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository().availableStations()
            .first { it.id.value == "dfm" }
        assertEquals("https://death.fm/images/cover/040/B000000002.jpg", favoriteCoverUrl(station, "B000000002"))
        assertEquals(null, favoriteCoverUrl(station, "../secret"))
        assertEquals(null, favoriteCoverUrl(station, null))
        assertEquals(null, favoriteCoverUrl(null, "B000000002"))
    }
}
